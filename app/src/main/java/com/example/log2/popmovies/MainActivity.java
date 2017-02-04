package com.example.log2.popmovies;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
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

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;
    private ListType listType = null;


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

        if (isOnline()) {
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
            setListType(ListType.POPULAR);
        } else {
            Toast.makeText(this, R.string.internet_needed_for_app, Toast.LENGTH_LONG).show();
            finish();
        }
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
        final Toast wipToast = Toast.makeText(this, R.string.preparingMoviesList, Toast.LENGTH_LONG);
        final DelayedWarning delayedWarning = new DelayedWarning(new Runnable() {
            @Override
            public void run() {
                wipToast.show();
            }
        });
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
                delayedWarning.hide(new Runnable() {
                    @Override
                    public void run() {
                        wipToast.cancel();
                    }
                });
            }
        }));
    }


    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    protected void onStop() {
        VolleyHolder.in(this).cancelAll();
        super.onStop();
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
        } else if (id == R.id.menu_tmdb_credit) {
            Toast.makeText(this, R.string.courtesy_tmdb, Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setListType(ListType newListType) {
        if (newListType != listType) {
            listType = newListType;
            setTitle(listType == ListType.POPULAR ? getString(R.string.most_popular) : getString(R.string.top_rated));
            initializeAdapter();
        }
    }
}
