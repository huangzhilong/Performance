package com.yy.thread

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.google.common.collect.ImmutableSet
import com.yy.performance.thread.SearchThread
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project

/**
 * Created by huangzhilong on 19/6/13.
 *
 */

class ThreadTransform extends Transform {

    def TAG = "ThreadTransform"

    private Project project

    private String tmpDir

    private SearchThread mSearchThread

    ThreadTransform(Project project) {
        System.out.println(TAG)
        this.project = project
        tmpDir = "${project.buildDir.absolutePath}${File.separator}tmp${File.separator}" + getName()
        mSearchThread = new SearchThread(project)
    }

    @Override
    String getName() {
        return "ThreadTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        //表示只输入class文件，还可以时RESOURCES
        return ImmutableSet.<QualifiedContent.ContentType> of(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
        //return ImmutableSet.<QualifiedContent.Scope> of (QualifiedContent.Scope.PROJECT)
    }

    //支持增量 If it does, then the TransformInput may contain a list of changed/removed/added files
    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(@NonNull TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        Collection<TransformInput> mInputCollection = transformInvocation.getInputs()
        TransformOutputProvider mOutputProvider = transformInvocation.getOutputProvider()
        def isIncremental = transformInvocation.isIncremental()

        println(TAG + " start transform isIncremental-------- " + isIncremental)

        mInputCollection.each { TransformInput input ->
            //遍历文件夹
            input.directoryInputs.each { DirectoryInput directoryInput ->
                println(TAG + " DirectoryInput ------ " + directoryInput.file.getPath())

                //获取output
                // getContentLocation方法相当于创建一个对应名称表示的目录
                // 是从0 、1、2开始递增。如果是目录，名称就是对应的数字，如果是jar包就类似0.jar
                // /app/build/intermediates/transforms/(transform的getName)
                //增量
//                Map<File, Status> map = directoryInput.getChangedFiles()
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
                def dest = mOutputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes,
                                directoryInput.scopes, Format.DIRECTORY)
                //println(TAG + " DirectoryInput -- dest " + dest.getPath())

                mSearchThread.addPathList(directoryInput.file.absolutePath)
                mSearchThread.findThread(directoryInput.file, directoryInput.file.absolutePath)

                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            //遍历jar
            input.jarInputs.each { JarInput jarInput ->
               // println(TAG + " JarInput ------ " + jarInput.file.getPath() + "  " + jarInput.getStatus().name())
                //重命名输出文件（因为可能同名。会覆盖冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //获取output
                def dest = mOutputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes,
                        jarInput.scopes, Format.JAR)
                //println(TAG + " JarInput -- dest " + dest.getPath())
                String tmpPath = "${tmpDir}${File.separator}${jarInput.name.replace(':', '')}"
                JarZipUtils.unzipJarZip(jarInput.file.absolutePath, tmpPath)

                File f = new File(tmpPath)
                mSearchThread.addPathList(f.absolutePath)
                mSearchThread.findThread(f, f.absolutePath)
                FileUtils.copyFile(jarInput.file, dest)
            }
        }

        //遍历完成
        mSearchThread.reportSearchResult()
    }
}