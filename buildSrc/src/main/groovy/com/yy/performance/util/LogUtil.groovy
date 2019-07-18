
package com.yy.thread.util

/**
 * Created by huangzhilong on 19/7/17.
 */

class LogUtil {

    public static log(String tag, String format, Object... argv) {
        println("[${tag}]\t${String.format(format, argv)}")
    }
}