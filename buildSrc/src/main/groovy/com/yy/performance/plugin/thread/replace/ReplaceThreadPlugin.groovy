package com.yy.performance.plugin.thread.replace


import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.yy.performance.plugin.AbsBasePlugin
import com.yy.performance.util.JarZipUtils
import com.yy.performance.util.JavaAssistHelper
import com.yy.performance.util.LogUtil
import javassist.CannotCompileException
import javassist.CtClass
import javassist.CtMethod
import javassist.expr.ExprEditor
import javassist.expr.NewExpr
import org.gradle.api.Project

/**
 * Created by huangzhilong on 19/7/22.
 */

class ReplaceThreadPlugin extends AbsBasePlugin {

    private final static String TAG = "ReplaceThreadPlugin"

    private String FRAMEWORK_MODULE = "framework"

    private static final GLIDE_ARTIFACT = "com.github.bumptech.glide:glide"
    private static final OKHTTP_ARTIFACT = "com.squareup.okhttp3:okhttp"

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
                    JavaAssistHelper.getInstance().importClass("com.yy.framework.YYTaskExecutor")
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
        } else if (jarInput.name.startsWith(OKHTTP_ARTIFACT)) {
            return true
        }
        return false
    }

    @Override
    void onAfterEachJar(JarInput jarInput, String dir) {
        if (jarInput.name.contains(GLIDE_ARTIFACT)) {
            LogUtil.log(TAG, "onAfterEachJar start hook ThreadPool name: %s", jarInput.name)
            //替换GlideExecutor里面的线程池
            CtClass ctClass = JavaAssistHelper.getInstance().getCtClass("com.bumptech.glide.load.engine.executor.GlideExecutor")
            JavaAssistHelper.getInstance().importClass("com.bumptech.glide.load.engine.executor.GlideExecutor")
            def mThreadPoolMethod = ["newSourceExecutor", "newDiskCacheExecutor", "newUnlimitedSourceExecutor", "newAnimationExecutor"]
            for (int i = 0; i < mThreadPoolMethod.size(); i++) {
                String methodName = mThreadPoolMethod.get(i)
                CtMethod[] ctMethods = ctClass.getDeclaredMethods(methodName)
                for (int j = 0; j < ctMethods.length; j++) {
                    CtMethod m = ctMethods[j]
                    m.setBody(''' return new GlideExecutor(com.yy.framework.YYTaskExecutor.getThreadPool());''')
                }
            }
            ctClass.writeFile(dir)
        } else if (jarInput.name.contains(OKHTTP_ARTIFACT)) {
            LogUtil.log(TAG, "onAfterEachJar start hook ThreadPool name: %s", jarInput.name)
            //替换OkHttp线程池
            def mThreadClass = ["okhttp3.ConnectionPool", "okhttp3.Dispatcher", "okhttp3.internal.cache.DiskLruCache",
                                "okhttp3.internal.http2.Http2Connection"]
            for (int i = 0; i < mThreadClass.size(); i++) {
                String className = mThreadClass.get(i)
                CtClass ctClass = JavaAssistHelper.getInstance().getCtClass(className)
                ctClass.instrument(new ExprEditor() {
                    void edit(NewExpr e) throws CannotCompileException {
                        if (e.className == "java.util.concurrent.ThreadPoolExecutor") {
                            e.replace('$_ = com.yy.framework.YYTaskExecutor.getThreadPool();')
                        }
                    }
                })
                LogUtil.log(TAG, "handler threadPool class: %s", className)
                ctClass.writeFile(dir)
            }
        }
    }
}
