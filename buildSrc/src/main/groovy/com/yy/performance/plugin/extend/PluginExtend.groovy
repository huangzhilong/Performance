package com.yy.thread.plugin.extend

import com.yy.thread.plugin.IBasePlugin

/**
 * Created by huangzhilong on 19/6/19
 */

class PluginExtend <T extends IBasePlugin> {

    public String name

    PluginExtend(String name) {
        this.name = name
    }

    public T plugin
}