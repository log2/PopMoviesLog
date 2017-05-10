package com.example.log2.popmovies.detail;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.example.log2.popmovies.R;
import com.example.log2.popmovies.data.FavoriteContract;
import com.example.log2.popmovies.data.ListType;
import com.example.log2.popmovies.databinding.ActivityScrollingBinding;
import com.example.log2.popmovies.model.Movie;
import com.example.log2.popmovies.model.Review;
import com.example.log2.popmovies.model.ReviewListResponse;
import com.example.log2.popmovies.model.Trailer;
import com.example.log2.popmovies.model.TrailerListResponse;
import com.example.log2.popmovies.network.APIHelper;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScrollingActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Boolean> {
    public static final String MOVIE_LIST_TYPE = Intent.EXTRA_TEXT;
    private static final String MOVIE_ID = "movieId";
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
    private String movieId;
    private String movieTitle;
    private TrailersAdapter trailersAdapter;
    private ReviewsAdapter reviewsAdapter;
    private ActivityScrollingBinding binding;

    private boolean isFavorite;

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
                    boolean isNonEmpty = cursor != null && cursor.getCount() > 0;
                    return isNonEmpty;
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
        fab.setImageResource(favoriteCheckBoxValue ? android.R.drawable.btn_minus : android.R.drawable.ic_menu_save);
        fab.setVisibility(View.VISIBLE);
    }

    private void setFavorite(Boolean favoriteCheckBoxValue) {
        if (favoriteCheckBoxValue != null) {
            saveFavValue(favoriteCheckBoxValue);
            String message = (favoriteCheckBoxValue) ? getString(R.string.addToFavorite) : getString(
                    R.string.removeFromFavorite);
            Snackbar.make(fab, message, Snackbar.LENGTH_LONG)
                    .setAction("UNDO", new View.OnClickListener() {
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

    private void shareMovie() {
        final String mimeType = "text/plain";
        ShareCompat.IntentBuilder
                .from(this)
                .setType(mimeType)
                .setText(createShareMovieIntentText())
                .startChooser();
    }

    private String createShareMovieIntentText() {
        final boolean oneOrMoreTrailers = (trailersAdapter.getItemCount() > 0);
        if (oneOrMoreTrailers) {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding =
                DataBindingUtil.setContentView(this, R.layout.activity_scrolling);
        ButterKnife.bind(this);

        getSupportLoaderManager().initLoader(IS_FAVORITE_LOADER, null, this);
        if (!isOnline())
            Toast.makeText(this, R.string.no_internet_no_details, Toast.LENGTH_LONG).show();

        setSupportActionBar(toolbar);

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
            ListType listType = ListType.valueOf(intent.getStringExtra(MOVIE_LIST_TYPE));
            final Movie movie = intent.getParcelableExtra(getString(R.string.extraMovieContentKey));
            if (movie != null)
                bindMovie(movie);
        }
    }

    private void bindMovie(Movie movie) {
        binding.setMovie(movie);

        movieId = movie.id;
        movieTitle = movie.title;

        prepareFav();

        APIHelper apiHelper = new APIHelper(this);
        apiHelper.getTrailersForMovie(movieId).enqueue(new Callback<TrailerListResponse>() {
            @Override
            public void onResponse(Call<TrailerListResponse> call, Response<TrailerListResponse> response) {
                trailersAdapter = new TrailersAdapter(new TrailersAdapter.OnClickListener() {
                    @Override
                    public void onTrailerItemClick(Trailer trailer) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(trailer.getLink())));
                    }
                });
                List<Trailer> trailers = response.body().trailers;
                if (trailers == null || trailers.isEmpty()) {
                    rvTrailers.setVisibility(View.GONE);
                    Log.v(TAG, "Raw body: " + response.raw().body());
                } else {
                    Log.v(TAG, "Got " + trailers.size() + " trailers to display");
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

            @Override
            public void onFailure(Call<TrailerListResponse> call, Throwable t) {

            }
        });
        apiHelper.getReviewsForMovie(movieId).enqueue(new Callback<ReviewListResponse>() {
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

            @Override
            public void onFailure(Call<ReviewListResponse> call, Throwable t) {

            }
        });
        Log.v(TAG, "Getting poster " + movie.posterPath);

        Glide.with(this).load(getString(R.string.poster_url_prefix) + 780 +
                movie.posterPath)
                .priority(Priority.IMMEDIATE)
                .into(posterBackground);
    }
}
