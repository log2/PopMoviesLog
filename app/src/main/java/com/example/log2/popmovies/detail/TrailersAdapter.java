package com.example.log2.popmovies.detail;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.example.log2.popmovies.R;
import com.example.log2.popmovies.model.Trailer;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Adapter for providing trailers for the RecyclerView.
 */
public class TrailersAdapter extends RecyclerView.Adapter<TrailersAdapter.TrailerViewHolder> {
    private final static String LOG_TAG = TrailersAdapter.class.getSimpleName();
    private Trailer[] trailers;
    private TrailersAdapter.OnClickListener listener;

    public TrailersAdapter(final TrailersAdapter.OnClickListener listener) {
        trailers = null;
        this.listener = listener;
    }

    @Override
    public TrailerViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final Context context = parent.getContext();

        final LayoutInflater inflater = LayoutInflater.from(context);

        final View view = inflater.inflate(R.layout.item_trailer, parent,
                false);

        return new TrailerViewHolder(context, view);
    }

    public Trailer[] getTrailers() {
        return trailers;
    }

    public void setTrailers(final Trailer[] reviews) {
        this.trailers = reviews;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final TrailerViewHolder holder, final int position) {
        final Trailer trailer = trailers[position];
        holder.bind(trailer);
    }

    @Override
    public int getItemCount() {
        if (trailers == null) return 0;
        return trailers.length;
    }

    public interface OnClickListener {
        void onTrailerItemClick(Trailer trailer);
    }

    /**
     * View holder for trailer items in the RecyclerView.
     */
    public class TrailerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final Context context;

        @BindView(R.id.image_trailer_thumbnail)
        ImageView trailerThumbnail;

        @BindView(R.id.trailer_type)
        TextView trailerType;
        @BindView(R.id.trailer_title)
        TextView trailerTitle;

        public TrailerViewHolder(final Context context, final View itemView) {
            super(itemView);
            this.context = context;
            ButterKnife.bind(this, itemView);

            trailerThumbnail.setOnClickListener(this);
        }

        public void bind(final Trailer trailer) {
            trailerTitle.setText(trailer.getName());
            trailerType.setText(trailer.getType());
            Glide.with(context).load(trailer.getThumbnailLink())
                    .priority(Priority.HIGH)
                    .into(trailerThumbnail);
        }

        @Override
        public void onClick(final View view) {
            if (trailers == null) {
                Log.wtf(LOG_TAG, "OnClick handler call with empty trailers list.");
            } else {
                final int position = getAdapterPosition();
                final Trailer trailer = trailers[position];
                listener.onTrailerItemClick(trailer);
            }
        }
    }
}
