package com.example.log2.popmovies;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.example.log2.popmovies.data.FavoriteContract;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean> {
    public static final String MOVIE_LIST_TYPE = Intent.EXTRA_TEXT;
    private static final String MOVIE_ID = "movieId";
    public static int IS_FAVORITE_LOADER = 43;
    CheckBox checkBox;
    private int movieId;
    private String movieTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportLoaderManager().initLoader(IS_FAVORITE_LOADER, null, this);
        if (!isOnline()) {
            Toast.makeText(this, R.string.no_internet_no_details, Toast.LENGTH_LONG).show();
            finish();
        } else {
            setContentView(R.layout.activity_detail);
            checkBox = (CheckBox) findViewById(R.id.cb_fav);
            Intent intent = getIntent();

            if (intent != null && intent.hasExtra(MOVIE_LIST_TYPE)) {
                ListType listType = ListType.valueOf(intent.getStringExtra(MOVIE_LIST_TYPE));

                if (intent.hasExtra(getString(R.string.extra_movie_id))) {
                    final int movieId = intent.getIntExtra(getString(R.string.extra_movie_id), -1);
                    onMovieById(movieId, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject movieContent) {
                            try {
                                setFrom(movieContent);
                            } catch (JSONException e) {
                                throw new RuntimeException(MessageFormat.format("Malformed JSON Object (movie #{0})", movieId), e);
                            }
                        }
                    });
                } else if (intent.hasExtra(getString(R.string.extra_movie_index))) {
                    final int position = intent.getIntExtra(getString(R.string.extra_movie_index), -1);
                    onMovie(listType, position, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject movieContent) {
                            try {
                                setFrom(movieContent);
                            } catch (JSONException e) {
                                throw new RuntimeException(MessageFormat.format(getString(R.string.malformed_json_object), position), e);
                            }
                        }
                    });
                }
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
                uri = uri.buildUpon().build();
                int movieId = args.getInt(MOVIE_ID);
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

    private void setFrom(JSONObject movieContent) throws JSONException {
        TextView tv_releasedate = (TextView) findViewById(R.id.tv_releasedate);
        TextView tv_title = (TextView) findViewById(R.id.tv_title);
        TextView tv_synopsis = (TextView) findViewById(R.id.tv_synopsis);
        RatingBar rb = (RatingBar) findViewById(R.id.ratingBar);
        ImageView iv_poster = (ImageView) findViewById(R.id.iv_poster);

        tv_releasedate.setText(movieContent.getString(getString(R.string.json_attr_release_date)));
        String overview = movieContent.getString(getString(R.string.json_attr_overview));
        // Newline is needed to avoid awful text justification on last line
        String overview_with_newline = overview + "\n";
        tv_synopsis.setText(overview_with_newline);
        movieTitle = movieContent.getString(getString(R.string.json_attr_title));
        tv_title.setText(movieTitle);
        double rating = movieContent.getDouble(getString(R.string.json_attr_vote_average));
        rb.setNumStars(5);
        rb.setRating((float) ((5 * rating) / 10));
        movieId = movieContent.getInt("id");
        prepareFav();

        Glide.with(DetailActivity.this).load(getString(R.string.poster_url_prefix) + 342 +
                movieContent.getString(getString(R.string.json_attr_poster_path)))
                //.override(342, 513)
                .priority(Priority.IMMEDIATE)
                .into(iv_poster);
    }

    private void prepareFav() {
        Bundle checkFavBundle = new Bundle();
        checkFavBundle.putInt(MOVIE_ID, movieId);

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

    private void onMovie(ListType listType, final int position, final Response.Listener<JSONObject> listener) {
        APIHelper APIHelper = new APIHelper(this);
        VolleyHolder.in(this).add(APIHelper.newReq(true, listType, position, listener));
    }

    private void onMovieById(final int movieId, final Response.Listener<JSONObject> listener) {
        APIHelper APIHelper = new APIHelper(this);
        VolleyHolder.in(this).add(APIHelper.newReqById(true, movieId, listener));
    }

    public void favorite(View view) {
        final boolean checked = !checkBox.isChecked();
        new AsyncTask<Integer, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Integer... params) {
                if (checked) {
                    getContentResolver().delete(FavoriteContract.FavoriteEntry.CONTENT_URI.buildUpon().appendPath("" + params[0]).build(), null, null);
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
