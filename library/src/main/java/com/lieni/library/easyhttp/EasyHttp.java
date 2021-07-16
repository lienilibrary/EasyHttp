package com.lieni.library.easyhttp;


import com.lieni.library.easyhttp.interceptor.HeaderInterceptor;
import com.lieni.library.easyhttp.logger.HttpLogger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EasyHttp {
    private final LinkedHashMap<String, Object> apiCache = new LinkedHashMap<>();
    private volatile static EasyHttp instance;
    private static EasyBuilder builder;
    private final Retrofit retrofit;
    private static final int max = 10;

    private EasyHttp() {
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.readTimeout(builder.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(builder.getReadTimeout(), TimeUnit.MILLISECONDS)
                .connectTimeout(builder.getReadTimeout(), TimeUnit.MILLISECONDS);

        //cookie缓存
        if (builder.getCookieJar() != null) {
            okHttpBuilder.cookieJar(builder.getCookieJar());
        }
        //拦截器
        for (Interceptor interceptor : builder.getInterceptors()) {
            okHttpBuilder.addInterceptor(interceptor);
        }
        //请求头
        if (builder.getHeaders().size() > 0) {
            okHttpBuilder.addInterceptor(new HeaderInterceptor(builder.getHeaders()));
        }
        //日志
        if (builder.isLog()) {
            okHttpBuilder.addNetworkInterceptor(new HttpLoggingInterceptor(new HttpLogger()).setLevel(HttpLoggingInterceptor.Level.BODY));
        }
        OkHttpClient client = okHttpBuilder.build();


        //retrofit
        retrofit = new Retrofit.Builder()
                .baseUrl(builder.getBaseUrl())
                .client(client)
                .addConverterFactory(builder.getConvertFactory() != null ? builder.getConvertFactory() : GsonConverterFactory.create())
                .build();
    }

    private static EasyHttp getInstance() {
        if (instance == null) {
            synchronized (EasyHttp.class) {
                if (instance == null) {
                    instance = new EasyHttp();
                }
            }
        }
        return instance;
    }

    private <T> T get(Class<T> service) {
        if (apiCache.containsKey(service.getName())) {
            return (T) apiCache.get(service.getName());
        } else {
            T t = retrofit.create(service);
            apiCache.put(service.getName(), t);
            afterApiInsert();
            return t;
        }
    }

    private synchronized void afterApiInsert() {
        if (apiCache.size() > max) {
            int index = 0;
            for (Map.Entry<String, Object> api : apiCache.entrySet()) {
                index++;
                if (index > max) apiCache.remove(api.getKey());
            }
        }
    }

    public static void init(EasyBuilder easyBuilder) {
        builder = easyBuilder;
    }

    public static <T> T create(Class<T> service) {
        return getInstance().get(service);
    }


}
