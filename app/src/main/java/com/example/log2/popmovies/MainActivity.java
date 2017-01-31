package com.example.log2.popmovies;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Response;

import org.json.JSONObject;

import static com.example.log2.popmovies.NetworkUtils.reqHigh;
import static com.example.log2.popmovies.NetworkUtils.theMovieDB;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;
    private ListType listType = ListType.POPULAR;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        deriveLayoutSize(newConfig);
    }

    private int[] computeMaxDensities() {
        int densityDpi = getResources().getDisplayMetrics().densityDpi;
        switch (densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
            case DisplayMetrics.DENSITY_MEDIUM:
                return new int[]{7, 5};

            case DisplayMetrics.DENSITY_HIGH:
            default:
                return new int[]{3, 2};
        }
    }
    private void deriveLayoutSize(Configuration configuration) {
        if (layoutManager != null) {
            // Checks the orientation of the screen
            int orientation = configuration.orientation;

            int[] maxDensities = computeMaxDensities();
            switch (orientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    layoutManager.setSpanCount(maxDensities[0]);
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                    layoutManager.setSpanCount(maxDensities[1]);
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isOnline()) {
            Toast.makeText(this, R.string.internet_needed_for_app, Toast.LENGTH_LONG).show();
            finish();
        } else {
            setContentView(R.layout.activity_main);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);


            recyclerView = (RecyclerView) findViewById(R.id.rv_movies);
            recyclerView.setHasFixedSize(true);
            recyclerView.setClipToPadding(true);
            recyclerView.setClipChildren(true);
            recyclerView.setItemViewCacheSize(100);

            final Context context = this;

            layoutManager = new GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false);
            deriveLayoutSize(getResources().getConfiguration());
            layoutManager.setSmoothScrollbarEnabled(true);
            recyclerView.setLayoutManager(layoutManager);
            setListType(ListType.POPULAR);
        }
    }

    private void initializeAdapter() {
        final Context context = this;
        VolleyHolder.in(this).add(reqHigh(theMovieDB(getString(R.string.themoviedb_base_api) + listType.getUrlFragment()), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                recyclerView.setAdapter(new MoviesAdapter(listType, jsonObject, new MoviesAdapter.MovieClickListener() {

                    @Override
                    public void clickMovie(int movieId) {
                        Intent intent = new Intent(context, DetailActivity.class);
                        intent.putExtra(DetailActivity.MOVIE_INDEX, movieId);
                        intent.putExtra(DetailActivity.MOVIE_LIST_TYPE, listType.toString());
                        startActivity(intent);
                    }
                }));
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
        if (id == R.id.action_list) {
            setListType(this.listType == ListType.POPULAR ? ListType.TOP_RATED : ListType.POPULAR);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setListType(ListType newListType) {
        listType = newListType;
        setTitle(listType == ListType.POPULAR ? getString(R.string.most_popular) : getString(R.string.top_rated));
        initializeAdapter();
    }
}
