package com.yy.performance.plugin.extend
/**
 * Created by huangzhilong on 19/6/19
 */

class PluginExtend <T extends com.yy.performance.plugin.IBasePlugin> {

    public String name

    PluginExtend(String name) {
        this.name = name
    }

    public T plugin
}