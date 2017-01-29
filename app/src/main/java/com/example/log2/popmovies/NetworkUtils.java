package com.example.log2.popmovies;

import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

/**
 * Created by Lorenzo on 25/01/2017.
 */

public class NetworkUtils {
    private static final String THEMOVIEDB_URL = "https://api.themoviedb.org/3";
    private static final String API_KEY = "api_key";
    private static final String TAG = NetworkUtils.class.getSimpleName();


    public static JsonObjectRequest req(String url, Response.Listener<JSONObject> listener) {
        return new JsonObjectRequest(Request.Method.GET, url, null, listener, null);
    }

    public static JsonObjectRequest reqHigh(String url, Response.Listener<JSONObject> listener) {
        return new JsonObjectRequest(Request.Method.GET, url, null, listener, null) {
            @Override
            public Priority getPriority() {
                return Priority.HIGH;
            }
        };
    }

    public static String theMovieDB(String verb, String[]... params) {
        String baseURL = THEMOVIEDB_URL + verb;
        Uri.Builder builder = Uri.parse(baseURL).buildUpon();
        for (String[] pair : params) {
            builder.appendQueryParameter(pair[0], pair[1]);
        }
        Log.v(TAG, "URL built: " + builder.toString());
        builder
                .appendQueryParameter(API_KEY, BuildConfig.THEMOVIEDB_KEY);
        Uri builtUri = builder.build();
        return builtUri.toString();
    }

}
