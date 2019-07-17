package com.yy.framework;

import java.util.concurrent.ThreadFactory;

/**
 * Created by huangzhilong on 19/7/17.
 */

public class DefaultThreadFactory implements ThreadFactory {

    private String mThreadName;

    private int mThreadLevel;

    public DefaultThreadFactory(String threadName, int level) {
        mThreadName = threadName;
        threadNum = 0;
        mThreadLevel = level;
    }

    int threadNum = 0;

    @Override
    public Thread newThread(Runnable runnable) {
        final Thread result = new Thread(runnable, mThreadName + threadNum) {
            @Override
            public void run() {
                setPriority(mThreadLevel);
                super.run();
            }
        };
        threadNum++;
        return result;
    }
}
