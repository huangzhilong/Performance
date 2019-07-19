package com.yy.performance

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import com.android.utils.FileUtils
import com.google.common.collect.ImmutableSet
import com.yy.performance.plugin.AbsBasePlugin
import com.yy.performance.util.JavaAssistHelper
import com.yy.performance.util.LogUtil
import com.yy.performance.util.TransformUtils
import com.yy.performance.util.JarZipUtils
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project

import java.util.concurrent.Callable

/**
 * Created by huangzhilong on 19/7/17.
 *
 * 支持增量和并发
 */

class PerformanceTransform extends Transform {

    private final static String TAG = "PerformanceTransform"

    private Project mProject
    //同一个Transform可对应多个plugin，避免多个Transform解压遍历操作
    private List<AbsBasePlugin> mPluginList

    PerformanceTransform(Project project) {
        mProject = project
        //初始化pool
        JavaAssistHelper.getInstance().initPool(project)
    }

    void setPluginList(List<AbsBasePlugin> pluginList) {
        LogUtil.log(TAG, "setPluginList size: %s", pluginList == null ? 0 : pluginList.size())
        mPluginList = pluginList
    }

    @Override
    String getName() {
        return TAG
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        //表示只输入class文件，还可以是RESOURCES
        return ImmutableSet.<QualifiedContent.ContentType> of(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    //支持增量 If it does, then the TransformInput may contain a list of changed/removed/added files
    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(@NonNull TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {

        if (mPluginList == null || mPluginList.size() == 0) {
            LogUtil.log(TAG, "transform return because plugin is empty")
            return
        }
        try {
            doTransform(transformInvocation)
        } catch (Exception e) {
            LogUtil.log(TAG, "doTransform get ex: %s", e)
        } finally {
            onFinallyTransform()
        }
    }

    private void doTransform(@NonNull TransformInvocation transformInvocation) {
        Collection<TransformInput> mInputCollection = transformInvocation.getInputs()
        // 创建一个对应名称表示的输出目录 位于app/build/intermediates/transforms/(transform的getName)
        // 是从0 、1、2开始递增。如果是目录，名称就是对应的数字，如果是jar包就类似0.jar
        TransformOutputProvider mOutputProvider = transformInvocation.getOutputProvider()
        //此次是否是增量
        boolean isIncremental = transformInvocation.isIncremental()
        onBeforeTransform()

        //非增量清空旧的输出内容
        if (!isIncremental) {
            mOutputProvider.deleteAll()
        }

        //支持多线程并发
        WaitableExecutor waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()

        mInputCollection.each { TransformInput input ->
            //遍历文件夹
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File dest = mOutputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)
                //保证dest目录存在
                if (dest.exists() && !dest.isDirectory()) {
                    dest.delete()
                }
                if (!dest.exists()) {
                    FileUtils.mkdirs(dest)
                }
                JavaAssistHelper.getInstance().addClassPath(directoryInput.file.absolutePath)

                waitableExecutor.execute(new Callable<Object>() {
                    @Override
                    Object call() throws Exception {
                        handlerDirectoryInput(directoryInput, isIncremental, dest)
                        return null
                    }
                })
            }

            //遍历jar包
            input.jarInputs.each { JarInput jarInput ->
                //重命名输出文件（因为可能同名。会覆盖冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //获取output
                def dest = mOutputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes,
                        jarInput.scopes, Format.JAR)
                waitableExecutor.execute(new Callable<Object>() {
                    @Override
                    Object call() throws Exception {
                        handlerJarInput(jarInput, isIncremental, dest)
                        return null
                    }
                })
            }
        }
        //等待所有任务结束
        waitableExecutor.waitForTasksWithQuickFail(true)
    }

    private void handlerDirectoryInput(DirectoryInput directoryInput, boolean isIncremental, File dest) {
        //增量,只需要操作变动文件
        if (isIncremental) {
            Map<File, Status> statusMap = directoryInput.getChangedFiles()
            Iterator<File> iterator = statusMap.keySet().iterator()
            //输出目录文件夹地址和输入文件夹地址
            String destPath = dest.getAbsolutePath()
            String directoryPath = directoryInput.file.getAbsolutePath()
            while (iterator.hasNext()) {
                File file = iterator.next()
                Status status = statusMap.get(file)
                //得到输出目录的文件路径
                String fileDestPath = file.getAbsolutePath().replace(directoryPath, destPath)
                File destFile = new File(fileDestPath)
                switch (status) {
                    case Status.NOTCHANGED:
                        break
                    case Status.REMOVED:
                        if (destFile.exists()) {
                            FileUtils.delete(destFile)
                        }
                        break
                    case Status.ADDED:
                    case Status.CHANGED:
                        //操作修改对输入file，然后copy到输出目录
                        onHandlerEachClass(file, directoryPath, true)
                        FileUtils.copyFile(file, destFile)
                        break
                    default:
                        break
                }
            }
        } else {
            //遍历操作每个文件再进行copy的输出目录
            eachFileToDirectory(directoryInput.file, directoryInput.file.absolutePath, true)
            FileUtils.copyDirectory(directoryInput.file, dest)
        }
    }

    private void handlerJarInput(JarInput jarInput, boolean isIncremental, File dest) {
        if (isIncremental) {
            Status status = jarInput.getStatus()
            switch (status) {
                case Status.NOTCHANGED:
                    break
                case Status.REMOVED:
                    if (dest.exists()) {
                        FileUtils.delete(dest)
                    }
                    break
                case Status.CHANGED:
                case Status.ADDED:
                    doTransformJar(jarInput, dest)
                    break
                default:
                    break
            }
        } else {
            doTransformJar(jarInput, dest)
        }
    }

    private void doTransformJar(JarInput jarInput, File dest) {
        boolean needUnzip = isNeedUnzipJar(jarInput)
        if (!needUnzip) {
            //不需要解压
            FileUtils.copyFile(jarInput.file, dest)
            return
        }
        String unzipTmp = "${mProject.buildDir.absolutePath}${File.separator}tmp${File.separator}" + getName()
        unzipTmp = "${unzipTmp}${File.separator}${jarInput.name.replace(':', '')}"

        JarZipUtils.unzipJarZip(jarInput.file.absolutePath, unzipTmp)
        //加入classPool
        JavaAssistHelper.getInstance().addClassPath(unzipTmp)
        File f = new File(unzipTmp)
        eachFileToDirectory(f, unzipTmp, false)

        //修改完再压缩生成jar再copy到输出目录
        JarZipUtils.zipJarZip(unzipTmp, dest.absolutePath)
    }

    private void eachFileToDirectory(File file, String dir, boolean isDirectory) {
        if (file == null || !file.exists()) {
            return
        }
        if (file.isDirectory()) {
            File [] fileList = file.listFiles()
            for (int i = 0; i < fileList.length; i++) {
                File subFile = fileList[i]
                if (subFile.isDirectory()) {
                    eachFileToDirectory(subFile, dir, isDirectory)
                } else {
                    onHandlerEachClass(subFile, dir, isDirectory)
                }
            }
        } else {
            onHandlerEachClass(file, dir, isDirectory)
        }
    }

    private void onHandlerEachClass(File file, String dir, boolean isDirectory) {
        String fileName = file.name
        if (!fileName.endsWith(".class") || fileName.endsWith("R.class") || fileName.endsWith("BuildConfig.class")
                || fileName.contains("R\$")) {
            return
        }
        String className = TransformUtils.getClassName(file.getAbsolutePath(), dir)
        doHandlerEachClass(file, dir, className, isDirectory)
    }

    private void onBeforeTransform() {
        for (int i = 0; i < mPluginList.size(); i++) {
            AbsBasePlugin plugin = mPluginList.get(i)
            plugin.onBeforeTransform(mProject)
        }
    }

    private void onFinallyTransform() {
        for (int i = 0; i < mPluginList.size(); i++) {
            AbsBasePlugin plugin = mPluginList.get(i)
            plugin.onFinallyTransform(mProject)
        }
        JavaAssistHelper.getInstance().releaseClassPool()
    }

    private void doHandlerEachClass(File inputFile, String directoryPath, String className, boolean isDirectory) {
        for (int i = 0; i < mPluginList.size(); i++) {
            AbsBasePlugin plugin = mPluginList.get(i)
            plugin.doHandlerEachClass(inputFile, directoryPath, className, isDirectory)
        }
    }

    private boolean isNeedUnzipJar(JarInput jarInput) {
        for (int i = 0; i < mPluginList.size(); i++) {
            AbsBasePlugin plugin = mPluginList.get(i)
            if (plugin.isNeedUnzipJar(jarInput)) {
                return true
            }
        }
        return false
    }
}