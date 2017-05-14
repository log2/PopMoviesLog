package com.example.log2.popmovies.helpers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gallucci on 04/02/2017.
 */

public class ChokeTracker {
    private final AtomicBoolean hidden = new AtomicBoolean(false);
    private final Runnable delayShowAction;
    private final Runnable delayHideAction;

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

    public static ChokeTracker showingSnackbar(Context context, View viewforSnackbar, final String s) {
        MessageTracker messageTracker = SignallingUtils.showAndTrack(context, viewforSnackbar, s);
        return new ChokeTracker(messageTracker.show(), messageTracker.hide());
    }

    public void signalChoke() {
        if (!hidden.get()) delayShowAction.run();
    }

    public void hide() {
        delayHideAction.run();
        hidden.set(true);
    }
}
