package com.lieni.library.easyhttp.logger;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import okhttp3.logging.HttpLoggingInterceptor;

public class HttpLogger implements HttpLoggingInterceptor.Logger {

    @Override
    public void log(@NotNull String message) {
        Log.i("EasyHttpInfo",message);
    }
}