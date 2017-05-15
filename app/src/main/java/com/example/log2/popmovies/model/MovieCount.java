package com.example.log2.popmovies.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by gallucci on 07/05/2017.
 */

@SuppressWarnings({"CanBeFinal", "unused"})
public class MovieCount {
    @SerializedName("total_results")
    public int count;
}
