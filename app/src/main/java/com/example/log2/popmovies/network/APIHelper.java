package com.example.log2.popmovies.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

import static okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS;
import static okhttp3.logging.HttpLoggingInterceptor.Level.NONE;

/**
 * Created by Lorenzo on 25/01/2017.
 */
public class APIHelper {
    public static final String RETRY_AFTER = "Retry-After"; //NON-NLS
    public static final String X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining"; //NON-NLS
    private static final String TAG = APIHelper.class.getSimpleName();
    private final Context context;
    private ServiceHolder serviceHolder;
    private WeakReference<View> viewRef;

    public APIHelper(Context context) {
        this.context = context;
        serviceHolder = new ServiceHolder(context);
    }

    private static String obfuscateKey(HttpUrl url) {
        return obfuscateKey(url.toString());
    }

    private static String obfuscateKey(String text) {
        return text.replaceAll("api_key=[0-9a-f]+", "api_key=xxx");
    }

    public static boolean detectNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private <E> Call<E> trackCall(final Call<E> call) {
        return new AutoRetryingCall<>(call);
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

    public Call<MovieListResponse> getMovies(String type, int page) {
        return trackCall(service().getMovies(type, page, getApiKey()));
    }

    public Call<MovieCount> getMoviesCount(String type) {
        return trackCall(service().getMoviesCount(type, getApiKey()));
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
        if (listType.isProvidedByTMDB()) return getMovies(listType.getExternalName(), page);
        else
            throw new IllegalArgumentException("List type " + listType + " is not TMDB-provided, can't use here");
    }

    public Call<MovieCount> getMoviesCount(ListType listType) {
        if (listType.isProvidedByTMDB()) return getMoviesCount(listType.getExternalName());
        else
            throw new IllegalArgumentException("List type " + listType + " is not TMDB-provided, can't use here");
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
                if (detectNetwork(context)) {
                    SignallingUtils.alert(context, viewRef.get(), R.string.networkIssuesDetected);
                    rescheduleAction.run();
                } else {
                    Timber.w("Could not get URL %s content, network appear non-working right now", obfuscateKey(call.request().url()));
                }
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

    /**
     * Disclaimer: offline cache handling inspired by https://github.com/adavis/adept-android/tree/retrofit2-cache
     */
    static class ServiceHolder {
        private static final int lowLimit = 1;
        private static final String CACHE_CONTROL = "Cache-Control";
        final TheMovieDbService service;
        private final Handler handler = new Handler();
        private final Context context;
        // At least one slot for calls
        private int remaining = 1;
        private List<Runnable> paused = new ArrayList<>();
        private volatile boolean retryAllowed = true;

        ServiceHolder(Context context) {
            this.context = context;
            service = createServiceOnce();
        }

        private static HttpLoggingInterceptor provideHttpLoggingInterceptor() {
            HttpLoggingInterceptor httpLoggingInterceptor =
                    new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                        @Override
                        public void log(String message) {
                            Timber.d(obfuscateKey(message));
                        }
                    });
            httpLoggingInterceptor.setLevel(BuildConfig.DEBUG ? HEADERS : NONE);
            return httpLoggingInterceptor;
        }

        public static Interceptor provideCacheInterceptor() {
            return new Interceptor() {
                @Override
                public okhttp3.Response intercept(Chain chain) throws IOException {
                    okhttp3.Response response = chain.proceed(chain.request());

                    // re-write response header to force use of cache
                    CacheControl cacheControl = new CacheControl.Builder()
                            .maxAge(5, TimeUnit.MINUTES)
                            .build();

                    return response.newBuilder()
                            .header(CACHE_CONTROL, cacheControl.toString())
                            .build();
                }
            };
        }

        public Retrofit provideRetrofit(String baseUrl) {
            return new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(provideOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .validateEagerly(true)
                    .build();
        }

        private OkHttpClient provideOkHttpClient() {
            return new OkHttpClient.Builder()
                    .addInterceptor(provideHttpLoggingInterceptor())
                    .addInterceptor(provideOfflineCacheInterceptor())
                    .addNetworkInterceptor(provideCacheInterceptor())
                    .cache(provideCache())
                    .build();
        }

        private Cache provideCache() {
            try {
                return new Cache(new File(context.getCacheDir(), "http-cache"),
                        50 * 1024 * 1024); // 50 MiB
            } catch (Exception e) {
                Timber.e(e, "Could not create cache!");
                return null;
            }
        }

        public boolean hasNetwork() {
            return detectNetwork(context);
        }

        public Interceptor provideOfflineCacheInterceptor() {
            return new Interceptor() {
                @Override
                public okhttp3.Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();

                    if (!hasNetwork()) {
                        CacheControl cacheControl = new CacheControl.Builder()
                                .maxStale(7, TimeUnit.DAYS)
                                .build();

                        request = request.newBuilder()
                                .cacheControl(cacheControl)
                                .build();
                    }

                    return chain.proceed(request);
                }
            };
        }


        private TheMovieDbService createServiceOnce() {
            return provideRetrofit(TheMovieDbService.BASE_URL).create(TheMovieDbService.class);
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
                Timber.v("Set remaining call value to %d", remaining);
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
            if (canRun || !hasNetwork()) {
                // Always execute code when no network is detected
                // (offline cache will be used instead)
                runnable.run();
                chokeTracker.hide();
            } else {
                if (isRetryAllowed()) {
                    chokeTracker.signalChoke();
                    Timber.v("Delaying call to cope with rate limitations (call limit %d)", callLimit);
                    withDelay(1, new Runnable() {
                        @Override
                        public void run() {
                            if (isRetryAllowed()) {
                                setRemainingValue(1);
                                performWhenNotChoked(runnable, chokeTracker);

                            } else {
                                Timber.w("Retry not allowed anymore since we planned a retry, no more trials will be performed (application is paused)");
                                addPaused(runnable);
                            }
                        }
                    });
                } else {
                    Timber.w("Retry not allowed, application is paused");
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

    private class AutoRetryingCall<E> implements Call<E> {
        private final Call<E> call;
        private Call<E> safeCopy;

        public AutoRetryingCall(Call<E> call) {
            this.call = call;
        }

        @Override
        public Response<E> execute() throws IOException {
            return call.execute();
        }

        @Override
        public void enqueue(final Callback<E> callback) {
            safeCopy = trackCall(call.clone());
            serviceHolder.performWhenNotChoked(new Runnable() {
                public void run() {
                    call.enqueue(new AutoRetryingCallback(callback));
                }
            }, getChokeTracker());
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

        private class AutoRetryingCallback implements Callback<E> {
            private final Callback<E> callback;

            public AutoRetryingCallback(Callback<E> callback) {
                this.callback = callback;
            }

            @Override
            public void onResponse(Call<E> call, Response<E> response) {
                int code = response.code();
                Timber.v("Got response with code %d from call on %s", code, obfuscateKey(call.request().url()));

                Headers headers = response.headers();
                if (!response.isSuccessful())
                    Timber.w("HTTP Response Code is %d", code);
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

                    Timber.v("Call was rate limited, re-scheduling automatically in %d s as required", retryAfter);
                    serviceHolder.deplete(retryAfter);
                    safeCopy.enqueue(callback);
                } else
                    callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<E> call, Throwable t) {
                callback.onFailure(call, t);
            }
        }
    }
}
