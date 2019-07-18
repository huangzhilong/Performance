package com.yy.performance.plugin

/**
 * Created by huangzhilong on 19/6/19
 *
 * 一个transform可对应多个IBasePlugin
 */

interface IBasePlugin {

    /**
     * 开始执行之前
     */
    void onBeforeTransform()

    /**
     * transforms运行结束
     */
    void onFinallyTransform()
}