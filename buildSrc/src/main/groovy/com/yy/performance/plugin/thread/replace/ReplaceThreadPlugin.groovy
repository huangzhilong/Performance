package com.yy.performance.plugin.thread.replace


import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.yy.performance.plugin.AbsBasePlugin
import com.yy.performance.util.JarZipUtils
import com.yy.performance.util.JavaAssistHelper
import com.yy.performance.util.LogUtil
import javassist.CtClass
import javassist.CtMethod
import org.gradle.api.Project

/**
 * Created by huangzhilong on 19/7/22.
 */

class ReplaceThreadPlugin extends AbsBasePlugin {

    private final static String TAG = "ReplaceThreadPlugin"

    private String FRAMEWORK_MODULE = "framework"
    private static final GLIDE_ARTIFACT = "com.github.bumptech.glide:glide"

    ReplaceThreadPlugin() {
    }

    //需要先把YYTaskExecutor模块解压添加到pool，项目里的framework模块也会生成jar。
    //避免处理glide等线程接管sdk时还没把YYTaskExecutor添加到pool中
    @Override
    void onBeforeTransform(Project project, TransformInvocation transformInvocation) {
        Collection<TransformInput> mInputCollection = transformInvocation.getInputs()
        mInputCollection.each { TransformInput input ->
            //遍历jar包
            input.jarInputs.each { JarInput jarInput ->
                if (jarInput.name.contains(FRAMEWORK_MODULE)) {
                    String unzipTmp = "${project.buildDir.absolutePath}${File.separator}tmp${File.separator}" + FRAMEWORK_MODULE
                    unzipTmp = "${unzipTmp}${File.separator}${jarInput.name.replace(':', '')}"
                    JarZipUtils.unzipJarZip(jarInput.file.absolutePath, unzipTmp)
                    //加入classPool
                    JavaAssistHelper.getInstance().addClassPath(unzipTmp)
                    LogUtil.log(TAG, "onBeforeTransform add framework to pool")
                    return
                }
            }
        }
    }

    @Override
    boolean isNeedHandlerJar(JarInput jarInput) {
        if (jarInput.name.startsWith(GLIDE_ARTIFACT)) {
            return true
        }
        return false
    }

    @Override
    void onAfterEachJar(JarInput jarInput, String dir) {
        if (jarInput.name.contains(GLIDE_ARTIFACT)) {
            LogUtil.log(TAG, "onAfterEachJar name: %s", jarInput.name)
            try {
                //替换GlideExecutor里面的线程池
                CtClass ctClass = JavaAssistHelper.getInstance().getCtClass("com.bumptech.glide.load.engine.executor.GlideExecutor")
                JavaAssistHelper.getInstance().importClass("com.yy.framework.YYTaskExecutor")
                JavaAssistHelper.getInstance().importClass("com.bumptech.glide.load.engine.executor.GlideExecutor")
                def mThreadPoolMethod = ["newSourceExecutor", "newDiskCacheExecutor", "newUnlimitedSourceExecutor", "newAnimationExecutor"]
                for (int i = 0; i < mThreadPoolMethod.size(); i++) {
                    String methodName = mThreadPoolMethod.get(i)
                    CtMethod[] ctMethods = ctClass.getDeclaredMethods(methodName)
                    LogUtil.log(TAG, "find name: %s  count: %s", methodName, ctMethods == null ? 0 : ctMethods.length)
                    for (int j = 0; j < ctMethods.length; j++) {
                        CtMethod m = ctMethods[j]
                        m.setBody(''' return new GlideExecutor(com.yy.framework.YYTaskExecutor.getThreadPool());''')
                    }
                }
                ctClass.writeFile(dir)
            } catch (Exception e) {
                LogUtil.log(TAG, "onEndEachClass e: %s", e)
            }
        }
    }
}
