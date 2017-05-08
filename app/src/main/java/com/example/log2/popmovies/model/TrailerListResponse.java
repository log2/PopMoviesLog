package com.example.log2.popmovies.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * The model of themoviedb.org's response for the /movie/{id}/trailers query.
 * For the simplicity it only contains the fields those are interesting for us.
 */
public final class TrailerListResponse {

    @SerializedName("results")
    public List<Trailer> trailers;
}

