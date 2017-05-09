package com.example.log2.popmovies.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by gallucci on 07/05/2017.
 */

public class FormattingUtils {
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd MMM yyyy");

    public static String formatReleaseDate(final Date date) {
        return DATE_FORMATTER.format(date);
    }
}
