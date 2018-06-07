package com.lbest.rm.common;

import android.content.Context;

import com.lbest.rm.BuildConfig;
import com.lbest.rm.account.broadlink.BLHttpErrCode;
import com.lbest.rm.data.RequestTimestampResult;
import com.lbest.rm.utils.http.HttpGetAccessor;

/**
 * Created by dell on 2017/11/30.
 */

public class BLFamilyTimestampPresenter {
    private static final int TIMESTAMP_INDATE = 2 * 60 * 60 * 1000;
    private static String FAMILY_REQUEST_TIMESTAMP="http://"+BuildConfig.ChannelID+"bizihcv0.ibroadlink.com" + "/ec4/v1/common/api";
    private static RequestTimestampResult mCacheTimestamp;
    private static long mCacheTime;

    public static RequestTimestampResult getTimestamp(Context context){

        if(mCacheTimestamp == null || System.currentTimeMillis() - mCacheTime >= TIMESTAMP_INDATE){
            HttpGetAccessor getAccessor = new HttpGetAccessor();
            RequestTimestampResult timestampResult = getAccessor.execute(FAMILY_REQUEST_TIMESTAMP, null, RequestTimestampResult.class);
            if(timestampResult != null && timestampResult.getError() == BLHttpErrCode.SUCCESS){
                mCacheTime = System.currentTimeMillis();
                mCacheTimestamp = timestampResult;
            }
        }

        return  mCacheTimestamp;
    }
}
