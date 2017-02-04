package com.example.log2.popmovies;

import android.os.Handler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gallucci on 04/02/2017.
 */

public class DelayedWarning {
    private final AtomicBoolean hidden = new AtomicBoolean(false);

    public DelayedWarning(final Runnable delayShowAction) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!hidden.get()) delayShowAction.run();
            }
        }, 150);
    }

    public void hide(Runnable delayHideAction) {
        delayHideAction.run();
        hidden.set(true);
    }
}
