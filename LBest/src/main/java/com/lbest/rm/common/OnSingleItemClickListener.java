package com.lbest.rm.common;

import android.view.View;
import android.widget.AdapterView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dell on 2017/11/29.
 */

public abstract class OnSingleItemClickListener implements AdapterView.OnItemClickListener {

    private boolean mEnable = true;
    private static final int mDelay = 500;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mEnable) {
            mEnable = false;
            doOnClick(parent, view, position, id);
            new Timer().schedule(new TimerTask() {

                @Override
                public void run() {
                    mEnable = true;
                }
            }, mDelay);
        }
    }

    public abstract void doOnClick(AdapterView<?> parent, View view, int position, long id);
}
