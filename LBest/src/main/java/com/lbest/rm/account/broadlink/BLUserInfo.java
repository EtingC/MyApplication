package com.lbest.rm.account.broadlink;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by dell on 2017/10/20.
 */

public class BLUserInfo {
    /***
     * 用户信息保存的文件
     ***/
    private String refresh_token;
    private String access_token;
    private String expires_in;
    private String bl_loginsession;
    private String bl_nickname;
    private String bl_userid;
    private String bl_icon;

    public BLUserInfo() {
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(String expires_in) {
        this.expires_in = expires_in;
    }

    public String getBl_loginsession() {
        return bl_loginsession;
    }

    public void setBl_loginsession(String bl_loginsession) {
        this.bl_loginsession = bl_loginsession;
    }

    public String getBl_nickname() {
        return bl_nickname;
    }

    public void setBl_nickname(String bl_nickname) {
        this.bl_nickname = bl_nickname;
    }

    public String getBl_userid() {
        return bl_userid;
    }

    public void setBl_userid(String bl_userid) {
        this.bl_userid = bl_userid;
    }

    public String getBl_icon() {
        return bl_icon;
    }

    public void setBl_icon(String bl_icon) {
        this.bl_icon = bl_icon;
    }

}
