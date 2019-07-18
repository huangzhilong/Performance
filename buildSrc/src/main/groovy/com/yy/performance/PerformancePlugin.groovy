package com.yy.performance

import com.android.build.gradle.AppExtension
import com.yy.performance.plugin.AbsBasePlugin
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

    private final static String PLUGIN_NAME = "performance_manager"

    private PerformanceTransform mTransform

    @Override
    void apply(Project project) {

        LogUtil.log(TAG," ------ start ThreadManagerPlugin ----------")
        createExtend(project)

        if (project.hasProperty(PLUGIN_NAME)) {
            //注册PerformanceTransform
            LogUtil.log(TAG, "registerTransform PerformancePlugin")
            def android = project.extensions.getByType(AppExtension)
            mTransform = new PerformanceTransform(project)
            android.registerTransform(mTransform)

            //要放到这里面读取,transform放在里面会register失败
            project.afterEvaluate {
                List<AbsBasePlugin> plugins = readPluginExtend(project)
                mTransform.setPluginList(plugins)
            }
        }
    }

    private void createExtend(Project project) {
        project.extensions.create(PLUGIN_NAME, PluginExtendContainer, project)
    }

    private List<AbsBasePlugin> readPluginExtend(Project project) {
        PluginExtendContainer container = project[PLUGIN_NAME]
        if (!container.enable) {
            LogUtil.log(TAG, "readPluginExtend plugin not enable!!!")
            return null
        }
        List<AbsBasePlugin> pluginList = new ArrayList<>()
        container.plugins.each { PluginExtend extend ->
            if (extend.plugin != null) {
                LogUtil.log(TAG, "readPluginExtend plugin name: %s", extend.name)
                pluginList.add(extend.plugin)
            }
        }
        return pluginList
    }
}
