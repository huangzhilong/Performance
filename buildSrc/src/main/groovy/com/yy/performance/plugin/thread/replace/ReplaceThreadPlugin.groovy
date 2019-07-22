package com.yy.performance.plugin.thread.replace

import com.android.build.api.transform.JarInput
import com.yy.performance.plugin.thread.BaseThreadPlugin
import com.yy.performance.util.JavaAssistHelper
import com.yy.performance.util.LogUtil
import javassist.CtClass
import javassist.expr.Expr

import java.util.concurrent.ConcurrentHashMap


/**
 * Created by huangzhilong on 19/7/22.
 */

class ReplaceThreadPlugin extends BaseThreadPlugin {

    private final static String TAG = "ReplaceThreadTransform"

    private String OKHTTP_NAME = "com.squareup.okhttp"

    private Map<String, List<ReplaceInfo>> mReplaceMap

    ReplaceThreadPlugin() {
        mReplaceMap = new ConcurrentHashMap<>()
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
        if (!className.contains("okhttp3")) {
            return
        }
//        LogUtil.log(TAG, "start onEachResult class: %s", className)
//        CtClass ctClass = JavaAssistHelper.getInstance().getCtClass(className)
//        if (ctClass == null) {
//            return
//        }
//        try {
//            //JavaAssistHelper.getInstance().importClass("com.yy.framework.YYTaskExecutor")
//            expr.replace('$_ = null;')
//            ctClass.writeFile(dir)
//        } catch (Exception e) {
//            LogUtil.log(TAG, "onEachResult ex: %s", e)
//        }
        LogUtil.log(TAG, "onReplace class: %s  method: %s  line: %s", className, methodName, lineNumber)
    }

    @Override
    void onEndEachClass(String className, String dir) {
    }
}
