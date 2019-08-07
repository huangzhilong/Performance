package com.yy.hago.thread;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.yy.framework.DefaultThreadFactory;
import com.yy.framework.YYTaskExecutor;
import com.yy.framework.net.OkHttpUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final String GET_BUILD_URL = "https://ci.yy.com/jenkins2/view/android-app/job/hiyo-android77/";

    private ExecutorService mExecutors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(new Runnable() {
            @Override
            public void run() {
                new TestThread().startMultiThread();
            }
        }).start();

        mExecutors = Executors.newFixedThreadPool(10,
                new DefaultThreadFactory("Test3-", YYTaskExecutor.THREAD_PRIORITY_BACKGROUND));

        startHttp();
    }

    private ScheduledExecutorService pool = new ScheduledThreadPoolExecutor(10,
            new DefaultThreadFactory("Test4-", YYTaskExecutor.THREAD_PRIORITY_BACKGROUND));

    int i = 0;
    private void startHttp() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                OkHttpUtil.getInstance().execRequest(GET_BUILD_URL + i);
                pool.schedule(this, 500, TimeUnit.MILLISECONDS);
                i++;
            }
        };
        r.run();
    }

}
