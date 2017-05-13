package com.example.log2.popmovies.main;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
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
import com.example.log2.popmovies.application.CustomApplication;
import com.example.log2.popmovies.data.FavoriteContract;
import com.example.log2.popmovies.data.ListType;
import com.example.log2.popmovies.helpers.DelayedWarning;
import com.example.log2.popmovies.model.Movie;
import com.example.log2.popmovies.network.APIHelper;

import java.text.MessageFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;


/**
 * Created by Lorenzo on 21/01/2017.
 */
public class FavoriteMoviesAdapter extends RecyclerView.Adapter<FavoriteMoviesAdapter.MoviesViewHolder> {
    private static final String TAG = FavoriteMoviesAdapter.class.getSimpleName();
    private final Context context;
    private final ListType listType;
    private final MovieClickListener movieClickListener;
    private final Cursor cursor;

    public FavoriteMoviesAdapter(Context context, ListType listType, Cursor cursor, MovieClickListener movieClickListener) {
        this.context = context;
        this.listType = listType;
        this.cursor = cursor;
        this.movieClickListener = movieClickListener;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public MoviesViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.movie_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, viewGroup, false);
        return new MoviesViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(MoviesViewHolder holder, int position) {
        holder.setMovie(position);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public APIHelper getApiHelper() {
        return getCustomApplication().getApiHelper();
    }

    public CustomApplication getCustomApplication() {
        return (CustomApplication) context.getApplicationContext();
    }

    public interface MovieClickListener {
        void clickMovie(Movie movie);
    }

    public class MoviesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        //private final TextView mv_position;
        private final Context context;
        @BindView(R.id.movie_image)
        ImageView movieView;
        @BindView(R.id.pb_loading)
        ProgressBar pbLoading;
        Request lastGlideRequest;
        private Movie movie;

        public MoviesViewHolder(View view, Context context) {
            super(view);
            ButterKnife.bind(this, view);
            this.context = context;
            movieView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            movieClickListener.clickMovie(movie);
        }

        public void setMovie(final int position) {

            onMovie(listType, position, new Response.Listener<Movie>() {


                @Override
                public void onResponse(Movie movieContent) {
                    movie = movieContent;
                    // w185 -> 185x277
                    // w342 -> 342x513
                    int expectedWidth = 342;
                    //int expectedHeight = 513;
                    final DelayedWarning delayedWarning = DelayedWarning.showingTemporarily(pbLoading);
                    Glide.with(context).load(getApiHelper().getPosterWide(movie.posterPath))
                            .priority(Priority.LOW).preload();
                    addGlideRequest(Glide.with(context).load(getApiHelper().getPoster(expectedWidth, movieContent.posterPath))
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


        private void onMovie(ListType listType, final int position, final Response.Listener<Movie> listener) {
            Call<Movie> call = getApiHelper().getMovie(getIdAt(position));
            call.enqueue(new Callback<Movie>() {
                @Override
                public void onResponse(Call<Movie> call, retrofit2.Response<Movie> response) {
                    listener.onResponse(response.body());
                }

                @Override
                public void onFailure(Call<Movie> call, Throwable t) {
                    throw new RuntimeException(MessageFormat.format(context.getString(R.string.malformed_json_object), position), t);
                }
            });
            if (lastGlideRequest != null) {
                lastGlideRequest.clear();
                lastGlideRequest = null;
            }
        }

        private String getIdAt(int position) {
            cursor.moveToPosition(position);
            return cursor.getString(cursor.getColumnIndex(FavoriteContract.FavoriteEntry.COLUMN_TMDB_ID));
        }

        private void addGlideRequest(Request newGlideRequest) {
            lastGlideRequest = newGlideRequest;
        }

    }
}
