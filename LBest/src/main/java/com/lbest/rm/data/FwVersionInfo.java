package com.lbest.rm.data;

import java.io.Serializable;

/**
 * Created by dell on 2017/11/29.
 */

public class FwVersionInfo implements Serializable {

    private static final long serialVersionUID = 6164977007657968515L;

    private String version;

    private String url;

    private String date;

    private UpdateContent changelog;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public UpdateContent getChangelog() {
        return changelog;
    }

    public void setChangelog(UpdateContent changelog) {
        this.changelog = changelog;
    }

}