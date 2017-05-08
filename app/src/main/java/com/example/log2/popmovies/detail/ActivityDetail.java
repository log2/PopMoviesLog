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
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.example.log2.popmovies.R;
import com.example.log2.popmovies.data.FavoriteContract;
import com.example.log2.popmovies.data.ListType;
import com.example.log2.popmovies.databinding.ActivityDetailBinding;
import com.example.log2.popmovies.model.Movie;
import com.example.log2.popmovies.model.Review;
import com.example.log2.popmovies.model.ReviewListResponse;
import com.example.log2.popmovies.model.Trailer;
import com.example.log2.popmovies.model.TrailerListResponse;
import com.example.log2.popmovies.network.TheMovieDbUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityDetail extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean> {
    public static final String MOVIE_LIST_TYPE = Intent.EXTRA_TEXT;
    private static final String MOVIE_ID = "movieId";
    private static final String TAG = ActivityDetail.class.getSimpleName();
    public static int IS_FAVORITE_LOADER = 43;
    @BindView(R.id.cb_fav)
    CheckBox checkBox;
    @BindView(R.id.iv_poster)
    ImageView iv_poster;

    @BindView(R.id.rv_trailers)
    RecyclerView rvTrailers;
    @BindView(R.id.rv_reviews)
    RecyclerView rvReviews;
    private String movieId;
    private String movieTitle;
    private ActivityDetailBinding binding;
    private TrailersAdapter trailersAdapter;
    private ReviewsAdapter reviewsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail);
        ButterKnife.bind(this);
        getSupportLoaderManager().initLoader(IS_FAVORITE_LOADER, null, this);
        if (!isOnline())
            Toast.makeText(this, R.string.no_internet_no_details, Toast.LENGTH_LONG).show();

        Intent intent = getIntent();

        if (intent != null && intent.hasExtra(MOVIE_LIST_TYPE)) {
            ListType listType = ListType.valueOf(intent.getStringExtra(MOVIE_LIST_TYPE));

            if (intent.hasExtra(getString(R.string.extraMovieContentKey))) {
                final Movie movie = intent.getParcelableExtra(getString(R.string.extraMovieContentKey));
                bindMovie(movie);
            }
        }

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
    public void onLoadFinished(Loader<Boolean> loader, Boolean data) {
        if (data != null) {
            if (checkBox != null) {
                checkBox.setChecked(data);
                checkBox.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    private void bindMovie(Movie movie) {

        binding.setMovie(movie);

        movieId = movie.id;
        movieTitle = movie.title;

        prepareFav();

        TheMovieDbUtils.getTrailersForMovie(movieId).enqueue(new Callback<TrailerListResponse>() {
            @Override
            public void onResponse(Call<TrailerListResponse> call, Response<TrailerListResponse> response) {
                trailersAdapter = new TrailersAdapter(new TrailersAdapter.OnClickListener() {
                    @Override
                    public void onTrailerItemClick(Trailer trailer) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(trailer.getLink())));
                    }
                });
                List<Trailer> trailers = response.body().trailers;
                trailersAdapter.setTrailers(trailers == null ? new Trailer[]{} : trailers.toArray(new Trailer[]{}));
                rvTrailers.setAdapter(trailersAdapter);
            }

            @Override
            public void onFailure(Call<TrailerListResponse> call, Throwable t) {

            }
        });
        TheMovieDbUtils.getReviewsForMovie(movieId).enqueue(new Callback<ReviewListResponse>() {
            @Override
            public void onResponse(Call<ReviewListResponse> call, Response<ReviewListResponse> response) {
                reviewsAdapter = new ReviewsAdapter();
                List<Review> reviews = response.body().reviews;
                reviewsAdapter.setReviews(reviews == null ? new Review[]{} : reviews.toArray(new Review[]{}));
                rvReviews.setAdapter(reviewsAdapter);
            }

            @Override
            public void onFailure(Call<ReviewListResponse> call, Throwable t) {

            }
        });
        Log.v(TAG, "Getting poster " + movie.posterPath);
        Glide.with(ActivityDetail.this).load(getString(R.string.poster_url_prefix) + 342 +
                movie.posterPath)
                //.override(342, 513)
                .priority(Priority.IMMEDIATE)
                .into(iv_poster);
    }

    private void prepareFav() {
        Bundle checkFavBundle = new Bundle();
        checkFavBundle.putString(MOVIE_ID, movieId);

        checkBox.setVisibility(View.INVISIBLE);

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
        final boolean movieHasAtLeastOneTrailer = (0 != trailersAdapter.getItemCount());
        if (movieHasAtLeastOneTrailer) {
            final Trailer[] trailers = trailersAdapter.getTrailers();
            final Trailer firstTrailer = trailers[0];
            final String firstTrailerLink = firstTrailer.getLink();

            return getString(R.string.share_movie_text_with_trailer, movieTitle, firstTrailerLink);
        } else {
            return getString(R.string.share_movie_text, movieTitle);
        }
    }

    public void favorite(View view) {
        final boolean checked = !checkBox.isChecked();
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
                checkBox.setEnabled(false);
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                checkBox.setEnabled(true);
            }
        }.execute(movieId);
    }
}
