package com.yy.performance

import com.yy.performance.plugin.IBasePlugin
import com.yy.performance.plugin.extend.PluginExtend
import com.yy.performance.plugin.extend.PluginExtendContainer
import com.yy.performance.util.LogUtil
import org.gradle.api.Plugin
import org.gradle.api.Project;

/**
 * Created by huangzhilong on 19/7/17.
 */

class PerformancePlugin implements Plugin<Project>{

    private final static String TAG = "PerformancePlugin"

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
            //注册PerformanceTransform
            def android = project.extensions.getByType(AppExtension)
            PerformanceTransform transform = new PerformanceTransform(project, plugins)
            android.registerTransform(transform)
        }
    }

    private void createExtend(Project project) {
        project.extensions.create(PLUGIN_NAME, PluginExtendContainer, project)
    }

    private List<com.yy.performance.plugin.IBasePlugin> readThreadExtend(Project project) {
        if (!project.hasProperty(PLUGIN_NAME)) {
            LogUtil.log(TAG, "readThreadExtend not plugin!!!")
            return null
        }
        PluginExtendContainer container = project[PLUGIN_NAME]
        if (!container.enable) {
            LogUtil.log(TAG, "readThreadExtend plugin not enable!!!")
            return null
        }
        List<com.yy.performance.plugin.IBasePlugin> pluginList = new ArrayList<>()
        container.plugins.each { PluginExtend extend ->
            if (extend.plugin != null) {
                LogUtil.log(TAG, "readThreadExtend plugin name: %s", extend.name)
                pluginList.add(extend.plugin)
            }
        }
        return pluginList
    }
}
