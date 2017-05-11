package com.example.log2.popmovies.network;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.example.log2.popmovies.BuildConfig;
import com.example.log2.popmovies.R;
import com.example.log2.popmovies.data.ListType;
import com.example.log2.popmovies.helpers.ChokeTracker;
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
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Lorenzo on 25/01/2017.
 */
public class APIHelper {

    private static final String TAG = APIHelper.class.getSimpleName();
    private static final String POSTER_PATH_BASE_URL = "http://image.tmdb.org/t/p/w185/";
    private final Context context;
    private View viewforSnackbar;


    public APIHelper(Context context, View viewforSnackbar) {
        this.context = context;
        this.viewforSnackbar = viewforSnackbar;
    }

    private static String getAbsolutePosterPath(final String relativePosterPath) {
        final Uri posterUri = Uri.parse(POSTER_PATH_BASE_URL).buildUpon()
                .appendEncodedPath(relativePosterPath)
                .build();
        return posterUri.toString();
    }

    private static String obfuscateKey(HttpUrl url) {
        return url.toString().replaceAll("api_key=[0-9a-f]+", "api_key=xxx");
    }

    private <E> Call<E> trackCall(final Call<E> call) {
        return new Call<E>() {
            private Call<E> safeCopy;

            @Override
            public Response<E> execute() throws IOException {
                return call.execute();
            }

            @Override
            public void enqueue(final Callback<E> callback) {
                safeCopy = trackCall(call.clone());
                ServiceHolder.doASAP(new Runnable() {
                                         public void run() {
                                             call.enqueue(new Callback<E>() {
                                                 @Override
                                                 public void onResponse(Call<E> call, Response<E> response) {
                                                     int code = response.code();
                                                     Log.v(TAG, "Got response with code " + code + " from call on " + obfuscateKey(call.request().url()));

                                                     Headers headers = response.headers();
                                                     if (!response.isSuccessful())
                                                         Log.w(TAG, "HTTP Response Code is " + code);
                                                     if (code == 404) {
                                                         onFailure(call, new IllegalArgumentException("Bad address, 404"));
                                                     } else {
                                                         // See: https://www.themoviedb.org/talk/5317af69c3a3685c4a0003b1

                                                         String rateLimit = "X-RateLimit-Remaining";
                                                         if (headers.names().contains(rateLimit)) {
                                                             ServiceHolder.setRemainingValue(Integer.valueOf(headers.get(rateLimit)));
                                                         }
                                                     }
                                                     if (code == 429) {
                                                         int retryAfter = headers.names().contains("Retry-After") ?
                                                                 Math.max(1, Integer.valueOf(headers.get("Retry-After"))) : 1;

                                                         Log.v(TAG, "Call was rate limited, re-scheduling automatically in " + retryAfter + " s as required");
                                                         ServiceHolder.deplete(retryAfter);
                                                         safeCopy.enqueue(callback);
                                                     } else
                                                         callback.onResponse(call, response);
                                                 }

                                                 @Override
                                                 public void onFailure(Call<E> call, Throwable t) {
                                                     callback.onFailure(call, t);
                                                 }
                                             });
                                         }
                                     }
                        , ChokeTracker.showingSnackbar(viewforSnackbar, context.getString(R.string.callChokingTMDB)));
            }

            @Override
            public boolean isExecuted() {
                return call.isExecuted();
            }

            @Override
            public void cancel() {
                call.cancel();
                if (safeCopy != null)
                    safeCopy.cancel();
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
        }
                ;
    }

    private String getApiKey() {
        return BuildConfig.THEMOVIEDB_KEY;
    }

    private TheMovieDbService service() {
        return ServiceHolder.service;
    }

    public Call<MovieListResponse> getPopularMovies(int page) {
        return trackCall(service().getPopularMovies(page, getApiKey()));
    }

    public Call<MovieListResponse> getTopRatedMovies(int page) {
        return trackCall(service().getTopRatedMovies(page, getApiKey()));
    }

    public Call<MovieCount> getPopularMoviesCount() {
        return trackCall(service().getPopularMoviesCount(getApiKey()));
    }

    public Call<MovieCount> getTopRatedMoviesCount() {
        return trackCall(service().getTopRatedMoviesCount(getApiKey()));
    }

    public Call<ReviewListResponse> getReviewsForMovie(final String id) {
        return trackCall(service().getReviewsForMovie(id, getApiKey()));
    }

    public Call<TrailerListResponse> getTrailersForMovie(final String id) {
        return trackCall(service().getTrailersForMovie(id, getApiKey()));
    }

    public Call<Movie> getMovie(final String id) {
        return trackCall(service().getMovie(id, getApiKey()));
    }

    public Call<MovieListResponse> getMovies(ListType listType, int page) {
        return listType == ListType.POPULAR ? getPopularMovies(page) : getTopRatedMovies(page);
    }

    public Call<MovieCount> getMoviesCount(ListType listType) {
        return listType == ListType.POPULAR ? getPopularMoviesCount() : getTopRatedMoviesCount();
    }

    public String getPoster(int expectedWidth, String posterId) {
        return context.getString(R.string.poster_url_prefix) + expectedWidth +
                posterId;
    }

    public String getPosterWide(String posterPath) {
        return getPoster(780, posterPath);
    }

    static class ServiceHolder {
        private static int lowLimit = 1;
        private static Handler handler = new Handler();
        private static int remaining;
        static TheMovieDbService service = createServiceOnce();

        private static TheMovieDbService createServiceOnce() {
            int cacheSize = 50 * 1024 * 1024; // 50 MiB
            Cache cache = new Cache(new File("."), cacheSize);

            OkHttpClient client = new OkHttpClient.Builder()
                    .cache(cache)
                    .build();
            final Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(TheMovieDbService.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .validateEagerly(true)
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

        public static void doASAP(final Runnable runnable, final ChokeTracker chokeTracker) {
            boolean canRun = false;
            int callLimit = 0;
            synchronized (ServiceHolder.class) {
                if (remaining >= lowLimit) {
                    remaining--;
                    canRun = true;
                }
                callLimit = remaining;
            }
            if (canRun) {
                runnable.run();
                chokeTracker.hide();
            } else {
                chokeTracker.signalChoke();
                Log.v(TAG, "Delaying call to cope with rate limitations (call limit " + callLimit + ")");
                withDelay(1, new Runnable() {
                    @Override
                    public void run() {
                        setRemainingValue(1);
                        doASAP(runnable, chokeTracker);
                    }
                });
            }

        }

        public static void deplete(int retryAfter) {
            trackRemaining(0, retryAfter);
        }
    }
}
