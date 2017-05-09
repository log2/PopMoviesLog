package com.example.log2.popmovies.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.log2.popmovies.helpers.FormattingUtils;
import com.google.gson.annotations.SerializedName;

import java.util.Date;


public final class Movie implements Parcelable {

    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(final Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(final int size) {
            return new Movie[size];
        }
    };
    @SerializedName("vote_average")
    public double userRating;
    @SerializedName("id")
    public String id;
    @SerializedName("title")
    public String title;
    @SerializedName("original_title")
    public String originalTitle;
    @SerializedName("overview")
    public String synopsis;
    @SerializedName("release_date")
    public Date releaseDate;
    @SerializedName("poster_path")
    public String posterPath;


    public Movie() {
    }

    public Movie(final Parcel in) {
        id = in.readString();
        title = in.readString();
        originalTitle = in.readString();
        synopsis = in.readString();
        userRating = in.readDouble();
        releaseDate = new Date(in.readLong());
        posterPath = in.readString();
    }

    public float ratingAcross(float numStars) {
        return (float) ((numStars * userRating) / 10);
    }

    public String getReadableSynopsis()

    {
        return synopsis + "\n";
    }

    public String getReleaseDateHR() {
        return FormattingUtils.formatReleaseDate(releaseDate);
    }


    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(originalTitle);
        dest.writeString(synopsis);
        dest.writeDouble(userRating);
        dest.writeLong(releaseDate.getTime());
        dest.writeString(posterPath);
    }

    @Override
    public int describeContents() {
        // Nothing special here (no FD, etc.)
        return 0;
    }
}
