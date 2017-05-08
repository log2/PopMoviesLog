package com.example.log2.popmovies.detail;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
    private Trailer[] mTrailers;
    private TrailersAdapter.OnClickListener mListener;

    public TrailersAdapter(final TrailersAdapter.OnClickListener listener) {
        mTrailers = null;
        mListener = listener;
    }

    @Override
    public TrailerViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final Context context = parent.getContext();

        final LayoutInflater inflater = LayoutInflater.from(context);

        final boolean shouldAttachToParentImmediately = false;

        final View view = inflater.inflate(R.layout.item_trailer, parent,
                shouldAttachToParentImmediately);

        final TrailerViewHolder viewHolder = new TrailerViewHolder(context, view);

        return viewHolder;
    }

    public Trailer[] getTrailers() {
        return mTrailers;
    }

    public void setTrailers(final Trailer[] reviews) {
        this.mTrailers = reviews;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final TrailerViewHolder holder, final int position) {
        final Trailer trailer = mTrailers[position];
        holder.bind(trailer);
    }

    @Override
    public int getItemCount() {
        if (null == mTrailers) return 0;
        return mTrailers.length;
    }

    public interface OnClickListener {
        void onTrailerItemClick(Trailer trailer);
    }

    /**
     * View holder for trailer items in the RecyclerView.
     */
    public class TrailerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final Context mContext;

        @BindView(R.id.image_trailer_thumbnail)
        ImageView mTrailerThumbnail;

        public TrailerViewHolder(final Context context, final View itemView) {
            super(itemView);

            mContext = context;
            ButterKnife.bind(this, itemView);
            mTrailerThumbnail.setOnClickListener(this);
        }

        public void bind(final Trailer trailer) {
            final String thumbnailLinkToBind = trailer.getThumbnailLink();
            Glide.with(mContext).load(thumbnailLinkToBind)
                    .priority(Priority.IMMEDIATE)
                    .into(mTrailerThumbnail);
        }

        @Override
        public void onClick(final View view) {
            if (mTrailers == null) {
                Log.wtf(LOG_TAG, "OnClick handler call with empty trailers list.");
            } else {
                final int position = getAdapterPosition();
                final Trailer trailer = mTrailers[position];
                mListener.onTrailerItemClick(trailer);
            }
        }
    }
}
