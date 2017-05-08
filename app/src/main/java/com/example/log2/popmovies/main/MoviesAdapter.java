package com.example.log2.popmovies.main;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.android.volley.Response;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.log2.popmovies.R;
import com.example.log2.popmovies.data.ListType;
import com.example.log2.popmovies.helpers.DelayedWarning;
import com.example.log2.popmovies.model.Movie;
import com.example.log2.popmovies.model.MovieListResponse;
import com.example.log2.popmovies.network.APIHelper;
import com.example.log2.popmovies.network.TheMovieDbUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;


/**
 * Created by Lorenzo on 21/01/2017.
 */
public class MoviesAdapter extends RecyclerView.Adapter<MoviesAdapter.MoviesViewHolder> {
    private static final String TAG = MoviesAdapter.class.getSimpleName();
    private final int totalResults;
    private final ListType listType;
    private final MovieClickListener movieClickListener;

    public MoviesAdapter(Context context, ListType listType, int count, MovieClickListener movieClickListener) {
        this.listType = listType;
        this.movieClickListener = movieClickListener;
        this.totalResults = count;
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
        void clickMovie(Movie movie);
    }

    public class MoviesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final com.example.log2.popmovies.network.APIHelper APIHelper;
        private final Context context;
        //private final TextView mv_position;
        @BindView(R.id.movie_image)
        ImageView movieView;
        @BindView(R.id.pb_loading)
        ProgressBar pbLoading;
        Request lastGlideRequest;
        private Movie movie;
        private Call<MovieListResponse> lastCall;


        public MoviesViewHolder(View view, Context context) {
            super(view);
            ButterKnife.bind(this, view);
            this.context = context;
            APIHelper = new APIHelper(context);
            movieView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            Log.v(TAG, "Clicked " + adapterPosition);

            movieClickListener.clickMovie(movie);
        }

        public void setMovie(final int position) {

            onMovie(listType, position, new Response.Listener<Movie>() {
                @Override
                public void onResponse(Movie movie) {

                    MoviesViewHolder.this.movie = movie;
                    // w185 -> 185x277
                    // w342 -> 342x513
                    int expectedWidth = 342;
                    //int expectedHeight = 513;
                    final DelayedWarning delayedWarning = DelayedWarning.showingTemporarily(pbLoading);
                    addGlideRequest(Glide.with(context).load(APIHelper.getPoster(expectedWidth, movie.posterPath))
                            //.override(expectedWidth, expectedHeight)
                            .priority(Priority.IMMEDIATE)
                            .listener(new RequestListener<String, GlideDrawable>() {
                                @Override
                                public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                    hideLoadingIndicator();
                                    return false;
                                }

                                private void hideLoadingIndicator() {
                                    delayedWarning.hide();
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
                }
            });
        }

        private void onMovie(final ListType listType, final int position, final Response.Listener<Movie> listener) {
            if (lastCall != null) {
                lastCall.cancel();
                lastCall = null;
            }
            if (lastGlideRequest != null) {
                lastGlideRequest.clear();
                lastGlideRequest = null;
            }
            final int pageLength = 20;
            int initialPage = 1;
            final int page = initialPage + position / pageLength;
            Call<MovieListResponse> call = TheMovieDbUtils.getMovies(listType, page);
            call.enqueue(new Callback<MovieListResponse>() {
                @Override
                public void onResponse(Call<MovieListResponse> call, retrofit2.Response<MovieListResponse> response) {
                    final MovieListResponse movieListResponse = response.body();
                    if (movieListResponse == null)

                        onFailure(call, new RuntimeException("movie response is null (code = " + response.code() + ", message = " + response.message() + ")"));
                    else {
                        List<Movie> movies = movieListResponse.movies;
                        if (movies == null)
                            onFailure(call, new RuntimeException("movie list is null (code = " + movieListResponse.statusCode + ", message = " + movieListResponse.statusMessage + ")"));
                        else {
                            Movie movie = movies.get(position % pageLength);
                            listener.onResponse(movie);
                        }
                    }
                }

                @Override
                public void onFailure(Call<MovieListResponse> call, Throwable t) {
                    throw new RuntimeException("Could not get page " + page + ", listType = " + listType, t);
                }
            });

            lastCall = call;

        }

        private void addGlideRequest(Request newGlideRequest) {
            lastGlideRequest = newGlideRequest;
        }

    }

}