package com.example.log2.popmovies.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("CanBeFinal")
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
    @SuppressWarnings("HardCodedStringLiteral")
    private static final String YOUTUBE_VIDEO_PATTERN = "https://www.youtube.com/watch?v=%s";
    @SuppressWarnings("HardCodedStringLiteral")
    private static final String YOUTUBE_THUMBNAIL_URL_PATTERN =
            "https://img.youtube.com/vi/%s/mqdefault.jpg";
    @SerializedName("name")
    private String name;
    @SerializedName("key")
    private String id;
    @SerializedName("type")
    private String type;


    public Trailer(final Parcel in) {
        name = in.readString();
        id = in.readString();
        type = in.readString();
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return String.format(YOUTUBE_VIDEO_PATTERN, id);
    }

    public String getThumbnailLink() {
        return String.format(YOUTUBE_THUMBNAIL_URL_PATTERN, id);
    }

    public String getType() {
        return type;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(name);
        dest.writeString(id);
        dest.writeString(type);
    }


    @Override
    public int describeContents() {
        // Nothing special here (no FD, etc.)
        return 0;
    }
}

