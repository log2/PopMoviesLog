package com.example.log2.popmovies.data;

import android.net.Uri;
import android.provider.BaseColumns;

public class FavoriteContract {
    public static final String AUTHORITY = "com.example.log2.popmovies"; //NON-NLS

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY); //NON-NLS

    public static final String PATH_FAVORITES = "favorites"; //NON-NLS

    public static final class FavoriteEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVORITES).build();

        public static final String TABLE_NAME = "favorites"; //NON-NLS
        public static final String COLUMN_TMDB_ID = "tmdbId"; //NON-NLS
        public static final String COLUMN_TITLE = "title"; //NON-NLS
        public static final String COLUMN_TIMESTAMP = "timestamp"; //NON-NLS
    }

}
