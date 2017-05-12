package com.example.log2.popmovies.helpers;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gallucci on 04/02/2017.
 */

public class DelayedWarning {
    private final AtomicBoolean hidden = new AtomicBoolean(false);
    private final Runnable delayHideAction;

    public DelayedWarning(final Runnable delayShowAction, Runnable delayHideAction) {
        this.delayHideAction = delayHideAction;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!hidden.get()) delayShowAction.run();
            }
        }, 150);
    }

    @NonNull
    public static DelayedWarning showingTemporarily(final View view) {
        return new DelayedWarning(new Runnable() {
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

    public void hide() {
        delayHideAction.run();
        hidden.set(true);
    }
}
