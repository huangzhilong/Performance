package com.yy.thread

import com.android.build.gradle.AppExtension
import com.yy.thread.plugin.IBasePlugin
import com.yy.thread.plugin.extend.PluginExtend
import com.yy.thread.plugin.extend.PluginExtendContainer
import com.yy.thread.util.LogUtil
import org.gradle.api.Plugin
import org.gradle.api.Project;

/**
 * Created by huangzhilong on 19/7/17.
 */

class ThreadManagerPlugin implements Plugin<Project>{

    private final static String TAG = "ThreadManagerPlugin"

    private final static String PLUGIN_NAME = "thread_manager"

    @Override
    void apply(Project project) {

        LogUtil.log(TAG," ------ start ThreadManagerPlugin ----------")
        createExtend(project)

        //要放到这里面读取
        project.afterEvaluate {
            List<IBasePlugin> plugins = readThreadExtend(project)
            if (plugins == null || plugins.size() == 0) {
                LogUtil.log(TAG, "not plugin end ThreadManagerPlugin")
                return
            }
        }

        //注册ThreadTransform
//        def android = project.extensions.getByType(AppExtension)
//        ThreadTransform threadTransform = new ThreadTransform(project, plugins)
//        android.registerTransform(threadTransform)
    }

    private void createExtend(Project project) {
        project.extensions.create(PLUGIN_NAME, PluginExtendContainer, project)
    }

    private List<IBasePlugin> readThreadExtend(Project project) {
        if (!project.hasProperty(PLUGIN_NAME)) {
            LogUtil.log(TAG, "readThreadExtend not plugin!!!")
            return null
        }
        PluginExtendContainer container = project[PLUGIN_NAME]
        if (!container.enable) {
            LogUtil.log(TAG, "readThreadExtend plugin not enable!!!")
            return null
        }
        List<IBasePlugin> pluginList = new ArrayList<>()
        container.plugins.each { PluginExtend extend ->
            if (extend.plugin != null) {
                LogUtil.log(TAG, "readThreadExtend plugin name: %s", extend.name)
                pluginList.add(extend.plugin)
            }
        }
        return pluginList
    }
}
