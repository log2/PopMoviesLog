package com.example.log2.popmovies.network;

import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.example.log2.popmovies.BuildConfig;
import com.example.log2.popmovies.data.ListType;
import com.example.log2.popmovies.model.Movie;
import com.example.log2.popmovies.model.MovieCount;
import com.example.log2.popmovies.model.MovieListResponse;
import com.example.log2.popmovies.model.ReviewListResponse;
import com.example.log2.popmovies.model.TrailerListResponse;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.content.ContentValues.TAG;

public class TheMovieDbUtils {

    private static final String POSTER_PATH_BASE_URL = "http://image.tmdb.org/t/p/w185/";

    public static String getAbsolutePosterPath(final String relativePosterPath) {
        final Uri posterUri = Uri.parse(POSTER_PATH_BASE_URL).buildUpon()
                .appendEncodedPath(relativePosterPath)
                .build();
        return posterUri.toString();
    }

    public static String getApiKey() {
        return BuildConfig.THEMOVIEDB_KEY;
    }

    public static TheMovieDbService createService() {
        return ServiceHolder.service;

    }

    private static <E> Call<E> trackCall(final Call<E> call) {
        return new Call<E>() {
            @Override
            public Response<E> execute() throws IOException {
                return call.execute();
            }

            @Override
            public void enqueue(final Callback<E> callback) {
                final Call<E> safeCopy = trackCall(call.clone());
                ServiceHolder.doASAP(new Runnable() {
                                         public void run() {

                                             call.enqueue(new Callback<E>() {
                                                 @Override
                                                 public void onResponse(Call<E> call, Response<E> response) {
                                                     int code = response.code();
                                                     if (code != 200)
                                                         Log.w(TAG, "HTTP Response Code is " + code);
                                                     if (code == 404) {
                                                         onFailure(call, new IllegalArgumentException("Bad address, 404"));
                                                     } else {
                                                         // See: https://www.themoviedb.org/talk/5317af69c3a3685c4a0003b1
                                                         Headers headers = response.headers();
                                                         String rateLimit = "X-RateLimit-Remaining";
                                                         if (headers.names().contains(rateLimit)) {
                                                             ServiceHolder.setRemainingValue(Integer.valueOf(headers.get(rateLimit)));
                                                         }
                                                         if (code == 429) {
                                                             if (headers.names().contains("Retry-After")) {
                                                                 int retryAfter = Math.max(1, Integer.valueOf(headers.get("Retry-After")));
                                                                 Log.v(TAG, "Call was rate limited, re-scheduling automatically in " + retryAfter + " s as required");
                                                                 ServiceHolder.deplete(retryAfter);
                                                                 safeCopy.enqueue(callback);
                                                             } else {
                                                                 Log.v(TAG, "Call was rate limited, re-scheduling automatically in a second");
                                                                 ServiceHolder.deplete(1);
                                                                 safeCopy.enqueue(callback);
                                                             }
                                                         } else
                                                             callback.onResponse(call, response);
                                                     }
                                                 }

                                                 @Override
                                                 public void onFailure(Call<E> call, Throwable t) {
                                                     callback.onFailure(call, t);
                                                 }
                                             });
                                         }
                                     }
                );
            }

            @Override
            public boolean isExecuted() {
                return call.isExecuted();
            }

            @Override
            public void cancel() {
                call.cancel();
            }

            @Override
            public boolean isCanceled() {
                return call.isCanceled();
            }

            @Override
            public Call<E> clone() {
                return trackCall(call.clone());
            }

            @Override
            public Request request() {
                return call.request();
            }
        };
    }

    public static Call<MovieListResponse> getPopularMovies(int page) {
        return trackCall(createService().getPopularMovies(page, getApiKey()));
    }

    public static Call<MovieListResponse> getTopRatedMovies(int page) {
        return trackCall(createService().getTopRatedMovies(page, getApiKey()));
    }

    public static Call<MovieCount> getPopularMoviesCount() {
        return trackCall(createService().getPopularMoviesCount(getApiKey()));
    }

    public static Call<MovieCount> getTopRatedMoviesCount() {
        return trackCall(createService().getTopRatedMoviesCount(getApiKey()));
    }

    public static Call<ReviewListResponse> getReviewsForMovie(final String id) {
        return trackCall(createService().getReviewsForMovie(id, getApiKey()));
    }

    public static Call<TrailerListResponse> getTrailersForMovie(final String id) {
        return trackCall(createService().getTrailersForMovie(id, getApiKey()));
    }

    public static Call<Movie> getMovie(final String id) {
        return trackCall(createService().getMovie(id, getApiKey()));
    }

    public static Call<MovieListResponse> getMovies(ListType listType, int page) {
        return listType == ListType.POPULAR ? getPopularMovies(page) : getTopRatedMovies(page);
    }

    public static Call<MovieCount> getMoviesCount(ListType listType) {
        return listType == ListType.POPULAR ? getPopularMoviesCount() : getTopRatedMoviesCount();
    }

    static class ServiceHolder {
        private static int lowLimit = 1;
        private static Handler handler = new Handler();
        private static int remaining;
        static TheMovieDbService service = createServiceOnce();

        private static TheMovieDbService createServiceOnce() {
            int cacheSize = 10 * 1024 * 1024; // 10 MiB
            Cache cache = new Cache(new File("."), cacheSize);

            OkHttpClient client = new OkHttpClient.Builder()
                    .cache(cache)
                    .build();
            final Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(TheMovieDbService.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            final Class<TheMovieDbService> theMovieDbServiceDefinition = TheMovieDbService.class;

            remaining = 1; // At least one
            return retrofit.create(theMovieDbServiceDefinition);
        }

        public static void trackRemaining(final int remaining, int delay) {
            setRemainingValue(remaining);
            withDelay(delay, new Runnable() {
                @Override
                public void run() {
                    // Open a seat, at least
                    setRemainingValue(remaining + 1);
                }
            });
        }

        private static void withDelay(int delay, Runnable r) {
            handler.postDelayed(r, TimeUnit.SECONDS.toMillis(delay));
        }

        private static void setRemainingValue(int remaining) {
            synchronized (ServiceHolder.class) {
                ServiceHolder.remaining = remaining;
                Log.v(TAG, "Set remaining call value to " + remaining);
                ServiceHolder.class.notifyAll();
            }
        }

        public static void doASAP(final Runnable runnable) {
            boolean canRun = false;
            int callLimit = 0;
            synchronized (ServiceHolder.class) {
                if (remaining >= lowLimit) {
                    remaining--;
                    canRun = true;
                }
                callLimit = remaining;
            }
            if (canRun)
                runnable.run();
            else {
                Log.v(TAG, "Delaying call to cope with rate limitations (call limit " + callLimit + ")");
                withDelay(2, new Runnable() {
                    @Override
                    public void run() {
                        doASAP(runnable);
                    }
                });
            }

        }

        public static void deplete(int retryAfter) {
            trackRemaining(0, retryAfter);
        }
    }
}