package com.lbest.rm.common;

import android.os.Environment;

import com.lbest.rm.BuildConfig;

/**
 * Created by dell on 2017/11/30.
 */

public class BLImageUrl {
    private static String BASE_FAMILY_URL = "http://"+ BuildConfig.ChannelID+"bizihcv0.ibroadlink.com";
    // 网络请求地址
    private static String getUrlBase(boolean isOffical) {
        String url_base = BASE_FAMILY_URL + "/ec4/v1/userspace/%s";
        if (isOffical) {
            url_base = String.format(url_base, "openlimit");
        } else {
            url_base = String.format(url_base, "ownerlimit");
        }
        return url_base;
    }

    private static String getUrlMtag(boolean isGetImgFile) {
        return isGetImgFile ? "imagelib" : "imagelibinfo";
    }

    public static String getAddImgFileUrl(boolean isOffical) {
        return getUrlBase(isOffical) + "/uploadfile?mtag=" + getUrlMtag(true);
    }

    public static String getAddImgJsonUrl(boolean isOffical) {
        return getUrlBase(isOffical) + "/uploadjson?mtag=" + getUrlMtag(false);
    }

    public static String getDelImgFileUrlByKey(boolean isOffical, String key) {
        return getUrlBase(isOffical) + "/delete?mtag=" + getUrlMtag(true) + "&mkey=" + key;
    }

    public static String getDelImgJsonUrlByKey(boolean isOffical, String key) {
        return getUrlBase(isOffical) + "/delete?mtag=" + getUrlMtag(false) + "&mkey=" + key;
    }

    public static String getFindImgFileUrlByKey(boolean isOffical, String key) {
        return getUrlBase(isOffical) + "/queryfile?mtag=" + getUrlMtag(true) + "&mkey=" + key;
    }

    public static String getFindImgJsonUrl(boolean isOffical) {
        return getUrlBase(isOffical) + "/querybyfilter?mtag=" + getUrlMtag(false);
    }
}
