package com.yy.performance.plugin.thread.replace

import com.android.build.api.transform.JarInput
import com.yy.performance.plugin.thread.BaseThreadPlugin
import com.yy.performance.util.JavaAssistHelper
import com.yy.performance.util.LogUtil
import javassist.CtClass
import javassist.expr.Expr

/**
 * Created by huangzhilong on 19/7/22.
 */

class ReplaceThreadPlugin extends BaseThreadPlugin {

    private final static String TAG = "ReplaceThreadTransform"

    private String OKHTTP_NAME = "com.squareup.okhttp"


    ReplaceThreadPlugin() {
    }

    @Override
    boolean isNeedHandlerJar(JarInput jarInput) {
        if (jarInput.name.startsWith(OKHTTP_NAME)) {
            return true
        } else if (jarInput.name.contains("framework")) {
            //对应模块也生成jar文件,需要把YYTaskExecutor模块解压添加到pool
            return true
        }
        return false
    }

    @Override
    void onEachResult(String key, String className, String methodName, int lineNumber, Expr expr, String dir) {
        if (!className.equals("okhttp3.ConnectionPool")) {
            return
        }
        LogUtil.log(TAG, "start onEachResult class: %s dir: %s", className, dir)
        CtClass ctClass = JavaAssistHelper.getInstance().getCtClass(className)
        if (ctClass == null) {
            return
        }
        if (ctClass.isFrozen()) {
            ctClass.defrost()
        }
        try {
//            JavaAssistHelper.getInstance().importClass("com.yy.framework.YYTaskExecutor")
//            expr.replace('$_ = com.yy.framework.YYTaskExecutor.getThreadPool();')
//            ctClass.writeFile(dir)

            //获取属性方式  这样可用编译通过，但是 static field remove只是移除了定义，导致静态代码块找不到定义直接崩溃
//            CtField field = ctClass.getDeclaredField("executor")
//            ctClass.removeField(field)
//            JavaAssistHelper.getInstance().importClass("com.yy.framework.YYTaskExecutor")
//            CtField mValue = CtField.make('''public int value = com.yy.framework.YYTaskExecutor.THREAD_PRIORITY_BACKGROUND;''', ctClass)
//            ctClass.addField(mValue)
//
//            CtField mValue1 = CtField.make('''public java.util.concurrent.Executor myExecutor = com.yy.framework.YYTaskExecutor.getThreadPool();''', ctClass)
//            ctClass.addField(mValue1)
//            ctClass.writeFile(dir)
        } catch (Exception e) {
            LogUtil.log(TAG, "onEachResult ex: %s", e)
        }
        LogUtil.log(TAG, "onReplace class: %s  method: %s  line: %s", className, methodName, lineNumber)
    }

    @Override
    void onEndEachClass(String className, String dir) {
    }
}
