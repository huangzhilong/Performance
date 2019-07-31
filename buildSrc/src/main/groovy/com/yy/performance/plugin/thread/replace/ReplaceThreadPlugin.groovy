package com.yy.performance.plugin.thread.replace

import com.android.build.api.transform.JarInput
import com.yy.performance.plugin.AbsBasePlugin
import com.yy.performance.util.JavaAssistHelper
import com.yy.performance.util.LogUtil
import javassist.CtClass
import javassist.CtMethod

/**
 * Created by huangzhilong on 19/7/22.
 */

class ReplaceThreadPlugin extends AbsBasePlugin {

    private final static String TAG = "ReplaceThreadPlugin"

    private String OKHTTP_NAME = "com.squareup.okhttp"
    private static final GLIDE_ARTIFACT = "com.github.bumptech.glide:glide"

    ReplaceThreadPlugin() {
    }

    @Override
    boolean isNeedHandlerJar(JarInput jarInput) {
        if (jarInput.name.startsWith(GLIDE_ARTIFACT)) {
            return true
        } else if (jarInput.name.contains("framework")) {
            //对应模块也生成jar文件,需要把YYTaskExecutor模块解压添加到pool
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
