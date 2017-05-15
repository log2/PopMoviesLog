package com.example.log2.popmovies.data;

/**
 * Created by gallucci on 29/01/2017.
 */
public enum ListType {
    POPULAR("popular"), TOP_RATED("top_rated"), FAVORITES();

    private final String externalName;
    private final boolean providedByTMDB;

    ListType() {
        providedByTMDB = false;
        externalName = null;
    }

    ListType(String externalName) {
        this.externalName = externalName;
        providedByTMDB = true;
    }

    public String getExternalName() {
        return externalName;
    }

    public boolean isProvidedByTMDB() {
        return providedByTMDB;
    }
}
