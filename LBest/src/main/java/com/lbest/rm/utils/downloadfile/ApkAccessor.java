package com.lbest.rm.utils.downloadfile;

import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;

import android.content.Context;
import android.widget.Toast;

import com.lbest.rm.R;

/**
 * Apk下载类
 */
public class ApkAccessor extends DownloadAccessor {

    private boolean mToastError = true;

    /**
     * 构造函数
     * 
     * @param context
     */
    public ApkAccessor(Context context) {
        super(context);
    }

    /**
     * 异常处理
     * 
     * @param e
     *            异常
     */
    @Override
    protected void onException(Exception e) {
        super.onException(e);

        if (!mToastError)
            return;

        final int msgId;
        if (e instanceof SocketException
                || e instanceof InterruptedIOException || e instanceof UnknownHostException
                || e instanceof UnknownServiceException) {
            // 网络错误
            msgId = R.string.err_network;
        } else {
            // 系统错误
            msgId = R.string.err_system;
        }

        mHandler.post(new Runnable() {

            @Override
            public void run() {
                // 提示错误
                Toast.makeText(mContext, msgId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean isToastError() {
        return mToastError;
    }

    public void setToastError(boolean toastError) {
        this.mToastError = toastError;
    }
}