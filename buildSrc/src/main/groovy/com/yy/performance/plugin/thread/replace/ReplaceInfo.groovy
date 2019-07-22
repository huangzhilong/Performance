package com.yy.performance.plugin.thread.replace

import javassist.expr.Expr

/**
 * Created by huangzhilong on 19/7/16.
 */

class ReplaceInfo {

    public Expr mExpr

    public String dir

    public String method

    ReplaceInfo(Expr expr, String dir, String method) {
        mExpr = expr
        this.dir = dir
        this.method = method
    }
}
