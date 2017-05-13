package com.example.log2.popmovies.main;

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
import android.support.design.widget.Snackbar;
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

import com.example.log2.popmovies.R;
import com.example.log2.popmovies.application.CustomApplication;
import com.example.log2.popmovies.data.FavoriteContract;
import com.example.log2.popmovies.data.ListType;
import com.example.log2.popmovies.detail.ScrollingActivity;
import com.example.log2.popmovies.helpers.DelayedWarning;
import com.example.log2.popmovies.model.Movie;
import com.example.log2.popmovies.model.MovieCount;
import com.example.log2.popmovies.network.APIHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int FAVORITE_LOADER = 42;
    private static final String TAG = MainActivity.class.getSimpleName();
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.rv_movies)
    RecyclerView recyclerView;
    private DelayedWarning loadWarning;
    private GridLayoutManager layoutManager;
    private ListType listType = null;
    private APIHelper apiHelper;

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
                return getContentResolver().query(uri, null, null, null, null);
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
        hideLoadWarning();
    }

    private void showFavorites(Cursor data) {
        recyclerView.setAdapter(new FavoriteMoviesAdapter(MainActivity.this, listType, data, new FavoriteMoviesAdapter.MovieClickListener() {

            @Override
            public void clickMovie(Movie movie) {
                openMovieDetail(movie);

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
            showSnackbar(R.string.internet_needed_for_app);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        recyclerView.setHasFixedSize(true);
        recyclerView.setClipToPadding(true);
        recyclerView.setClipChildren(true);
        recyclerView.setItemViewCacheSize(100);

        trackOurMainViewOnApiHolder();
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
            // Use loader rather than Retrofit2
            startLoading(new Bundle());
        } else {
            stopLoading();
            createLoadWarning();
            startupAdapter(context);
        }
    }

    private void startupAdapter(final Context context) {
        apiHelper = new APIHelper(this, recyclerView);
        apiHelper.getMoviesCount(listType).enqueue(new Callback<MovieCount>() {
            @Override
            public void onResponse(Call<MovieCount> call, retrofit2.Response<MovieCount> response) {

                recyclerView.setAdapter(new MoviesAdapter(context, toolbar, listType, response.body().count, new MoviesAdapter.MovieClickListener() {

                    @Override
                    public void clickMovie(Movie movie) {
                        openMovieDetail(movie);
                    }
                }));
                hideLoadWarning();
            }

            @Override
            public void onFailure(Call<MovieCount> call, Throwable t) {
                // FIXME add automatic retry with snackbar for signalling problem, not just do alerting
                alert(getString(R.string.couldInitialize));
                t.printStackTrace();
            }
        });
    }

    private void openMovieDetail(Movie movie) {
        Intent intent = new Intent(this, ScrollingActivity.class);
        intent.putExtra(getString(R.string.extraMovieContentKey), movie);
        intent.putExtra(ScrollingActivity.MOVIE_LIST_TYPE, listType.toString());
        startActivity(intent);
    }

    private void alert(String s) {
        Snackbar.make(recyclerView, s, Snackbar.LENGTH_LONG).show();
    }

    private void startLoading(Bundle queryBundle) {
        final LoaderManager supportLoaderManager = getSupportLoaderManager();
        Loader<Cursor> loader = supportLoaderManager.getLoader(FAVORITE_LOADER);

        if (loader == null) {
            supportLoaderManager.initLoader(FAVORITE_LOADER, queryBundle, this);
        } else {
            supportLoaderManager.restartLoader(FAVORITE_LOADER, queryBundle, this);
        }
    }

    private void createLoadWarning() {
        final Snackbar progress = Snackbar.make(recyclerView, R.string.preparingMoviesList, Snackbar.LENGTH_LONG);
        loadWarning = new DelayedWarning(new Runnable() {
            @Override
            public void run() {
                progress.show();
            }
        }, new Runnable() {
            @Override
            public void run() {
                progress.dismiss();
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
            if (listTypeName != null)
                return ListType.valueOf(ListType.class, listTypeName);
        }
        return ListType.POPULAR;
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    protected void onPause() {

        // FIXME anything to pause?
        super.onPause();
        untrackOurMainViewOnApiHolder();
    }


    @Override
    protected void onResume() {
        trackOurMainViewOnApiHolder();
        // FIXME anything to resume?
        super.onResume();
    }

    private void trackOurMainViewOnApiHolder() {
        getCustomApplication().getApiHelper().setView(recyclerView);
    }

    private void untrackOurMainViewOnApiHolder() {
        getCustomApplication().getApiHelper().setView(null);
    }

    private CustomApplication getCustomApplication() {
        return (CustomApplication) getApplication();
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
        (getCustomApplication()).getVolleyHolder().cancelAll();
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
            int courtesy_tmdb = R.string.courtesy_tmdb;
            showSnackbar(courtesy_tmdb);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSnackbar(int resourceId) {
        Snackbar.make(recyclerView, resourceId, Snackbar.LENGTH_LONG).show();
    }

    private void setListType(ListType newListType) {
        if (newListType != listType) {
            listType = newListType;
            setTitle(listType == ListType.POPULAR ? getString(R.string.most_popular) : (listType == ListType.FAVORITES ? getString(R.string.favorite_movies) : getString(R.string.top_rated)));
            initializeAdapter();
        }
    }
}
