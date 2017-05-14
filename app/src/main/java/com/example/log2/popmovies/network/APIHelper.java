package com.example.log2.popmovies.network;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.example.log2.popmovies.BuildConfig;
import com.example.log2.popmovies.R;
import com.example.log2.popmovies.data.ListType;
import com.example.log2.popmovies.helpers.ChokeTracker;
import com.example.log2.popmovies.helpers.SignallingUtils;
import com.example.log2.popmovies.model.Movie;
import com.example.log2.popmovies.model.MovieCount;
import com.example.log2.popmovies.model.MovieListResponse;
import com.example.log2.popmovies.model.ReviewListResponse;
import com.example.log2.popmovies.model.TrailerListResponse;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
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
    public static final String RETRY_AFTER = "Retry-After"; //NON-NLS
    public static final String X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining"; //NON-NLS
    private static final String TAG = APIHelper.class.getSimpleName();
    private final Context context;
    ServiceHolder serviceHolder = new ServiceHolder();
    private WeakReference<View> viewRef;


    public APIHelper(Context context) {
        this.context = context;
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
                serviceHolder.performWhenNotChoked(new Runnable() {
                                                       public void run() {
                                                           call.enqueue(new Callback<E>() {
                                                               @Override
                                                               public void onResponse(Call<E> call, Response<E> response) {
                                                                   int code = response.code();
                                                                   Log.v(TAG, MessageFormat.format("Got response with code {0} from call on {1}", code, obfuscateKey(call.request().url())));

                                                                   Headers headers = response.headers();
                                                                   if (!response.isSuccessful())
                                                                       Log.w(TAG, MessageFormat.format("HTTP Response Code is {0}", code));
                                                                   if (code == 404) {
                                                                       onFailure(call, new IllegalArgumentException("Bad address, 404"));
                                                                   } else {
                                                                       // See: https://www.themoviedb.org/talk/5317af69c3a3685c4a0003b1

                                                                       if (headers.names().contains(X_RATE_LIMIT_REMAINING)) {
                                                                           serviceHolder.setRemainingValue(Integer.valueOf(headers.get(X_RATE_LIMIT_REMAINING)));
                                                                       }
                                                                   }
                                                                   if (code == 429) {
                                                                       int retryAfter = headers.names().contains(RETRY_AFTER) ?
                                                                               Math.max(1, Integer.valueOf(headers.get(RETRY_AFTER))) : 1;

                                                                       Log.v(TAG, MessageFormat.format("Call was rate limited, re-scheduling automatically in {0} s as required", retryAfter));
                                                                       serviceHolder.deplete(retryAfter);
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
                        ,
                        getChokeTracker());
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

            @SuppressWarnings("CloneDoesntCallSuperClone")
            @Override
            public Call<E> clone() {
                // Intentionally wrapped clone
                return trackCall(call.clone());
            }

            @Override
            public Request request() {
                return call.request();
            }
        }
                ;
    }

    private ChokeTracker getChokeTracker() {
        return ChokeTracker.showingSnackbar(context, viewRef.get(), context.getString(R.string.callChokingTMDB));
    }

    @SuppressWarnings("SameReturnValue")
    private String getApiKey() {
        // We only use one TMDB key
        return BuildConfig.THEMOVIEDB_KEY;
    }

    private TheMovieDbService service() {
        return serviceHolder.service;
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

    public void setView(View view) {
        this.viewRef = new WeakReference<>(view);
    }

    public <E> Callback<E> wrapCallback(final SuccessOnlyCallback<E> callback, final Runnable rescheduleAction) {
        return new Callback<E>() {
            @Override
            public void onResponse(Call<E> call, Response<E> response) {
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<E> call, Throwable t) {
                SignallingUtils.alert(context, viewRef.get(), R.string.networkIssuesDetected);
                rescheduleAction.run();
            }
        };
    }

    public void pauseAll() {
        serviceHolder.pauseAll();
    }

    public void resumeAll() {
        serviceHolder.resumeAll(getChokeTracker());
    }

    public interface SuccessOnlyCallback<E> {
        void onResponse(Call<E> call, retrofit2.Response<E> response);

    }

    static class ServiceHolder {
        private static final int lowLimit = 1;
        private final Handler handler = new Handler();
        private int remaining;
        final TheMovieDbService service = createServiceOnce();

        private List<Runnable> paused = new ArrayList<>();

        private volatile boolean retryAllowed = true;

        private TheMovieDbService createServiceOnce() {
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

        public void trackRemaining(final int remaining, int delay) {
            setRemainingValue(remaining);
            withDelay(delay, new Runnable() {
                @Override
                public void run() {
                    // Open a seat, at least
                    setRemainingValue(remaining + 1);
                }
            });
        }

        private void withDelay(int delay, Runnable r) {
            handler.postDelayed(r, TimeUnit.SECONDS.toMillis(delay));
        }

        private void setRemainingValue(int remaining) {
            synchronized (this) {
                this.remaining = remaining;
                Log.v(TAG, MessageFormat.format("Set remaining call value to {0}", remaining));
            }
        }

        public void performWhenNotChoked(final Runnable runnable, final ChokeTracker chokeTracker) {
            boolean canRun = false;
            int callLimit = 0;
            synchronized (this) {
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
                if (isRetryAllowed()) {
                    chokeTracker.signalChoke();
                    Log.v(TAG, MessageFormat.format("Delaying call to cope with rate limitations (call limit {0})", callLimit));
                    withDelay(1, new Runnable() {
                        @Override
                        public void run() {
                            if (isRetryAllowed()) {
                                setRemainingValue(1);
                                performWhenNotChoked(runnable, chokeTracker);

                            } else {
                                Log.w(TAG, "Retry not allowed anymore since we planned a retry, no more trials will be performed (application is paused)");
                                addPaused(runnable);
                            }
                        }
                    });
                } else {
                    Log.w(TAG, "Retry not allowed, application is paused");
                    addPaused(runnable);
                }
            }

        }

        private void addPaused(Runnable runnable) {
            synchronized (this) {
                paused.add(runnable);
            }
        }

        private boolean isRetryAllowed() {
            return retryAllowed;
        }

        public void deplete(int retryAfter) {
            trackRemaining(0, retryAfter);
        }

        public void pauseAll() {
            retryAllowed = false;
        }

        public void resumeAll(ChokeTracker chokeTracker) {
            retryAllowed = true;
            List<Runnable> allPaused = drainAllPaused();
            for (Runnable runnable : allPaused) {
                performWhenNotChoked(runnable, chokeTracker);
            }
        }

        private List<Runnable> drainAllPaused() {
            synchronized (this) {
                List<Runnable> pausedCopy = new ArrayList<>(paused);
                paused.clear();
                return pausedCopy;
            }
        }
    }
}
