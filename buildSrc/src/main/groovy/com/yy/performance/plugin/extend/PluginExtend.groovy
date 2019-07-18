package com.yy.performance.plugin.extend

import com.yy.performance.plugin.AbsBasePlugin

/**
 * Created by huangzhilong on 19/6/19
 */

class PluginExtend <T extends AbsBasePlugin> {

    public String name

    PluginExtend(String name) {
        this.name = name
    }

    public T plugin
}