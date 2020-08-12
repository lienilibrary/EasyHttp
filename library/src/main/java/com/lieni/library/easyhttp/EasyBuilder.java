package com.lieni.library.easyhttp;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.internal.http2.Header;
import retrofit2.Converter;

public class EasyBuilder {
    private int readTimeout=60000;
    private int writeTimeout=60000;
    private int connectTimeout=60000;
    private Application application;
    private String baseUrl;
    private List<Interceptor> interceptors=new ArrayList<>();
    private List<Header> headers=new ArrayList<>();
    private Converter.Factory convertFactory;
    private boolean cache=false;
    private boolean cookie=true;
    private boolean loadCookie=false;
    private boolean log=true;

    public EasyBuilder(Application application, String baseUrl) {
        this.application = application;
        this.baseUrl = baseUrl;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public EasyBuilder setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public EasyBuilder setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public EasyBuilder setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }


    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    public EasyBuilder addInterceptor(Interceptor interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    public EasyBuilder addHeader(String key,String value) {
        this.headers.add(new Header(key,value));
        return this;
    }
    public EasyBuilder addHeaders(List<Header> headers) {
        this.headers.addAll(headers);
        return this;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public boolean isCache() {
        return cache;
    }

    public EasyBuilder setCache(boolean cache) {
        this.cache = cache;
        return this;
    }

    public boolean isCookie() {
        return cookie;
    }

    public EasyBuilder setCookie(boolean cookie) {
        this.cookie = cookie;
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isLoadCookie() {
        return loadCookie;
    }

    public EasyBuilder setLoadCookie(boolean loadCookie) {
        this.loadCookie = loadCookie;
        return this;
    }

    public boolean isLog() {
        return log;
    }

    public void setLog(boolean log) {
        this.log = log;
    }

    public Converter.Factory getConvertFactory() {
        return convertFactory;
    }

    public EasyBuilder setConvertFactory(Converter.Factory convertFactory) {
        this.convertFactory = convertFactory;
        return this;
    }

    public Application getApplication() {
        return application;
    }


}
