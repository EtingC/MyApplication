package com.lbest.rm.data;

import java.util.ArrayList;

/**
 * Created by dell on 2017/12/9.
 */

public class OtaQueryResponseData {
    private ArrayList<versionInfo> versions;
    public static class versionInfo{
        public String version;
        public String url;
        public String date;
        public ChangeLog changelog;
        public static class ChangeLog{
            public String cn;
            public String en;
        }
    }

    public ArrayList<versionInfo> getVersions() {
        return versions;
    }

    public void setVersions(ArrayList<versionInfo> versions) {
        this.versions = versions;
    }
}
