package com.example.log2.popmovies.helpers;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gallucci on 04/02/2017.
 */

public class ChokeTracker {
    private final AtomicBoolean hidden = new AtomicBoolean(false);
    private final Runnable delayShowAction;
    private Runnable delayHideAction;

    public ChokeTracker(final Runnable delayShowAction, Runnable delayHideAction) {
        this.delayHideAction = delayHideAction;
        this.delayShowAction = delayShowAction;
    }

    @NonNull
    public static ChokeTracker showingTemporarily(final View view) {
        return new ChokeTracker(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.VISIBLE);
            }
        }, new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.INVISIBLE);
            }
        });
    }

    public static ChokeTracker showingSnackbar(final View viewforSnackbar, final String s) {
        class SnackbarTracker {
            private Snackbar snackbar;

            Runnable show() {
                return new Runnable() {
                    @Override
                    public void run() {
                        snackbar = Snackbar.make(viewforSnackbar, s, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                };
            }

            Runnable hide() {
                return new Runnable() {
                    @Override
                    public void run() {
                        if (snackbar != null)
                            snackbar.dismiss();
                    }
                };
            }
        }
        SnackbarTracker snackbarTracker = new SnackbarTracker();
        return new ChokeTracker(snackbarTracker.show(), snackbarTracker.hide());
    }

    public void signalChoke() {
        if (!hidden.get()) delayShowAction.run();
    }

    public void hide() {
        delayHideAction.run();
        hidden.set(true);
    }
}
