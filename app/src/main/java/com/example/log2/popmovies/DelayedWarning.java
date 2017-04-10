package com.example.log2.popmovies;

import android.os.Handler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gallucci on 04/02/2017.
 */

class DelayedWarning {
    private final AtomicBoolean hidden = new AtomicBoolean(false);
    private Runnable delayHideAction;

    public DelayedWarning(final Runnable delayShowAction) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!hidden.get()) delayShowAction.run();
            }
        }, 150);
    }


    public DelayedWarning(final Runnable delayShowAction, Runnable delayHideAction) {
        this.delayHideAction = delayHideAction;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!hidden.get()) delayShowAction.run();
            }
        }, 150);
    }

    public void hide() {
        hide(delayHideAction == null ? new Runnable() {
            @Override
            public void run() {
                // Do nothing
            }
        } : delayHideAction);
    }

    public void hide(Runnable delayHideAction) {
        delayHideAction.run();
        hidden.set(true);
    }
}
