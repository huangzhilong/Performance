package com.yy.performance.plugin.thread

import com.android.build.api.transform.JarInput
import com.yy.performance.plugin.AbsBasePlugin
import com.yy.performance.util.LogUtil;

/**
 * Created by huangzhilong on 19/7/18.
 */

class FindThreadPlugin extends AbsBasePlugin {

    @Override
    void doHandlerEachClass(File inputFile, String srcPath, String className, boolean isDirectory) {
        LogUtil.log("FindThreadPlugin", "doHandlerEachClass className: %s  isDirectory: %s", className, isDirectory)
    }

    @Override
    boolean isNeedUnzipJar(JarInput jarInput) {
        if (jarInput.name.contains("okhttp")) {
            return true
        }
        return false
    }
}
