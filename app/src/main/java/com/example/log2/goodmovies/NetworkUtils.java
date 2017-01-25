package com.example.log2.goodmovies;

import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Lorenzo on 25/01/2017.
 */

public class NetworkUtils {
    private static final String THEMOVIEDB_URL = "https://api.themoviedb.org/3";
    private static final String API_KEY = "api_key";
    private static final String TAG = NetworkUtils.class.getSimpleName();

    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public static URL buildUrl(String verb, String[]... params) {
        String baseURL = THEMOVIEDB_URL + verb;
        Log.v(TAG, "Building URL for " + baseURL);
        Uri.Builder builder = Uri.parse(baseURL).buildUpon()
                .appendQueryParameter(API_KEY, BuildConfig.THEMOVIEDB_KEY);
        for (String[] pair : params) {
            builder.appendQueryParameter(pair[0], pair[1]);
        }
        Uri builtUri = builder.build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    public static JSONObject queryTheMovieDb(String verb, String[]... params) {
        URL url = buildUrl(verb, params);
        try {
            String responseFromHttpUrl = getResponseFromHttpUrl(url);
            Log.v(TAG, "Got data: " + responseFromHttpUrl);
            JSONObject jsonObject = new JSONObject(responseFromHttpUrl);
            return jsonObject;
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }

    }
}
