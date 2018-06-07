package com.lbest.rm.common;

import android.graphics.Bitmap;

/**
 * Created by dell on 2017/11/2.
 */

public class BLImageBlur {
    public static Bitmap blur(Bitmap bkg, int radius) {
        return doBlurJniBitMap(bkg, (int) radius, true);
    }

    private static Bitmap doBlurJniBitMap(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {
        Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }

        if (radius < 1) {
            return (null);
        }
        // Jni BitMap
        net.qiujuer.imageblurring.jni.ImageBlur.blurBitMap(bitmap, radius);

        return (bitmap);
    }
}
