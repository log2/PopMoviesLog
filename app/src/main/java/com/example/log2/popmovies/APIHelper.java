package com.example.log2.popmovies;

import android.content.Context;
import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;

/**
 * Created by Lorenzo on 25/01/2017.
 */

class APIHelper {

    private static final String TAG = APIHelper.class.getSimpleName();
    private final Context context;

    public APIHelper(Context context) {
        this.context = context;
    }

    public JsonObjectRequest req(String url, Response.Listener<JSONObject> listener) {
        return new JsonObjectRequest(Request.Method.GET, url, null, listener, null);
    }

    public JsonObjectRequest reqHigh(String url, Response.Listener<JSONObject> listener) {
        return new JsonObjectRequest(Request.Method.GET, url, null, listener, null) {
            @Override
            public Priority getPriority() {
                return Priority.HIGH;
            }
        };
    }

    private String toUrlFragment(ListType listType) {
        switch (listType) {
            case TOP_RATED:
                return context.getString(R.string.url_part_top_rated);

            default:
            case POPULAR:
                return context.getString(R.string.url_part_most_popular);

        }
    }

    private JsonObjectRequest reqLow(String url, Response.Listener<JSONObject> listener) {
        return new JsonObjectRequest(Request.Method.GET, url, null, listener, null) {
            @Override
            public Priority getPriority() {
                return Priority.LOW;
            }
        };
    }

    private String theMovieDB(String verb, String[]... params) {
        String baseURL = context.getString(R.string.themoviedb_api_url_prefix) + verb;
        Uri.Builder builder = Uri.parse(baseURL).buildUpon();
        for (String[] pair : params) {
            builder.appendQueryParameter(pair[0], pair[1]);
        }
        //Log.v(TAG, "URL built: " + builder.toString());
        builder
                .appendQueryParameter(context.getString(R.string.tmdb_param_api_key), BuildConfig.THEMOVIEDB_KEY);
        Uri builtUri = builder.build();
        return builtUri.toString();
    }

    public String getPage(ListType listType, String[]... params) {
        return theMovieDB(context.getString(R.string.themoviedb_base_api) + toUrlFragment(listType), params);
    }

    public JsonObjectRequest newReq(boolean highPriority, ListType listType, final int position, final Response.Listener<JSONObject> listener) {
        final int page = 1 + position / 20;
        final int subPosition = position % 20;
        //Log.v(TAG, "Setting position of " + this + " to " + position + "(" + page + ":" + subPosition);
        //mv_position.setText(page + ":" + subPosition);
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject pageContent) {
                try {
                    JSONObject item = pageContent.getJSONArray(context.getString(R.string.json_attr_results)).getJSONObject(subPosition);
                    listener.onResponse(item);
                } catch (JSONException e) {
                    throw new RuntimeException(MessageFormat.format(context.getString(R.string.malformed_json_object), position), e);
                }
            }
        };
        String pageUrl = getPage(listType, page);
        return highPriority ? reqHigh(pageUrl, responseListener) : reqLow(pageUrl, responseListener);
    }

    public JsonObjectRequest newReqById(boolean highPriority, int movieId, final Response.Listener<JSONObject> listener) {
        String url = theMovieDB(context.getString(R.string.themoviedb_base_api) + "/movie/" + movieId);
        return highPriority ? reqHigh(url, listener) : reqLow(url, listener);
    }

    private String getPage(ListType listType, int page) {
        return getPage(listType, new String[]{context.getString(R.string.tmdb_page_param), Integer.toString(page)});
    }

    public String getPoster(int expectedWidth, String posterId) {
        return context.getString(R.string.poster_url_prefix) + expectedWidth +
                posterId;
    }
}
