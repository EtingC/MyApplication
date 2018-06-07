package com.lbest.rm.data;

import com.lbest.rm.BuildConfig;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.utils.CommonUtils;

import cn.com.broadlink.sdk.BLLet;

/**
 * Created by dell on 2017/11/29.
 */

public class BaseHeadParam {
    private String mobileinfo;

    private String userid;

    private String timestamp;

    private String token;

    private String language;

    private String system = "android";

    private String appPlatform = "android";

    private String appVersion = BuildConfig.VERSION_NAME;

    private String locate;

    private String licenseid;

    public BaseHeadParam() {
        initBaseHeadParam();
    }

    public BaseHeadParam(String timestamp, String token) {
        this.timestamp = timestamp;
        this.token = token;
        initBaseHeadParam();
    }

    private void initBaseHeadParam(){
        this.locate = CommonUtils.getCountry();
        this.language = CommonUtils.getLanguage();
        this.userid = BLAcountToAli.getInstance().getBlUserInfo().getBl_userid();
        this.mobileinfo = android.os.Build.MODEL;
        this.licenseid = BLLet.getLicenseId();
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getLocate() {
        return locate;
    }

    public void setLocate(String locate) {
        this.locate = locate;
    }

    public String getLicenseid() {
        return licenseid;
    }

    public void setLicenseid(String licenseid) {
        this.licenseid = licenseid;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getAppPlatform() {
        return appPlatform;
    }

    public void setAppPlatform(String appPlatform) {
        this.appPlatform = appPlatform;
    }

    public String getMobileinfo() {
        return mobileinfo;
    }

    public void setMobileinfo(String mobileinfo) {
        this.mobileinfo = mobileinfo;
    }
}
