package com.example.log2.goodmovies;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by Lorenzo on 21/01/2017.
 */
public class MoviesAdapter extends RecyclerView.Adapter<MoviesAdapter.MoviesViewHolder> {
    @Override
    public MoviesViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.movie_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        return new MoviesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MoviesViewHolder holder, int position) {
        holder.setMovie();
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class MoviesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView movieView;

        public MoviesViewHolder(View view) {
            super(view);
            movieView = (ImageView) view.findViewById(R.id.movie_image);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
        }

        public void setMovie() {
            /*
            Glide
                    .with(myFragment)
                    .load(url)
                    .centerCrop()
                    .placeholder(R.drawable.loading_spinner)
                    .crossFade()
                    .into(myImageView);
                    */
        }
    }
}
