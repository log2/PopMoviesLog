package com.example.log2.popmovies;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

public class VolleyHolder {
    private static VolleyHolder volleyHolder;
    private static Context context;
    private final ImageLoader imageLoader;
    private RequestQueue requestQueue;

    private VolleyHolder(Context context) {
        VolleyHolder.context = context;
        requestQueue = getRequestQueue();

        // NOTE: Not used anymore
        imageLoader = new ImageLoader(requestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });


// Start the queue
        requestQueue.start();
    }

    public static synchronized VolleyHolder in(Context context) {
        if (volleyHolder == null) {
            volleyHolder = new VolleyHolder(context);
        }
        return volleyHolder;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void add(Request<T> request) {
        request.setTag(context);
        getRequestQueue().add(request);
    }

    public void cancelAll() {
        requestQueue.cancelAll(context);
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }
}
