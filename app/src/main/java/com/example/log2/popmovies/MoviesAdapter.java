package com.example.log2.popmovies;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;

import static com.example.log2.popmovies.NetworkUtils.reqLow;
import static com.example.log2.popmovies.NetworkUtils.theMovieDB;

/**
 * Created by Lorenzo on 21/01/2017.
 */
public class MoviesAdapter extends RecyclerView.Adapter<MoviesAdapter.MoviesViewHolder> {
    private static final String TAG = MoviesAdapter.class.getSimpleName();
    private final int totalResults;
    private final ListType listType;
    private final MovieClickListener movieClickListener;

    public MoviesAdapter(ListType listType, JSONObject initialPage, MovieClickListener movieClickListener) {
        this.listType = listType;
        this.movieClickListener = movieClickListener;
        try {
            this.totalResults = initialPage.getInt("total_results");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
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

    public interface MovieClickListener {
        void clickMovie(int movieId);
    }

    public class MoviesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        //private final TextView mv_position;
        final ImageView movieView;
        private final Context context;
        private final ProgressBar pbLoading;
        Request glideRequest;
        private JsonObjectRequest volleyRequest;

        public MoviesViewHolder(View view, Context context) {
            super(view);
            movieView = (ImageView) view.findViewById(R.id.movie_image);
            pbLoading = (ProgressBar) view.findViewById(R.id.pb_loading);
            //mv_position = (TextView) view.findViewById(R.id.mv_position);
            this.context = context;
            movieView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            movieClickListener.clickMovie(adapterPosition);
        }

        public void setMovie(final int position) {

            onMovie(listType, position, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject movieContent) {
                    try {
                        // w185 -> 185x277
                        // w342 -> 342x513
                        int expectedWidth = 342;
                        int expectedHeight = 513;
                        final DelayedWarning delayedWarning = new DelayedWarning(new Runnable() {
                            @Override
                            public void run() {
                                pbLoading.setVisibility(View.VISIBLE);
                            }
                        });
                        addGlideRequest(Glide.with(context).load("http://image.tmdb.org/t/p/w" + expectedWidth +
                                movieContent.getString("poster_path"))
                                //.override(expectedWidth, expectedHeight)
                                .priority(Priority.IMMEDIATE)
                                .listener(new RequestListener<String, GlideDrawable>() {
                                    @Override
                                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                        hideLoadingIndicator();
                                        return false;
                                    }

                                    private void hideLoadingIndicator() {
                                        delayedWarning.hide(new Runnable() {
                                            @Override
                                            public void run() {
                                                pbLoading.setVisibility(View.INVISIBLE);
                                            }
                                        });
                                    }

                                    @Override
                                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                        hideLoadingIndicator();
                                        return false;
                                    }
                                }).error(R.drawable.ic_load_failed)
                                //.placeholder(android.R.drawable.gallery_thumb)
                                //.crossFade(android.R.anim.fade_in, 250)
                                .animate(android.R.anim.fade_in)
                                .into(movieView).getRequest());
                    } catch (JSONException e) {
                        throw new RuntimeException(MessageFormat.format(context.getString(R.string.malformed_json_object), position), e);
                    }
                }
            });
        }

        private void onMovie(ListType listType, final int position, final Response.Listener<JSONObject> listener) {
            final int page = 1 + position / 20;
            final int subPosition = position % 20;
            //Log.v(TAG, "Setting position of " + this + " to " + position + "(" + page + ":" + subPosition);
            //mv_position.setText(page + ":" + subPosition);
            addNewRequest(reqLow(theMovieDB(context.getString(R.string.themoviedb_base_api) + listType.getUrlFragment(), new String[]{"page", "" + page}), new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject pageContent) {
                    try {
                        JSONObject item = pageContent.getJSONArray("results").getJSONObject(subPosition);
                        listener.onResponse(item);
                    } catch (JSONException e) {
                        throw new RuntimeException(MessageFormat.format(context.getString(R.string.malformed_json_object), position), e);
                    }
                }
            }));
        }

        private void addGlideRequest(Request newGlideRequest) {
            glideRequest = newGlideRequest;
        }

        private void addNewRequest(JsonObjectRequest newVolleyRequest) {
            if (glideRequest != null) {
                glideRequest.clear();
                glideRequest = null;
            }
            if (volleyRequest != null) {
                volleyRequest.cancel();
                volleyRequest = null;
            }
            volleyRequest = newVolleyRequest;
            VolleyHolder.in(context).add(newVolleyRequest);
        }
    }
}
