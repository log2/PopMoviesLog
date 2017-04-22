package com.example.log2.popmovies;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Response;
import com.example.log2.popmovies.data.FavoriteContract;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int FAVORITE_LOADER = 42;
    private DelayedWarning loadWarning;
    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;
    private ListType listType = null;

    @Override
    public Loader<Cursor> onCreateLoader(int id, final Bundle args) {
        if (args != null) createLoadWarning();
        return new AsyncTaskLoader<Cursor>(this) {
            @Override
            protected void onStartLoading() {
                if (args == null) {
                    return;
                }
                forceLoad();
            }

            @Override
            public Cursor loadInBackground() {
                Uri uri = FavoriteContract.FavoriteEntry.CONTENT_URI;
                uri = uri.buildUpon().build();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                return cursor;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        data.registerContentObserver(new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                //data.close();
                startLoading(new Bundle());
            }
        });

        showFavorites(data);
        if (data != null) hideLoadWarning();
    }

    private void showFavorites(Cursor data) {
        recyclerView.setAdapter(new FavoriteMoviesAdapter(MainActivity.this, listType, data, new FavoriteMoviesAdapter.MovieClickListener() {

            @Override
            public void clickMovie(int movieId) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra(getString(R.string.extra_movie_id), movieId);
                intent.putExtra(DetailActivity.MOVIE_LIST_TYPE, listType.toString());
                startActivity(intent);
            }
        }));
    }

    private void hideLoadWarning() {
        if (loadWarning != null) {
            loadWarning.hide();
            loadWarning = null;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setSpanCount();
    }

    private void setSpanCount() {
        if (layoutManager != null) {
            layoutManager.setSpanCount(getIdealSpanCount());
        }
    }

    private int getIdealSpanCount() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int availableWidthDp = pixelToDp(size.x);
        int horizontal_margin = pixelToDp((int) getResources().getDimension(R.dimen.horizontal_margin));
        int imageWidthDp = (144 + horizontal_margin * 2);
        return availableWidthDp / imageWidthDp;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isOnline())
            Toast.makeText(this, R.string.internet_needed_for_app, Toast.LENGTH_LONG).show();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        recyclerView = (RecyclerView) findViewById(R.id.rv_movies);
        recyclerView.setHasFixedSize(true);
        recyclerView.setClipToPadding(true);
        recyclerView.setClipChildren(true);
        recyclerView.setItemViewCacheSize(100);

        final Context context = this;

        layoutManager = new GridLayoutManager(context, getIdealSpanCount(), GridLayoutManager.VERTICAL, false);
        layoutManager.setSmoothScrollbarEnabled(true);
        recyclerView.setLayoutManager(layoutManager);

        startLoading(null);
        setListType(getListType(savedInstanceState));
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

    private void initializeAdapter() {
        final Context context = this;
        if (listType == ListType.FAVORITES) {
            // Use loader rather than Volley
            startLoading(new Bundle());
        } else {
            stopLoading();
            createLoadWarning();
            APIHelper APIHelper = new APIHelper(this);
            VolleyHolder.in(this).add(APIHelper.reqHigh(APIHelper.getPage(listType), new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    recyclerView.setAdapter(new MoviesAdapter(context, listType, jsonObject, new MoviesAdapter.MovieClickListener() {

                        @Override
                        public void clickMovie(int movieId) {
                            Intent intent = new Intent(context, DetailActivity.class);
                            intent.putExtra(getString(R.string.extra_movie_index), movieId);
                            intent.putExtra(DetailActivity.MOVIE_LIST_TYPE, listType.toString());
                            startActivity(intent);
                        }
                    }));
                    hideLoadWarning();
                }
            }));
        }
    }

    private void startLoading(Bundle queryBundle) {
        Loader<Cursor> loader = getSupportLoaderManager().getLoader(FAVORITE_LOADER);

        if (loader == null) {
            getSupportLoaderManager().initLoader(FAVORITE_LOADER, queryBundle, this);
        } else {
            getSupportLoaderManager().restartLoader(FAVORITE_LOADER, queryBundle, this);
        }
    }

    private void createLoadWarning() {
        final Toast wipToast = Toast.makeText(this, R.string.preparingMoviesList, Toast.LENGTH_LONG);
        loadWarning = new DelayedWarning(new Runnable() {
            @Override
            public void run() {
                wipToast.show();
            }
        }, new Runnable() {
            @Override
            public void run() {
                wipToast.cancel();
            }
        });
    }




    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("listType", listType.name());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ListType listType = getListType(savedInstanceState);
        setListType(listType);
    }

    @NonNull
    private ListType getListType(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("listType")) {
            String listTypeName = savedInstanceState.getString("listType");
            return ListType.valueOf(ListType.class, listTypeName);
        } else return ListType.POPULAR;
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        startLoading(null);
        super.onStart();
    }

    @Override
    protected void onRestart() {
        startLoading(null);
        super.onRestart();
    }

    @Override
    protected void onStop() {
        VolleyHolder.in(this).cancelAll();
        stopLoading();
        super.onStop();
    }

    private void stopLoading() {
        getSupportLoaderManager().destroyLoader(FAVORITE_LOADER);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menuMostPopular) {
            setListType(ListType.POPULAR);
            return true;
        } else if (id == R.id.menuTopRated) {
            setListType(ListType.TOP_RATED);
            return true;
        } else if (id == R.id.menuFavorite) {
            setListType(ListType.FAVORITES);
            return true;
        } else if (id == R.id.menu_tmdb_credit) {
            Toast.makeText(this, R.string.courtesy_tmdb, Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setListType(ListType newListType) {
        if (newListType != listType) {
            listType = newListType;
            setTitle(listType == ListType.POPULAR ? getString(R.string.most_popular) : (listType == ListType.FAVORITES ? getString(R.string.favorite_movies) : getString(R.string.top_rated)));
            initializeAdapter();
        }
    }
}
