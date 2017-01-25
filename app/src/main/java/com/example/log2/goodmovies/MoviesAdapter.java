package com.example.log2.goodmovies;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lorenzo on 21/01/2017.
 */
public class MoviesAdapter extends RecyclerView.Adapter<MoviesAdapter.MoviesViewHolder> {
    private static final String TAG = MoviesAdapter.class.getSimpleName();
    private final int totalResults;

    private Map<Integer, JSONObject> dataByPage = new HashMap<>();

    public MoviesAdapter(JSONObject initialPage) {
        try {
            this.totalResults = initialPage.getInt("total_results");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        dataByPage.put(1, initialPage);
    }

    @Override
    public MoviesViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.movie_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        return new MoviesViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(MoviesViewHolder holder, int position) {
        holder.setMovie(position);
    }

    @Override
    public int getItemCount() {
        return totalResults;
    }

    private void onItem(int position, final OnItem onItem) {
        // TODO handle cache properly!
        final int page = position / 20 + 1;
        final int subPosition = position % 20;
        Log.v(TAG, "Accessing item #" + position + " (page: " + page + ", offset: " + subPosition + ")");
        if (dataByPage.containsKey(page)) {
            JSONObject pageContent = dataByPage.get(page);
            doOnPageItem(pageContent, subPosition, onItem);
        } else {
            new AsyncTask<Integer, Void, JSONObject>() {
                @Override
                protected JSONObject doInBackground(Integer... page) {
                    return NetworkUtils.queryTheMovieDb("/movie/popular", new String[]{"page", "" + page});
                }

                @Override
                protected void onPostExecute(JSONObject pageContent) {
                    dataByPage.put(page, pageContent);
                    doOnPageItem(pageContent, subPosition, onItem);
                }
            }.execute();
        }
    }

    private void doOnPageItem(JSONObject pageContent, int subPosition, OnItem onItem) {
        try {
            onItem.doOnItem(pageContent.getJSONArray("results").getJSONObject(subPosition));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private interface OnItem {
        void doOnItem(JSONObject item) throws JSONException;
    }

    public class MoviesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView movieView;
        private Context context;

        public MoviesViewHolder(View view, Context context) {
            super(view);
            movieView = (ImageView) view.findViewById(R.id.movie_image);
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            // TODO handle click
        }

        public void setMovie(int position) {
            onItem(position, new OnItem() {
                public void doOnItem(JSONObject item) throws JSONException {
                    Picasso.with(context).load("http://image.tmdb.org/t/p/w185" + item.getString("poster_path")).into(movieView);
                }
            });
        }

    }
}
