package com.example.log2.popmovies.network;

import android.content.Context;

import com.android.volley.VolleyLog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.integration.volley.VolleyUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.GlideModule;
import com.example.log2.popmovies.application.CustomApplication;

import java.io.InputStream;

public class SharedVolleyQueueGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // Do nothing.
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        CustomApplication customApplication = (CustomApplication) context.getApplicationContext();
        glide.register(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(customApplication.getVolleyHolder().getRequestQueue()));
        VolleyLog.DEBUG = true;

    }
}
