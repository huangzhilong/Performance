package com.yy.performance.plugin.thread

import com.android.build.api.transform.JarInput
import com.yy.performance.plugin.AbsBasePlugin
import com.yy.performance.util.JavaAssistHelper
import com.yy.performance.util.LogUtil
import javassist.CannotCompileException
import javassist.CtBehavior
import javassist.CtClass
import javassist.expr.ExprEditor
import javassist.expr.FieldAccess
import javassist.expr.Instanceof
import javassist.expr.MethodCall
import javassist.expr.NewExpr
import org.gradle.api.Project

import java.util.concurrent.ConcurrentHashMap

/**
 * Created by huangzhilong on 19/7/18.
 *
 * 扫描使用了线程的plugin，项目根目录生成结果threadResult.txt
 */

class FindThreadPlugin extends AbsBasePlugin {

    private final static String TAG = "FindThreadPlugin"

    private String HANDLER_THREAD = "android.os.HandlerThread"
    private String THREAD = "java.lang.Thread"
    private String TIMER = "java.util.Timer"
    private String EXECUTORS = "java.util.concurrent.Executors"
    private String THREAD_POOL_EXECUTOR = "java.util.concurrent.ThreadPoolExecutor"

    private String THREAD_POOL_CACHE = "newCachedThreadPool"
    private String THREAD_POOL_FIXED = "newFixedThreadPool"
    private String THREAD_POOL_SINGLE = "newSingleThreadExecutor"
    private String THREAD_POOL_SINGLE_SCHEDULE = "newSingleThreadScheduledExecutor"
    private String THREAD_POOL_SCHEDULE = "newScheduledThreadPool"

    private Map<String, List<FindInfo>> mFindMap


    @Override
    boolean isNeedUnzipJar(JarInput jarInput) {
        //解压所有jar
        return true
    }

    @Override
    void onBeforeTransform(Project project) {
        mFindMap = new ConcurrentHashMap<>()
    }

    @Override
    void doHandlerEachClass(File inputFile, String srcPath, String className, boolean isDirectory) {
        CtClass ctClass = JavaAssistHelper.getInstance().getCtClass(className)
        if (ctClass == null) {
            LogUtil.log(TAG, "not find className: %s", className)
        }
        // 通过下面的方法可以把类中所有方法调用属性等都获得到（包括调用其他类的方法和属性）
        for (CtBehavior behavior : ctClass.getDeclaredBehaviors()) {
            try {
                behavior.instrument(new ExprEditor() {
                    //所有方法调用回调(包括调用其他类的方法）
                    @Override
                    void edit(MethodCall m) throws CannotCompileException {
                        if (m.className == EXECUTORS && (m.methodName == THREAD_POOL_CACHE
                                || m.methodName == THREAD_POOL_FIXED || m.methodName == THREAD_POOL_SINGLE
                                || m.methodName == THREAD_POOL_SINGLE_SCHEDULE || m.methodName == THREAD_POOL_SCHEDULE)) {
                            insertFindResult(EXECUTORS, className, m.methodName, m.getLineNumber())
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
                            insertFindResult(THREAD, className, e.constructor.longName, e.getLineNumber())
                        }
                        if (e.className == HANDLER_THREAD) {
                            insertFindResult(HANDLER_THREAD, className, e.constructor.longName, e.getLineNumber())
                        }
                        if (e.className == TIMER) {
                            insertFindResult(TIMER, className, e.constructor.longName, e.getLineNumber())
                        }
                        if (e.className == THREAD_POOL_EXECUTOR) {
                            insertFindResult(THREAD_POOL_EXECUTOR, className, e.constructor.longName, e.getLineNumber())
                        }
                        boolean isSubThreadPool = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), THREAD_POOL_EXECUTOR)
                        if (isSubThreadPool) {
                            insertFindResult(THREAD_POOL_EXECUTOR, className, e.constructor.longName, e.getLineNumber())
                        }
                        boolean isSubThread = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), THREAD)
                        if (isSubThread) {
                            insertFindResult(THREAD, className, e.constructor.longName, e.getLineNumber())
                        }
                        boolean isSubHandlerThread = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), HANDLER_THREAD)
                        if (isSubHandlerThread) {
                            insertFindResult(HANDLER_THREAD, className, e.constructor.longName, e.getLineNumber())
                        }
                        boolean isSubTimer = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), TIMER);
                        if (isSubTimer) {
                            insertFindResult(TIMER, className, e.constructor.longName, e.getLineNumber())
                        }
                    }

                    //类型判断回调
                    @Override
                    void edit(Instanceof i) throws CannotCompileException {

                    }
                })
            } catch (Exception e) {
            }
        }
    }

    private void insertFindResult(String key, String className, String methodName, int lineNumber) {
        LogUtil.log(TAG, "insertFindResult key: %s  className: %s  method: %s", key, className, methodName)
        if (mFindMap.get(key) == null) {
            List<FindInfo> list = new ArrayList<>()
            mFindMap.put(key, list)
        }
        FindInfo info = new FindInfo()
        info.className = className
        info.methodName = methodName
        info.lineNumber = lineNumber
        mFindMap.get(key).add(info)
    }

    void onFinallyTransform(Project project) {
        reportSearchResult(project)
    }

    /**
     * 输出结果
     */
    void reportSearchResult(Project project) {
        File resultFile = new File(project.rootDir.absolutePath + File.separator + "threadResult.txt")
        if (!resultFile.exists()) {
            resultFile.createNewFile()
        }
        StringBuilder str = new StringBuilder()
        if (mFindMap.size() == 0) {
            str.append("not find use Thread!!!!!!")
            resultFile.write(str.toString())
        } else {
            str.append("\n================== Search Result =====================\n\n")

            Iterator<String> keyIterator = mFindMap.keySet().iterator()
            while (keyIterator.hasNext()) {
                String key = keyIterator.next()
                List<FindInfo> infoList = mFindMap.get(key)
                if (infoList == null || infoList.size() == 0) {
                    continue
                }
                str.append("------------------ " + key + " ----------------")
                str.append("\n")
                for (int i = 0; i < infoList.size(); i++) {
                    FindInfo info = infoList.get(i)
                    str.append("className:  " + info.className + "  method: " + info.methodName + "  lineNumber: " + info
                            .getLineNumber())
                    str.append("\n")
                }
            }
            str.append("\n================== end =====================\n")
            resultFile.write(str.toString())
        }
        LogUtil.log(TAG, str.toString())
    }
}
