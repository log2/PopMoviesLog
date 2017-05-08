package com.example.log2.popmovies.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * The model of themoviedb.org's response for the popular movies and the top rated movies
 * queries. For the simplicity it only contains the fields those are interesting for us.
 */
public final class MovieListResponse {

    @SerializedName("results")
    public List<Movie> movies;

    @SerializedName("status_message")
    public String statusMessage;

    @SerializedName("status_code")
    public String statusCode;
}
