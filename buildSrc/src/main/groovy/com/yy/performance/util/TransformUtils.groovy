
package com.yy.performance.util
/**
 * Created by huangzhilong on 19/7/17.
 */

class TransformUtils {

    /**
     * 获取类名
     * @param filePath
     * @param dir
     * @return
     */
    static String getClassName(String filePath, String dir) {
        if (filePath == null || filePath.length() == 0 || dir == null || dir.length() == 0) {
            return null
        }
        if (!dir.endsWith(File.separator)) {
            dir = dir + File.separator
        }
        String classPath = filePath.replace(dir, '').replace('.class', '')
        String className = classPath.replaceAll(File.separator, ".")
        return className
    }
}