package com.lbest.rm.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.broadlink.lib.imageloader.cache.disc.naming.Md5FileNameGenerator;
import com.broadlink.lib.imageloader.core.DisplayImageOptions;
import com.broadlink.lib.imageloader.core.ImageLoader;
import com.broadlink.lib.imageloader.core.ImageLoaderConfiguration;
import com.broadlink.lib.imageloader.core.assist.QueueProcessingType;
import com.broadlink.lib.imageloader.core.listener.ImageLoadingListener;
import com.broadlink.lib.imageloader.core.listener.ImageLoadingProgressListener;
import com.broadlink.lib.imageloader.utils.DiskCacheUtils;
import com.broadlink.lib.imageloader.utils.MemoryCacheUtils;

/**
 * Created by dell on 2017/10/25.
 */

public class ImageLoaderUtils {

    private static ImageLoaderUtils mBitmapUtils;
    private static ImageLoader mImageLoader;

    private static DisplayImageOptions mOptions;

    private ImageLoaderUtils(Context context){
        mImageLoader = ImageLoader.getInstance();
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(70 * 1024 * 1024); //
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        config.memoryCacheSize(3 * 1024 * 1024);//内存缓存
//         config.writeDebugLogs(); // Remove for release app
        mImageLoader.init(config.build());

        mOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    public static ImageLoaderUtils getInstence(Context context){
        if(mBitmapUtils == null){
            mBitmapUtils = new ImageLoaderUtils(context);
        }
        return mBitmapUtils;
    }

    /***
     * 异步加载图片
     * @param uri
     *          图片地址
     * @param imageView
     *          显示的ImageView
     * @param listener
     *          回调
     */
    public void displayImage(String uri, ImageView imageView, ImageLoadingListener listener) {
        if(uri != null){
            mImageLoader.displayImage(uri, imageView, mOptions, listener);
        }else if(listener != null){
            listener.onLoadingFailed(uri, imageView, null);
        }
    }


    /***
     * 异步加载图片
     * @param uri
     *          图片地址
     * @param imageView
     *          显示的ImageView
     * @param listener
     *          回调
     * @progressListener
     * 			下载进度
     */
    public void displayImage(String uri, ImageView imageView, ImageLoadingListener listener, ImageLoadingProgressListener progressListener) {
        mImageLoader.displayImage(uri, imageView, mOptions, listener, progressListener);
    }

    /**
     * 同步加载图片
     * @param uri
     *          图片地址
     * @return
     *         Bitmap
     */
    public Bitmap loadImageSync(String uri) {
        return mImageLoader.loadImageSync(uri, mOptions);
    }

    public void clearCache() {
        mImageLoader.clearMemoryCache();
        mImageLoader.clearDiskCache();
    }

    public void clearCache(String imagePath) {
        MemoryCacheUtils.removeFromCache(imagePath, mImageLoader.getMemoryCache());
        DiskCacheUtils.removeFromCache(imagePath, mImageLoader.getDiskCache());
    }
}
