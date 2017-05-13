package com.example.log2.popmovies.application;

import android.app.Application;

import com.example.log2.popmovies.network.APIHelper;
import com.example.log2.popmovies.network.VolleyHolder;

/**
 * Created by gallucci on 13/05/2017.
 */

public class CustomApplication extends Application {

    private VolleyHolder volleyHolder;
    private APIHelper apiHelper;

    public CustomApplication() {
    }

    public APIHelper getApiHelper() {
        if (apiHelper == null)
            apiHelper = new APIHelper(getApplicationContext(), null);
        return apiHelper;
    }

    public VolleyHolder getVolleyHolder() {
        if (volleyHolder == null)
            volleyHolder = VolleyHolder.in(getApplicationContext());
        return volleyHolder;
    }
}
