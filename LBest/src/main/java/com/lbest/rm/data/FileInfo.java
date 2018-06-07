package com.lbest.rm.data;

/**
 * Created by dell on 2017/11/30.
 */

public class FileInfo {
    private String userid;
    private String type;
    private String subtype;
    private String url;

    public FileInfo() {
        this.type = "default";
        this.subtype = "default";
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
