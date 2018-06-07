package com.lbest.rm.data;

/**
 * @author wuxh
 * @Package: cn.com.broadlink.data
 * @Description: 更新信息类
 * @date 2017-05-13.
 */

public class UpdateInfo {

    private int version;
    private String versionName;
    private String updates;
    private String url;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getUpdates() {
        return updates;
    }

    public void setUpdates(String updates) {
        this.updates = updates;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
