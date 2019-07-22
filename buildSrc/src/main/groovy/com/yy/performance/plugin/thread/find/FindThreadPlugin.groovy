package com.yy.performance.plugin.thread.find

import com.yy.performance.plugin.thread.BaseThreadPlugin
import com.yy.performance.util.LogUtil
import javassist.expr.Expr
import org.gradle.api.Project

import java.util.concurrent.ConcurrentHashMap

/**
 * Created by huangzhilong on 19/7/18.
 *
 * 扫描使用了线程的plugin，项目根目录生成结果threadResult.txt
 */

class FindThreadPlugin extends BaseThreadPlugin {

    private final static String TAG = "FindThreadPlugin"

    private Map<String, List<FindInfo>> mFindMap


    @Override
    void onBeforeTransform(Project project) {
        mFindMap = new ConcurrentHashMap<>()
    }

    @Override
    void onEachResult(String key, String className, String methodName, int lineNumber, Expr expr, String dir) {
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

    @Override
    void onEndEachClass(String className, String dir) {

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
