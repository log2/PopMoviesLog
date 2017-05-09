package com.example.log2.popmovies.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public final class Trailer implements Parcelable {


    public static final Creator<Trailer> CREATOR = new Creator<Trailer>() {
        @Override
        public Trailer createFromParcel(final Parcel in) {
            return new Trailer(in);
        }

        @Override
        public Trailer[] newArray(final int size) {
            return new Trailer[size];
        }
    };
    private static final String YOUTUBE_VIDEO_BASE_URL = "https://www.youtube.com/watch?v=";
    private static final String YOUTUBE_THUMBNAIL_URL_PATTERN =
            "https://img.youtube.com/vi/%s/mqdefault.jpg";
    @SerializedName("name")
    private String mName;
    @SerializedName("key")
    private String mId;
    @SerializedName("type")
    private String mType;


    public Trailer(final Parcel in) {
        mName = in.readString();
        mId = in.readString();
        mType = in.readString();
    }

    public String getName() {
        return mName;
    }

    public String getLink() {
        return YOUTUBE_VIDEO_BASE_URL + mId;
    }

    public String getThumbnailLink() {
        return String.format(YOUTUBE_THUMBNAIL_URL_PATTERN, mId);
    }

    public String getType() {
        return mType;
    }


    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(mName);
        dest.writeString(mId);
        dest.writeString(mType);
    }


    @Override
    public int describeContents() {
        // Nothing special here (no FD, etc.)
        return 0;
    }
}

