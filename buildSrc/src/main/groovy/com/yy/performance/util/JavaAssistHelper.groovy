package com.yy.performance.util

import javassist.ClassPath
import javassist.ClassPool
import javassist.CtClass
import org.gradle.api.Project;

/**
 * Created by huangzhilong on 19/7/18.
 *
 * 统一管理JavaAssist ClassPool
 */

class JavaAssistHelper {

    private final static String TAG = "JavaAssistHelper"

    private static class SingleHolder {
        public static JavaAssistHelper instance = new JavaAssistHelper()
    }

    static JavaAssistHelper getInstance() {
        return SingleHolder.instance
    }

    private JavaAssistHelper() {
    }

    private ClassPool mClassPool
    private List<ClassPath> mClassPathList

    /**
     * 初始化classPool
     * @param project
     */
    void initPool(Project project) {
        LogUtil.log(TAG, "initPool")
        mClassPool = new ClassPool(false)
        mClassPathList = new ArrayList<>()
        //添加系统类
        ClassPath sysPath = mClassPool.appendSystemPath()
        mClassPathList.add(sysPath)
        //添加android对应类，这样才能扫到android包底下的类
        ClassPath androidPath = mClassPool.appendClassPath(project.android.bootClasspath[0].toString())
        mClassPathList.add(androidPath)
    }

    void addClassPath(String path) {
        if (mClassPool == null) {
            LogUtil.log(TAG, "init pools first!!!!")
            return
        }
        ClassPath classPath = mClassPool.appendClassPath(path)
        mClassPathList.add(classPath)
    }

    CtClass getCtClass(String className) {
        if (className == null || className.length() == 0) {
            return null
        }
        if (mClassPool == null) {
            LogUtil.log(TAG, "init pools first!!!!")
            return
        }
        return mClassPool.getCtClass(className)
    }

    ClassPool getClassPool() {
        return mClassPool
    }

    /**
     * 是否资源
     */
    void releaseClassPool() {
        if (mClassPool == null) {
            return
        }
        mClassPathList.each { ClassPath path ->
            mClassPool.removeClassPath(path)
        }
        mClassPool.clearImportedPackages()
    }
}
