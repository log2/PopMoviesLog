package com.example.log2.popmovies.application;

import android.app.Application;

import com.example.log2.popmovies.BuildConfig;
import com.example.log2.popmovies.network.APIHelper;
import com.example.log2.popmovies.network.VolleyHolder;

import timber.log.Timber;

/**
 * Created by gallucci on 13/05/2017.
 */

public class CustomApplication extends Application {

    private VolleyHolder volleyHolder;
    private APIHelper apiHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        Timber.i("Application created");
    }

    public APIHelper getApiHelper() {
        if (apiHelper == null)
            apiHelper = new APIHelper(getApplicationContext());
        return apiHelper;
    }

    public VolleyHolder getVolleyHolder() {
        if (volleyHolder == null) {
            volleyHolder = new VolleyHolder(getApplicationContext());
        }
        return volleyHolder;
    }
}
