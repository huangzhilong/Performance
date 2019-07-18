package com.yy.thread.plugin.extend

import com.yy.thread.plugin.IBasePlugin
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * Created by huangzhilong on 19/6/19
 */

class PluginExtendContainer {

    /**
     * 插件是否可用
     */
    boolean enable

    NamedDomainObjectContainer<PluginExtend<IBasePlugin>> plugins

    PluginExtendContainer(Project project) {
        plugins = project.container(PluginExtend)
    }

    //gradle配置使用
    void contanierPlugins(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, plugins)
    }
}