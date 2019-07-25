package com.yy.performance.plugin.thread

import com.yy.performance.plugin.AbsBasePlugin
import com.yy.performance.util.JavaAssistHelper
import com.yy.performance.util.LogUtil
import javassist.CannotCompileException
import javassist.CtClass
import javassist.expr.*

/**
 * Created by huangzhilong on 19/7/22.
 */

abstract class BaseThreadPlugin extends AbsBasePlugin {

    protected String HANDLER_THREAD = "android.os.HandlerThread"
    protected String THREAD = "java.lang.Thread"
    protected String TIMER = "java.util.Timer"
    protected String EXECUTORS = "java.util.concurrent.Executors"
    protected String THREAD_POOL_EXECUTOR = "java.util.concurrent.ThreadPoolExecutor"

    protected String THREAD_POOL_CACHE = "newCachedThreadPool"
    protected String THREAD_POOL_FIXED = "newFixedThreadPool"
    protected String THREAD_POOL_SINGLE = "newSingleThreadExecutor"
    protected String THREAD_POOL_SINGLE_SCHEDULE = "newSingleThreadScheduledExecutor"
    protected String THREAD_POOL_SCHEDULE = "newScheduledThreadPool"

    @Override
    void doHandlerEachClass(File inputFile, String srcPath, String className, boolean isDirectory) {
        CtClass ctClass = JavaAssistHelper.getInstance().getCtClass(className)
        if (ctClass == null) {
            LogUtil.log(TAG, "not find className: %s", className)
        }
        // 通过下面的方法可以把类中所有方法调用属性等都获得到（包括调用其他类的方法和属性）
        try {
            ctClass.instrument(new ExprEditor() {
                //所有方法调用回调(包括调用其他类的方法）
                @Override
                void edit(MethodCall m) throws CannotCompileException {
                    if (m.className == EXECUTORS && (m.methodName == THREAD_POOL_CACHE
                            || m.methodName == THREAD_POOL_FIXED || m.methodName == THREAD_POOL_SINGLE
                            || m.methodName == THREAD_POOL_SINGLE_SCHEDULE || m.methodName == THREAD_POOL_SCHEDULE)) {
                        insertFindResult(EXECUTORS, className, m.methodName, m.getLineNumber(), m, srcPath)
                    }
                }

                //属性回调
                @Override
                void edit(FieldAccess f) throws CannotCompileException {
                    //会把所有用到的属性都输出，不是本类定义的，调用其他类的属性也会输出
//                        println("FieldAccess className:  " + f.getFieldName() +"  " + "  methodName:  " + f
//                                .className +  "   " + f.getField().type.name)
                }

                //对象创建回调
                @Override
                void edit(NewExpr e) throws CannotCompileException {
                    //创建线程
                    if (e.className == THREAD) {
                        insertFindResult(THREAD, className, e.constructor.longName, e.getLineNumber(), e, srcPath)
                    }
                    if (e.className == HANDLER_THREAD) {
                        insertFindResult(HANDLER_THREAD, className, e.constructor.longName, e.getLineNumber(), e, srcPath)
                    }
                    if (e.className == TIMER) {
                        insertFindResult(TIMER, className, e.constructor.longName, e.getLineNumber(), e, srcPath)
                    }
                    if (e.className == THREAD_POOL_EXECUTOR) {
                        insertFindResult(THREAD_POOL_EXECUTOR, className, e.constructor.longName,
                                e.getLineNumber(), e, srcPath)
                    }
                    boolean isSubThreadPool = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), THREAD_POOL_EXECUTOR)
                    if (isSubThreadPool) {
                        insertFindResult(THREAD_POOL_EXECUTOR, className, e.constructor.longName,
                                e.getLineNumber(), e, srcPath)
                    }
                    boolean isSubThread = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), THREAD)
                    if (isSubThread) {
                        insertFindResult(THREAD, className, e.constructor.longName, e.getLineNumber(), e, srcPath)
                    }
                    boolean isSubHandlerThread = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), HANDLER_THREAD)
                    if (isSubHandlerThread) {
                        insertFindResult(HANDLER_THREAD, className, e.constructor.longName, e.getLineNumber(), e, srcPath)
                    }
                    boolean isSubTimer = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), TIMER)
                    if (isSubTimer) {
                        insertFindResult(TIMER, className, e.constructor.longName, e.getLineNumber(), e, srcPath)
                    }
                }

                //类型判断回调
                @Override
                void edit(Instanceof i) throws CannotCompileException {

                }
            })
        } catch (Exception e) {

        } finally {
            onEndEachClass(className, srcPath)
        }
    }


    private void insertFindResult(String key, String className, String methodName, int lineNumber, Expr expr,
                                  String dir) {
        //LogUtil.log(TAG, "insertFindResult key: %s  className: %s  method: %s", key, className, methodName)
        onEachResult(key, className, methodName, lineNumber, expr, dir)
    }

    abstract void onEachResult(String key, String className, String methodName, int lineNumber, Expr expr, String dir)

    abstract void onEndEachClass(String className, String dir)
}