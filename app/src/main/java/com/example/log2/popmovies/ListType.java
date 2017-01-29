package com.example.log2.popmovies;

/**
 * Created by gallucci on 29/01/2017.
 */
public enum ListType {
    POPULAR("popular"), TOP_RATED("top_rated");

    private String urlFragment;

    ListType(String urlFragment) {
        this.urlFragment = urlFragment;
    }

    public String getUrlFragment() {
        return urlFragment;
    }

}
