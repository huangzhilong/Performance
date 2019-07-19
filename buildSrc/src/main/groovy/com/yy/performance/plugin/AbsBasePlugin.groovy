package com.yy.performance.plugin

import com.android.build.api.transform.JarInput

/**
 * Created by huangzhilong on 19/6/19
 *
 * 一个transform可对应多个IBasePlugin
 */

abstract class AbsBasePlugin {

    /**
     * 开始执行之前
     */
    void onBeforeTransform() {

    }

    /**
     *  修改操作每个class
     * @param inputFile 来源文件
     * @param srcPath 来源文件夹
     * @param className 类名
     * @param isDirectory 是否是DirectoryInput
     */
    abstract void doHandlerEachClass(File inputFile, String srcPath, String className, boolean isDirectory)

    /**
     * 是否需要解压这个jar进行操作，有一个plugin需要解压就会进行解压操作
     * @param jarInput
     * @return
     */
    boolean isNeedUnzipJar(JarInput jarInput) {
        return true
    }

    /**
     * transforms运行结束
     */
    void onFinallyTransform() {

    }
}