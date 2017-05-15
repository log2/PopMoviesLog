package com.example.log2.popmovies.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

@SuppressWarnings({"CanBeFinal", "unused"})
public final class MovieListResponse {

    @SerializedName("results")
    public List<Movie> movies;

    @SerializedName("status_message")
    public String statusMessage;

    @SerializedName("status_code")
    public String statusCode;
}
