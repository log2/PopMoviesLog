package com.example.log2.popmovies;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;

public class DetailActivity extends AppCompatActivity {
    public static final String MOVIE_LIST_TYPE = Intent.EXTRA_TEXT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isOnline()) {
            Toast.makeText(this, R.string.no_internet_no_details, Toast.LENGTH_LONG).show();
            finish();
        } else {
            setContentView(R.layout.activity_detail);
            Intent intent = getIntent();

            if (intent != null && intent.hasExtra(getString(R.string.extra_movie_index)) && intent.hasExtra(MOVIE_LIST_TYPE)) {
                final int position = intent.getIntExtra(getString(R.string.extra_movie_index), -1);
                ListType listType = ListType.valueOf(intent.getStringExtra(MOVIE_LIST_TYPE));
                onMovie(listType, position, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject movieContent) {
                        try {
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
                            tv_title.setText(movieContent.getString(getString(R.string.json_attr_title)));
                            double rating = movieContent.getDouble(getString(R.string.json_attr_vote_average));
                            rb.setNumStars(5);
                            rb.setRating((float) ((5 * rating) / 10));
                            Glide.with(DetailActivity.this).load(getString(R.string.poster_url_prefix) + 342 +
                                    movieContent.getString(getString(R.string.json_attr_poster_path)))
                                    //.override(342, 513)
                                    .priority(Priority.IMMEDIATE)
                                    .into(iv_poster);
                        } catch (JSONException e) {
                            throw new RuntimeException(MessageFormat.format(getString(R.string.malformed_json_object), position), e);
                        }
                    }
                });
            }
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void onMovie(ListType listType, final int position, final Response.Listener<JSONObject> listener) {
        final int page = 1 + position / 20;
        final int subPosition = position % 20;
        //Log.v(TAG, "Setting position of " + this + " to " + position + "(" + page + ":" + subPosition);
        //mv_position.setText(page + ":" + subPosition);

        APIHelper APIHelper = new APIHelper(this);
        VolleyHolder.in(this).add(APIHelper.newReq(true, listType, position, listener));
    }


}
