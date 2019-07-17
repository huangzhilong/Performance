package com.yy.framework.net;

import android.text.TextUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by huangzhilong on 18/9/10.
 */

public class OkHttpUtil {

    private static final int IO_TIME_OUT = 30;
    private static final int CONNECT_TIME_OUT = 10;
    private OkHttpClient mOkHttpClient;

    private static final OkHttpUtil mOkHttpUtil = new OkHttpUtil();

    public static OkHttpUtil getInstance() {
        return mOkHttpUtil;
    }

    private OkHttpUtil() {
        initClient();
    }

    private void initClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(CONNECT_TIME_OUT, TimeUnit.SECONDS)
                .readTimeout(IO_TIME_OUT, TimeUnit.SECONDS)
                .writeTimeout(IO_TIME_OUT, TimeUnit.SECONDS)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
        mOkHttpClient = builder.build();
    }

    /**
     * 执行请求
     * @param url
     * @return
     */
    public void execRequest(final String url) {
        Request request = new Request.Builder().url(url).build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });
    }
}
