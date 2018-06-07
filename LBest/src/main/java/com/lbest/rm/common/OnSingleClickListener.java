package com.lbest.rm.common;

import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dell on 2017/11/29.
 */

public abstract class OnSingleClickListener implements View.OnClickListener {

    private boolean mEnable = true;

    private static final int mDelay = 500;

    @Override
    public void onClick(View v) {
        if (mEnable) {
            mEnable = false;
            doOnClick(v);
            new Timer().schedule(new TimerTask() {

                @Override
                public void run() {
                    mEnable = true;
                }
            }, mDelay);
        }
    }

    public abstract void doOnClick(View v);
}