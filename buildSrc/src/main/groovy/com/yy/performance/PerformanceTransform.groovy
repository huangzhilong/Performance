package com.yy.performance

import com.android.annotations.NonNull
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.ImmutableSet
import com.yy.performance.plugin.IBasePlugin
import com.yy.performance.util.LogUtil
import org.gradle.api.Project

/**
 * Created by huangzhilong on 19/7/17.
 */

class PerformanceTransform extends Transform {

    private final static String TAG = "PerformanceTransform"

    private Project mProject
    //同一个Transform可对应多个plugin，避免多个Transform解压遍历操作
    private List<IBasePlugin> mPluginList

    PerformanceTransform(Project project, List<IBasePlugin> pluginList) {
        mProject = project
        mPluginList = pluginList
    }

    @Override
    String getName() {
        return TAG
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        //表示只输入class文件，还可以是RESOURCES
        return ImmutableSet.<QualifiedContent.ContentType> of(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    //支持增量 If it does, then the TransformInput may contain a list of changed/removed/added files
    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(@NonNull TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        try {
            doTransform(transformInvocation)
        } catch (Exception e) {
            LogUtil.log(TAG, "doTransform get ex: %s", e)
        } finally {
            onFinallyTransform()
        }
    }

    private void doTransform(@NonNull TransformInvocation transformInvocation) {
        Collection<TransformInput> mInputCollection = transformInvocation.getInputs()
        TransformOutputProvider mOutputProvider = transformInvocation.getOutputProvider()
        //此次是否是增量
        boolean isIncremental = transformInvocation.isIncremental()
        LogUtil.log(TAG, "doTransform isIncremental: %s", isIncremental)

        onBeforeTransform()
    }

    private void onBeforeTransform() {
        mPluginList.each {IBasePlugin plugin ->
            plugin.onBeforeTransform()
        }
    }

    private void onFinallyTransform() {
        mPluginList.each { IBasePlugin plugin ->
            plugin.onFinallyTransform()
        }
    }
}