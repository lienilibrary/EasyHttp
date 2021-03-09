package com.lieni.library.easyhttp;


import com.lieni.library.easyhttp.cookie.CookieHelper;
import com.lieni.library.easyhttp.interceptor.HeaderInterceptor;
import com.lieni.library.easyhttp.logger.HttpLogger;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EasyHttp {
    private LinkedHashMap<Class,Object> apiCache=new LinkedHashMap<>();
    private volatile static EasyHttp instance;
    private static EasyBuilder builder;
    private Retrofit retrofit;

    public static void init(EasyBuilder easyBuilder) {
        builder = easyBuilder;
    }

    private EasyHttp() {
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.readTimeout(builder.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(builder.getReadTimeout(), TimeUnit.MILLISECONDS)
                .connectTimeout(builder.getReadTimeout(), TimeUnit.MILLISECONDS)
                .cookieJar(new CookieHelper(builder.getApplication(), builder.isLoadCookie()));

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

    public static <T> T create(Class<T> service) {
        if(getInstance().apiCache.containsKey(service)){
            return (T)getInstance().apiCache.get(service);
        }else {
            T t=getInstance().retrofit.create(service);
            getInstance().apiCache.put(service,t);
            return t;
        }
    }


}
