package com.yy.performance

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.google.common.collect.ImmutableSet
import com.yy.performance.plugin.AbsBasePlugin
import com.yy.performance.util.JavaAssistHelper
import com.yy.performance.util.LogUtil
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project

/**
 * Created by huangzhilong on 19/7/17.
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

    //获取output
    // getContentLocation方法相当于创建一个对应名称表示的目录
    // 是从0 、1、2开始递增。如果是目录，名称就是对应的数字，如果是jar包就类似0.jar
    // /app/build/intermediates/transforms/(transform的getName)
    //增量
//    Map<File, Status> map = directoryInput.getChangedFiles()
//                if (map == null || map.size() == 0) {
//                    println(TAG + " DirectoryInput getChangedFiles empty!!")
//                    FileUtils.copyDirectory(directoryInput.file, dest)
//                    return
//                }
//                map.each {File f, Status status ->
//                    if (status == Status.ADDED) {
//                        要把文件复制过去
//                    }
//                }
    private void doTransform(@NonNull TransformInvocation transformInvocation) {
        Collection<TransformInput> mInputCollection = transformInvocation.getInputs()
        TransformOutputProvider mOutputProvider = transformInvocation.getOutputProvider()
        //此次是否是增量
        boolean isIncremental = transformInvocation.isIncremental()
        LogUtil.log(TAG, "doTransform isIncremental: %s", isIncremental)

        onBeforeTransform()

        mInputCollection.each { TransformInput input ->
            //遍历文件夹
            input.directoryInputs.each { DirectoryInput directoryInput ->
                LogUtil.log(TAG, "DirectoryInput ------  %s", directoryInput.file.getPath())
                //创建一个对应名称表示的输出目录
                // 是从0 、1、2开始递增。如果是目录，名称就是对应的数字，如果是jar包就类似0.jar
                // 位于app/build/intermediates/transforms/(transform的getName)
                File dest = mOutputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)

            }

            //遍历jar包
            input.jarInputs.each { JarInput jarInput ->
                LogUtil.log(TAG, "JarInput ------  %s", jarInput.file.getPath())
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //获取output
                def dest = mOutputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes,
                        jarInput.scopes, Format.JAR)
                //println(TAG + " JarInput -- dest " + dest.getPath())
//                String tmpPath = "${tmpDir}${File.separator}${jarInput.name.replace(':', '')}"
//                JarZipUtils.unzipJarZip(jarInput.file.absolutePath, tmpPath)
//
//                File f = new File(tmpPath)
//                mSearchThread.addPathList(f.absolutePath)
//                mSearchThread.findThread(f, f.absolutePath)
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
    }

    private void onBeforeTransform() {
        mPluginList.each { AbsBasePlugin plugin ->
            plugin.onBeforeTransform()
        }
    }

    private void onFinallyTransform() {
        mPluginList.each { AbsBasePlugin plugin ->
            plugin.onFinallyTransform()
        }
        JavaAssistHelper.getInstance().releaseClassPool()
    }
}