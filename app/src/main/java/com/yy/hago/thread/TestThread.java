package com.yy.hago.thread;

import android.os.HandlerThread;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.yy.framework.DefaultThreadFactory;
import com.yy.framework.YYTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangzhilong on 19/6/19.
 *
 * 测试线程
 */

public class TestThread {

    private ExecutorService mExecutors;

    private MyThreadPoolExecutor mMyThreadPoolExecutor;

    private HandlerThread mHandlerThread;

    private ExecutorService mExecutorService = new ThreadPoolExecutor(10, 20,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new DefaultThreadFactory("Test1-",
            YYTaskExecutor.THREAD_PRIORITY_BACKGROUND));



    public void startMultiThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("Thread", "start------");
                try {
                    Thread.sleep(30000000000000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        mHandlerThread = new HandlerThread("TestHandler");
        mHandlerThread.start();

        mMyThreadPoolExecutor = new MyThreadPoolExecutor(10, 20,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new DefaultThreadFactory("Test2-", YYTaskExecutor.THREAD_PRIORITY_BACKGROUND));

        mExecutors = Executors.newFixedThreadPool(10,
                new DefaultThreadFactory("Test3-", YYTaskExecutor.THREAD_PRIORITY_BACKGROUND));

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.i("Thread", "!!!!!!!!!!!!!!");
            }
        };
        for (int i = 0; i < 20; i++) {
            mExecutors.execute(runnable);
            mMyThreadPoolExecutor.execute(runnable);
            mExecutorService.execute(runnable);
        }
    }
}
