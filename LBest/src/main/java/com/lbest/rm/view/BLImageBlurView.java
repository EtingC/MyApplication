package com.lbest.rm.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.lbest.rm.common.BLImageBlur;

/**
 * Created by dell on 2017/11/2.
 */

public class BLImageBlurView extends ImageView {

    public BLImageBlurView(Context context) {
        super(context);
    }

    public BLImageBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BLImageBlurView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void blurImage(final int blurValue) {
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                buildDrawingCache();

                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        Bitmap bmp = getDrawingCache();
                        Bitmap bitmap = BLImageBlur.blur(bmp, blurValue);
                        Message msg =  new Message();
                        msg.obj = bitmap;
                        myHandler.sendMessage(msg);
                    }
                }).start();
                return true;
            }
        });

    }

    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            Bitmap bitmap = (Bitmap) msg.obj;
            setImageBitmap(bitmap);
            super.handleMessage(msg);
        }
    };
}
