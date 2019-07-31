package com.yy.performance.plugin

import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInvocation
import org.gradle.api.Project

/**
 * Created by huangzhilong on 19/6/19
 *
 * 一个transform可对应多个IBasePlugin
 */

abstract class AbsBasePlugin {

    /**
     * 开始执行之前
     */
    void onBeforeTransform(Project project, TransformInvocation transformInvocation) {

    }

    /**
     * 遍历每个class回调
     * <p>
     * 会存在多线程同时调用，但多个plugin对同一个class的操作是顺序的，安全的
     * </p>
     * @param inputFile 来源文件
     * @param srcPath 来源文件夹
     * @param className 类名
     * @param isDirectory 是否是DirectoryInput
     */
    void doHandlerEachClass(File inputFile, String srcPath, String className, boolean isDirectory) {

    }

    /**
     * 是否需要对这个jar进行操作，有一个plugin需要操作就进行操作
     * @param jarInput
     * @return
     */
    boolean isNeedHandlerJar(JarInput jarInput) {
        return true
    }

    /**
     * 每个jar包遍历完成后回调，在压缩生成新jar包前回调
     * @param jarInput
     * @param dir 解压jar的目录
     */
    void onAfterEachJar(JarInput jarInput, String dir) {

    }

    /**
     * 是否处理文件夹输入源，有一个plugin处理就需要遍历
     * @return
     */
    boolean isNeedHandlerDirectoryInput() {
        return true
    }

    /**
     * transforms运行结束
     */
    void onFinallyTransform(Project project) {

    }
}