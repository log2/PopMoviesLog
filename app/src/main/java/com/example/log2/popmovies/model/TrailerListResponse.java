package com.example.log2.popmovies.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class TrailerListResponse {

    @SerializedName("results")
    public List<Trailer> trailers;
}

