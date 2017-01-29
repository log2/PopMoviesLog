package com.example.log2.popmovies;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.android.volley.Response;

import org.json.JSONObject;

import static com.example.log2.popmovies.NetworkUtils.reqHigh;
import static com.example.log2.popmovies.NetworkUtils.theMovieDB;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    GridLayoutManager layoutManager;
    ListType listType = ListType.POPULAR;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (layoutManager != null) {
            // Checks the orientation of the screen
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                layoutManager.setSpanCount(3);
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                layoutManager.setSpanCount(2);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        recyclerView = (RecyclerView) findViewById(R.id.rv_movies);
        recyclerView.setHasFixedSize(true);
        recyclerView.setClipToPadding(true);
        recyclerView.setClipChildren(true);
        recyclerView.setItemViewCacheSize(100);

        initializeAdapter();
        final Context context = this;
        // TODO change orientation when device orientation changes
//        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL);
//        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        layoutManager = new GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false);
        layoutManager.setSmoothScrollbarEnabled(true);
        recyclerView.setLayoutManager(layoutManager);

    }

    @NonNull
    private Context initializeAdapter() {
        final Context context = this;
        VolleyHolder.in(this).add(reqHigh(theMovieDB("/movie/" + listType.getUrlFragment()), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                recyclerView.setAdapter(new MoviesAdapter(listType, jsonObject, new MoviesAdapter.MovieClickListener() {

                    @Override
                    public void clickMovie(int movieId) {
                        Intent intent = new Intent(context, DetailActivity.class);
                        intent.putExtra(Intent.EXTRA_INDEX, movieId);
                        intent.putExtra(Intent.EXTRA_TEXT, listType.toString());
                        startActivity(intent);
                    }
                }));
            }
        }));
        return context;
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
            listType = listType == ListType.POPULAR ? ListType.TOP_RATED : ListType.POPULAR;
            initializeAdapter();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
