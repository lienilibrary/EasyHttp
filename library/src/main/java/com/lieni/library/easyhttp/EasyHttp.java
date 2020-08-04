package com.lieni.library.easyhttp;


import com.lieni.library.easyhttp.cookie.CookieHelper;
import com.lieni.library.easyhttp.interceptor.HeaderInterceptor;
import com.lieni.library.easyhttp.logger.HttpLogger;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EasyHttp {

    private volatile static EasyHttp instance;
    private static EasyBuilder builder;
    private Retrofit retrofit;

    public static void init(EasyBuilder easyBuilder){
        builder=easyBuilder;
    }
    private EasyHttp(){
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.readTimeout(builder.getReadTimeout(), TimeUnit.MILLISECONDS)
        .writeTimeout(builder.getReadTimeout(), TimeUnit.MILLISECONDS)
        .connectTimeout(builder.getReadTimeout(), TimeUnit.MILLISECONDS)
        .cookieJar(new CookieHelper(builder.getApplication(),builder.isLoadCookie()))
        .addNetworkInterceptor(new HttpLoggingInterceptor(new HttpLogger()).setLevel(HttpLoggingInterceptor.Level.BODY));
        //拦截器
        for (Interceptor interceptor:builder.getInterceptors()){
            okHttpBuilder.addInterceptor(interceptor);
        }
        //请求头
        if(builder.getHeaders().size()>0){
            okHttpBuilder.addInterceptor(new HeaderInterceptor(builder.getHeaders()));
        }
        OkHttpClient client=okHttpBuilder.build();


        //retrofit
        retrofit=new Retrofit.Builder()
                .baseUrl(builder.getBaseUrl())
                .client(client)
                .addConverterFactory(builder.getConvertFactory()!=null?builder.getConvertFactory(): GsonConverterFactory.create())
                .build();
    }
    private static EasyHttp getInstance(){
        if(instance==null){
            synchronized (EasyHttp.class){
                if(instance==null){
                    instance=new EasyHttp();
                }
            }
        }
        return instance;
    }
    public static  <T> T create(final Class<T> service){
        return getInstance().retrofit.create(service);
    }
}
