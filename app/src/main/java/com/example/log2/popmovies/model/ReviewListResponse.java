package com.example.log2.popmovies.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;


@SuppressWarnings({"CanBeFinal", "unused"})
public final class ReviewListResponse {

    @SerializedName("results")
    public List<Review> reviews;
}

