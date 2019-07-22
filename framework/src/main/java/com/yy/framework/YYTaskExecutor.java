package com.yy.framework;

import android.os.Process;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangzhilong on 19/7/17.
 * 统一使用这个线程池
 */

public class YYTaskExecutor {

    public static final int THREAD_PRIORITY_BACKGROUND = Process.THREAD_PRIORITY_BACKGROUND;

    //简单线程数量设置
    private final static int MIN_THREADPOOL_SIZE = 10;
    private final static int MAX_THREADPOOL_SIZE = 20;

    private static ThreadPoolExecutor sThreadPool =
            new ThreadPoolExecutor(MIN_THREADPOOL_SIZE, MAX_THREADPOOL_SIZE, 30, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    new DefaultThreadFactory("sThreadPool-",
                    THREAD_PRIORITY_BACKGROUND));

    private static ScheduledExecutorService sScheduledPool =
            new ScheduledThreadPoolExecutor(MIN_THREADPOOL_SIZE, new DefaultThreadFactory("sThreadPool-",
                            THREAD_PRIORITY_BACKGROUND));


    public static ThreadPoolExecutor getThreadPool() {
        return sThreadPool;
    }

    public static ScheduledExecutorService getScheduledPool() {
        return sScheduledPool;
    }
}
