package com.example.log2.popmovies.detail;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.example.log2.popmovies.R;
import com.example.log2.popmovies.application.CustomApplication;
import com.example.log2.popmovies.data.FavoriteContract;
import com.example.log2.popmovies.databinding.ActivityScrollingBinding;
import com.example.log2.popmovies.helpers.SignallingUtils;
import com.example.log2.popmovies.model.Movie;
import com.example.log2.popmovies.model.Review;
import com.example.log2.popmovies.model.ReviewListResponse;
import com.example.log2.popmovies.model.Trailer;
import com.example.log2.popmovies.model.TrailerListResponse;
import com.example.log2.popmovies.network.APIHelper;

import java.text.MessageFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Response;

public class ScrollingActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Boolean> {
    public static final String MOVIE_LIST_TYPE = Intent.EXTRA_TEXT;
    private static final String MOVIE_ID = "movieId"; //NON-NLS
    private static final String TAG = ScrollingActivity.class.getSimpleName();
    public static int IS_FAVORITE_LOADER = 43;

    @BindView(R.id.rv_trailers)
    RecyclerView rvTrailers;
    @BindView(R.id.rv_reviews)
    RecyclerView rvReviews;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.app_bar)
    AppBarLayout appBar;
    @BindView(R.id.iv_movie_poster)
    ImageView posterBackground;
    Call<ReviewListResponse> reviewsCall;
    Call<TrailerListResponse> trailersCall;
    private String movieId;
    private String movieTitle;
    private TrailersAdapter trailersAdapter;
    private ReviewsAdapter reviewsAdapter;
    private ActivityScrollingBinding binding;
    private boolean isFavorite;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setImageFrameSize();
    }

    private void setImageFrameSize() {
        if (appBar != null) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int availableHeightPixel = (size.y);
            int vertical_margin = (int) getResources().getDimension(R.dimen.vertical_margin);
            int synopsis_text_size = (int) getResources().getDimension(R.dimen.synopsisTextSize);
            boolean portrait = size.y > size.x;
            int synopsisLines = portrait ? 4 : 2;
            // At least some space for two vertical margins AND 2 or 4 lines of synopsis (2 in landscape, 4 in portrait)
            int newHeight = availableHeightPixel - vertical_margin * 2 - synopsis_text_size * synopsisLines;
            setAppBarHeight(newHeight);
        }
    }

    private void setAppBarHeight(int newHeight) {
        appBar.getLayoutParams().height = newHeight;
//        AppBarLayout.LayoutParams layoutParams = new AppBarLayout.LayoutParams(appBar.getLayoutParams());
//
//        layoutParams.height = newHeight;
//        appBar.setLayoutParams(layoutParams);
    }

    private int dpToPixel(int dp) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(displayMetrics);
        float scale = displayMetrics.density;
        return (int) (dp * scale + 0.5f);
    }

    private int pixelToDp(int pixel) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(displayMetrics);
        float scale = displayMetrics.density;
        return (int) (pixel / scale + 0.5f);
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<Boolean>(this) {
            @Override
            protected void onStartLoading() {
                if (args == null) {
                    return;
                }
                forceLoad();
            }

            @Override
            public Boolean loadInBackground() {
                Uri uri = FavoriteContract.FavoriteEntry.CONTENT_URI;
                String movieId = args.getString(MOVIE_ID);
                Cursor cursor = getContentResolver().query(uri, null, FavoriteContract.FavoriteEntry.COLUMN_TMDB_ID + " = ?", new String[]{"" + movieId}, null);
                try {
                    return cursor != null && cursor.getCount() > 0;
                } finally {
                    if (cursor != null)
                        cursor.close();
                }
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean favoriteCheckBoxValue) {
        saveFavValue(favoriteCheckBoxValue);
    }

    private void saveFavValue(Boolean favoriteCheckBoxValue) {
        isFavorite = favoriteCheckBoxValue;
        fab.setImageResource(favoriteCheckBoxValue ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        fab.setVisibility(View.VISIBLE);
    }

    private void setFavorite(Boolean favoriteCheckBoxValue) {
        if (favoriteCheckBoxValue != null) {
            saveFavValue(favoriteCheckBoxValue);
            String message = (favoriteCheckBoxValue) ? getString(R.string.addToFavorite) : getString(
                    R.string.removeFromFavorite);
            Snackbar.make(fab, message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undoActionName, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            favorite();
                        }
                    }).show();
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    private void prepareFav() {
        Bundle checkFavBundle = new Bundle();
        checkFavBundle.putString(MOVIE_ID, movieId);

        fab.setVisibility(View.INVISIBLE);

        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<Cursor> loader = loaderManager.getLoader(IS_FAVORITE_LOADER);

        if (loader == null) {
            loaderManager.initLoader(IS_FAVORITE_LOADER, checkFavBundle, this);
        } else {
            loaderManager.restartLoader(IS_FAVORITE_LOADER, checkFavBundle, this);
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.share)
            shareMovie();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        trackOurMainViewInApiHolder();
    }

    private void trackOurMainViewInApiHolder() {
        getCustomApplication().getApiHelper().setView(fab);
    }

    private void untrackOurMainViewInApiHolder() {
        getCustomApplication().getApiHelper().setView(null);
    }

    private void shareMovie() {
        final String mimeType = "text/plain"; //NON-NLS
        ShareCompat.IntentBuilder
                .from(this)
                .setType(mimeType)
                .setText(createShareMovieIntentText())
                .startChooser();
    }

    private String createShareMovieIntentText() {
        if (trailersAdapter.getItemCount() > 0) {
            final Trailer[] trailers = trailersAdapter.getTrailers();
            final Trailer firstTrailer = trailers[0];
            final String firstTrailerLink = firstTrailer.getLink();

            return getString(R.string.share_movie_text_with_trailer, movieTitle, firstTrailerLink);
        } else {
            return getString(R.string.share_movie_text, movieTitle);
        }
    }

    public void favorite() {
        final boolean checked = isFavorite;
        new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {
                if (checked) {
                    getContentResolver().delete(FavoriteContract.FavoriteEntry.CONTENT_URI, FavoriteContract.FavoriteEntry.COLUMN_TMDB_ID + " = ?", new String[]{"" + params[0]});
                } else {
                    ContentValues values = new ContentValues();
                    values.put(FavoriteContract.FavoriteEntry.COLUMN_TMDB_ID, params[0]);
                    values.put(FavoriteContract.FavoriteEntry.COLUMN_TITLE, movieTitle);
                    getContentResolver().insert(FavoriteContract.FavoriteEntry.CONTENT_URI, values);
                }
                return !checked;
            }

            @Override
            protected void onPreExecute() {
                // FIXME add progress
                fab.setEnabled(false);
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                fab.setEnabled(true);
                setFavorite(aBoolean);
            }
        }.execute(movieId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (trailersCall != null) {
            trailersCall.cancel();
            trailersCall = null;
        }
        if (reviewsCall != null) {
            reviewsCall.cancel();
            reviewsCall = null;
        }
        untrackOurMainViewInApiHolder();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding =
                DataBindingUtil.setContentView(this, R.layout.activity_scrolling);
        ButterKnife.bind(this);

        getSupportLoaderManager().initLoader(IS_FAVORITE_LOADER, null, this);
        if (!isOnline())
            SignallingUtils.alert(this, fab, R.string.no_internet_no_details);

        setSupportActionBar(toolbar);

        trackOurMainViewInApiHolder();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                favorite();
            }
        });

        Intent intent = getIntent();

        if (intent != null && intent.hasExtra(MOVIE_LIST_TYPE)
                && intent.hasExtra(getString(R.string.extraMovieContentKey))
                ) {
            final Movie movie = intent.getParcelableExtra(getString(R.string.extraMovieContentKey));
            if (movie != null)
                bindMovie(movie);
        }
        setImageFrameSize();
    }

    private void bindMovie(Movie movie) {
        binding.setMovie(movie);

        movieId = movie.id;
        movieTitle = movie.title;

        prepareFav();

        fillTrailers();
        fillReviews();
        Log.v(TAG, MessageFormat.format("Getting poster {0}", movie.posterPath));

        Glide.with(this).load(getApiHelper().getPosterWide(movie.posterPath))
                .priority(Priority.IMMEDIATE)
                .animate(android.R.anim.fade_in)
                .into(posterBackground);
    }

    private void fillReviews() {
        APIHelper apiHelper = getApiHelper();
        reviewsCall = apiHelper.getReviewsForMovie(movieId);
        reviewsCall.enqueue(apiHelper.wrapCallback(new APIHelper.SuccessOnlyCallback<ReviewListResponse>() {
            @Override
            public void onResponse(Call<ReviewListResponse> call, Response<ReviewListResponse> response) {
                reviewsAdapter = new ReviewsAdapter();
                List<Review> reviews = response.body().reviews;
                if (reviews == null || reviews.isEmpty())
                    rvReviews.setVisibility(View.GONE);
                else {
                    reviewsAdapter.setReviews(reviews.toArray(new Review[]{}));
                    rvReviews.setHasFixedSize(true);
                    rvReviews.setClipToPadding(true);
                    rvReviews.setClipChildren(true);
                    rvReviews.setItemViewCacheSize(10);

                    final Context context = ScrollingActivity.this;

                    GridLayoutManager layoutManager = new GridLayoutManager(context, 1, GridLayoutManager.VERTICAL, false);
                    layoutManager.setSmoothScrollbarEnabled(true);
                    rvReviews.setLayoutManager(layoutManager);
                    rvReviews.setAdapter(reviewsAdapter);
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                fillReviews();
            }
        }));
    }

    private void fillTrailers() {
        APIHelper apiHelper = getApiHelper();
        trailersCall = apiHelper.getTrailersForMovie(movieId);
        trailersCall.enqueue(apiHelper.wrapCallback(new APIHelper.SuccessOnlyCallback<TrailerListResponse>() {
            @Override
            public void onResponse(Call<TrailerListResponse> call, Response<TrailerListResponse> response) {
                trailersAdapter = new TrailersAdapter(new TrailersAdapter.OnClickListener() {
                    @Override
                    public void onTrailerItemClick(Trailer trailer) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(trailer.getLink()));
                        ComponentName componentName = intent.resolveActivity(getPackageManager());
                        if (componentName == null) {
                            SignallingUtils.alert(ScrollingActivity.this, fab, R.string.error_no_video_player);
                        } else {
                            startActivity(intent);
                        }
                    }
                });
                List<Trailer> trailers = response.body().trailers;
                if (trailers == null || trailers.isEmpty()) {
                    rvTrailers.setVisibility(View.GONE);
                    Log.v(TAG, MessageFormat.format("Raw body: {0}", response.raw().body()));
                } else {
                    Log.v(TAG, MessageFormat.format("Got {0} trailers to display", trailers.size()));
                    trailersAdapter.setTrailers(trailers.toArray(new Trailer[]{}));
                    rvTrailers.setHasFixedSize(true);
                    rvTrailers.setClipToPadding(true);
                    rvTrailers.setClipChildren(true);
                    rvTrailers.setItemViewCacheSize(10);

                    final Context context = ScrollingActivity.this;

                    GridLayoutManager layoutManager = new GridLayoutManager(context, 1, GridLayoutManager.HORIZONTAL, false);
                    layoutManager.setSmoothScrollbarEnabled(true);
                    rvTrailers.setLayoutManager(layoutManager);
                    rvTrailers.setAdapter(trailersAdapter);
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                fillTrailers();
            }
        }));
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public CustomApplication getCustomApplication() {
        return ((CustomApplication) getApplication());
    }

    public APIHelper getApiHelper() {
        return getCustomApplication().getApiHelper();
    }
}
