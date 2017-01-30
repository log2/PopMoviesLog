package com.example.log2.popmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.android.volley.Response;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import static com.example.log2.popmovies.NetworkUtils.reqHigh;
import static com.example.log2.popmovies.NetworkUtils.theMovieDB;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_INDEX) && intent.hasExtra(Intent.EXTRA_TEXT)) {
            final int position = intent.getIntExtra(Intent.EXTRA_INDEX, -1);
            ListType listType = ListType.valueOf(intent.getStringExtra(Intent.EXTRA_TEXT));
            onMovie(listType, position, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject movieContent) {
                    try {
                        TextView tv_releasedate = (TextView) findViewById(R.id.tv_releasedate);
                        TextView tv_title = (TextView) findViewById(R.id.tv_title);
                        TextView tv_synopsis = (TextView) findViewById(R.id.tv_synopsis);
                        RatingBar rb = (RatingBar) findViewById(R.id.ratingBar);
                        ImageView iv_poster = (ImageView) findViewById(R.id.iv_poster);

                        tv_releasedate.setText(movieContent.getString("release_date"));
                        tv_synopsis.setText(movieContent.getString("overview") + "\n");
                        tv_title.setText(movieContent.getString("title"));
                        double rating = movieContent.getDouble("vote_average");
                        rb.setNumStars(5);
                        rb.setRating((float) ((5 * rating) / 10));
                        Glide.with(DetailActivity.this).load("http://image.tmdb.org/t/p/w" + 342 +
                                movieContent.getString("poster_path")).override(342, 513).placeholder(R.mipmap.ic_launcher)
                                .into(iv_poster);
                    } catch (JSONException e) {
                        throw new RuntimeException("Malformed JSON Object (item #" + position + ")", e);
                    }
                }
            });
        }
    }

    private void onMovie(ListType listType, final int position, final Response.Listener<JSONObject> listener) {
        final int page = 1 + position / 20;
        final int subPosition = position % 20;
        //Log.v(TAG, "Setting position of " + this + " to " + position + "(" + page + ":" + subPosition);
        //mv_position.setText(page + ":" + subPosition);
        VolleyHolder.in(this).add(reqHigh(theMovieDB("/movie/" + listType.getUrlFragment(), new String[]{"page", "" + page}), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject pageContent) {
                try {
                    JSONObject item = pageContent.getJSONArray("results").getJSONObject(subPosition);
                    listener.onResponse(item);
                } catch (JSONException e) {
                    throw new RuntimeException("Malformed JSON Object (item #" + position + ")", e);
                }
            }
        }));
    }


}
