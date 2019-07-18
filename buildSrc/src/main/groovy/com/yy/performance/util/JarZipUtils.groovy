package com.yy.performance.util

import java.util.zip.ZipFile

/**
 * Created by huangzhilong on 19/6/14.
 *
 */

class JarZipUtils {

    private final static String TAG = "JarZipUtils"

    /**
     * 解压jar包
     * @param zipFilePath 要解压的文件路径
     * @param tmpDir 解压存放临时文件
     * @return 解压后的文件地址，可能为空（表示解压失败）
     */
    static String unzipJarZip(String zipFilePath, String tmpDir) {
        ZipFile file
        try {
            file = new ZipFile(zipFilePath)
            if (file.size() == 0) {
                return null
            }
            AntBuilder antBuilder = new AntBuilder()
            antBuilder.unzip(src: zipFilePath, dest: tmpDir, overwrite: true)
        } catch (Exception e) {
            LogUtil.log(TAG, "unzipJarZip error: %s", zipFilePath)
        } finally {
            if (file != null) {
                file.close()
            }
        }
        return null
    }

    /**
     * 压缩文件
     * @param filePath
     * @param zipFilePath
     */
    static void zipJarZip(String filePath, String zipFilePath) {
        AntBuilder antBuilder = new AntBuilder()
        antBuilder.zip(destfile: zipFilePath, basedir: filePath)
    }

}