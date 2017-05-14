package com.example.log2.popmovies.helpers;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

/**
 * Created by gallucci on 14/05/2017.
 */

public class SignallingUtils {
    public static void alert(Context context, View view, @StringRes int id) {
        if (view == null)
            Toast.makeText(context, id, Toast.LENGTH_LONG).show();
        else
            Snackbar.make(view, id, Snackbar.LENGTH_LONG).show();
    }

    public static void alert(Context context, View view, String s) {
        if (view == null)
            Toast.makeText(context, s, Toast.LENGTH_LONG).show();
        else
            Snackbar.make(view, s, Snackbar.LENGTH_LONG).show();
    }

    public static MessageTracker showAndTrack(final Context context, final View view, final String s) {
        if (view == null)
            return new MessageTracker() {
                private Toast toast = Toast.makeText(context, s, Toast.LENGTH_LONG);

                @Override
                public Runnable show() {
                    return new Runnable() {
                        @Override
                        public void run() {
                            toast.show();
                        }
                    };
                }

                @Override
                public Runnable hide() {
                    return new Runnable() {
                        @Override
                        public void run() {
                            if (toast != null)
                                toast.cancel();
                        }
                    };
                }
            };
        return new MessageTracker() {
            private Snackbar snackbar = Snackbar.make(view, s, Snackbar.LENGTH_LONG);

            @Override
            public Runnable show() {
                return new Runnable() {
                    @Override
                    public void run() {
                        snackbar.show();
                    }
                };
            }

            @Override
            public Runnable hide() {
                return new Runnable() {
                    @Override
                    public void run() {
                        if (snackbar != null)
                            snackbar.dismiss();
                    }
                };
            }
        };
    }
}
