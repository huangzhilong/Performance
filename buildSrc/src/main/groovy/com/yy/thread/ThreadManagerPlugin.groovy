package com.yy.thread

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project;

/**
 * Created by huangzhilong on 19/7/17.
 */

class ThreadManagerPlugin implements Plugin<Project>{

    private final static String TAG = "ThreadManagerPlugin"

    @Override
    void apply(Project project) {

        println(TAG + "  start ThreadManagerPlugin----------")
        //注册ThreadTransform
        def android = project.extensions.getByType(AppExtension)
       // android.registerTransform(new ThreadTransform(project))
    }
}
