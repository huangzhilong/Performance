package com.yy.performance.plugin.thread.find

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.yy.performance.plugin.AbsBasePlugin
import com.yy.performance.util.JavaAssistHelper
import com.yy.performance.util.LogUtil
import javassist.CannotCompileException
import javassist.CtClass
import javassist.expr.*
import org.gradle.api.Project

import java.util.concurrent.ConcurrentHashMap

/**
 * Created by huangzhilong on 19/7/18.
 *
 * 扫描使用了线程的plugin，项目根目录生成结果threadResult.txt
 */

class FindThreadPlugin extends AbsBasePlugin {

    private final static String TAG = "FindThreadPlugin"

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

    private Map<String, List<FindInfo>> mFindMap

    @Override
    void onBeforeTransform(Project project, TransformInvocation transformInvocation) {
        super.onBeforeTransform(project, transformInvocation)
        mFindMap = new ConcurrentHashMap<>()
        //需要把所有class添加到pool里，避免查找ThreadPoolExecutor的子类找不到class失败
        Collection<TransformInput> mInputCollection = transformInvocation.getInputs()
        mInputCollection.each { TransformInput input ->
            //遍历文件夹
            input.directoryInputs.each { DirectoryInput directoryInput ->
                JavaAssistHelper.getInstance().addClassPath(directoryInput.file.absolutePath)
            }

            //遍历jar包
            input.jarInputs.each { JarInput jarInput ->
                JavaAssistHelper.getInstance().addClassPath(jarInput.file.absolutePath)
            }
        }
    }


    @Override
    boolean isNeedHandlerJar(JarInput jarInput) {
        return true
    }

    @Override
    void doHandlerEachClass(String name, File inputFile, String srcPath, String className, boolean isDirectory) {
        // 通过下面的方法可以把类中所有方法调用属性等都获得到（包括调用其他类的方法和属性）
        try {
            CtClass ctClass = JavaAssistHelper.getInstance().getCtClass(className)
            if (ctClass == null) {
                return
            }
            ctClass.instrument(new ExprEditor() {
                //所有方法调用回调(包括调用其他类的方法）
                @Override
                void edit(MethodCall m) throws CannotCompileException {
                    if (m.className == EXECUTORS && (m.methodName == THREAD_POOL_CACHE
                            || m.methodName == THREAD_POOL_FIXED || m.methodName == THREAD_POOL_SINGLE
                            || m.methodName == THREAD_POOL_SINGLE_SCHEDULE || m.methodName == THREAD_POOL_SCHEDULE)) {
                        onEachResult(EXECUTORS, name, className, m.methodName, m.getLineNumber())
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
                        onEachResult(THREAD, name, className, e.className, e.getLineNumber())
                        return
                    }
                    if (e.className == HANDLER_THREAD) {
                        onEachResult(HANDLER_THREAD, name, className, e.className, e.getLineNumber())
                        return
                    }
                    if (e.className == TIMER) {
                        onEachResult(TIMER, name, className, e.className, e.getLineNumber())
                        return
                    }
                    if (e.className == THREAD_POOL_EXECUTOR) {
                        onEachResult(THREAD_POOL_EXECUTOR, name, className, e.className, e.getLineNumber())
                        return
                    }

                    boolean isSubThreadPool = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), THREAD_POOL_EXECUTOR)
                    if (isSubThreadPool) {
                        onEachResult(THREAD_POOL_EXECUTOR, name, className, e.className, e.getLineNumber())
                        return
                    }
                    boolean isSubThread = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), THREAD)
                    if (isSubThread) {
                        onEachResult(THREAD, name, className, e.className, e.getLineNumber())
                        return
                    }
                    boolean isSubHandlerThread = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), HANDLER_THREAD)
                    if (isSubHandlerThread) {
                        onEachResult(HANDLER_THREAD, name, className, e.className, e.getLineNumber())
                        return
                    }
                    boolean isSubTimer = JavaAssistHelper.getInstance().isSubClass(e.getCtClass(), TIMER)
                    if (isSubTimer) {
                        onEachResult(TIMER, name, className, e.className, e.getLineNumber())
                    }
                }

                //类型判断回调
                @Override
                void edit(Instanceof i) throws CannotCompileException {

                }
            })
        } catch (Exception e) {
            LogUtil.log(TAG, "doHandlerEachClass ex: %s", e)
        }
    }

    void onEachResult(String key, String jarName, String className, String methodName, int lineNumber) {
        if (mFindMap.get(key) == null) {
            List<FindInfo> list = new ArrayList<>()
            mFindMap.put(key, list)
        }
        FindInfo info = new FindInfo()
        info.jarName = jarName
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
            str.append("\n================== Search Result =====================\n")

            Iterator<String> keyIterator = mFindMap.keySet().iterator()
            while (keyIterator.hasNext()) {
                String key = keyIterator.next()
                List<FindInfo> infoList = mFindMap.get(key)
                if (infoList == null || infoList.size() == 0) {
                    continue
                }
                str.append("\n")
                str.append("------------------ " + key + " ----------------")
                str.append("\n")
                for (int i = 0; i < infoList.size(); i++) {
                    FindInfo info = infoList.get(i)
                    str.append("jarName: " + info.jarName + "  className:  " + info.className + "  method: " +
                            info.methodName + "  lineNumber: " + info.getLineNumber())
                    str.append("\n")
                }
            }
            str.append("\n================== end =====================\n")
            resultFile.write(str.toString())
        }
        LogUtil.log(TAG, str.toString())
    }
}
