package com.lbest.rm.data;

import java.util.List;

/**
 * Created by dell on 2017/11/30.
 */

public class HttpResponse extends BaseResult {

    /**
     * status : 0
     * total : 4
     * count : 4
     * data : [{"_id":"57beb157a98a70d764c02888","owner":"c64b5daea6308308307a7fe8bb475ddd","owngroup":"62f59d797c92771afc88548b30d8d775","shareuser":["c64b5daea6308308307a7fe8bb475ddd"],"subtype":"default","type":"offical-home","url":"http://ihcv0.ibroadlink.com/ec4/v1/userspace/openlimit/queryfile?mtag=imagelib&mkey=57beb157a98a70d764c02887","userid":"offical"},{"_id":"57beb170a98a70d764c0288a","owner":"c64b5daea6308308307a7fe8bb475ddd","owngroup":"62f59d797c92771afc88548b30d8d775","shareuser":["c64b5daea6308308307a7fe8bb475ddd"],"subtype":"default","type":"offical-home","url":"http://ihcv0.ibroadlink.com/ec4/v1/userspace/openlimit/queryfile?mtag=imagelib&mkey=57beb170a98a70d764c02889","userid":"offical"},{"_id":"57beb18da98a70d764c0288c","owner":"c64b5daea6308308307a7fe8bb475ddd","owngroup":"62f59d797c92771afc88548b30d8d775","shareuser":["c64b5daea6308308307a7fe8bb475ddd"],"subtype":"default","type":"offical-home","url":"http://ihcv0.ibroadlink.com/ec4/v1/userspace/openlimit/queryfile?mtag=imagelib&mkey=57beb18ca98a70d764c0288b","userid":"offical"},{"_id":"57beb1a1a98a70d764c0288e","owner":"c64b5daea6308308307a7fe8bb475ddd","owngroup":"62f59d797c92771afc88548b30d8d775","shareuser":["c64b5daea6308308307a7fe8bb475ddd"],"subtype":"default","type":"offical-home","url":"http://ihcv0.ibroadlink.com/ec4/v1/userspace/openlimit/queryfile?mtag=imagelib&mkey=57beb1a0a98a70d764c0288d","userid":"offical"}]
     */
    private String id;
    private int status;
    private int total;
    private int count;
    private List<DataBean> data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.setError(status);
        this.status = status;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<DataBean> getData() {
        return data;
    }

    public void setData(List<DataBean> data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * _id : 57beb157a98a70d764c02888
         * owner : c64b5daea6308308307a7fe8bb475ddd
         * owngroup : 62f59d797c92771afc88548b30d8d775
         * shareuser : ["c64b5daea6308308307a7fe8bb475ddd"]
         * subtype : default
         * type : offical-home
         * url : http://ihcv0.ibroadlink.com/ec4/v1/userspace/openlimit/queryfile?mtag=imagelib&mkey=57beb157a98a70d764c02887
         * userid : offical
         */

        private String id;
        private String imgId;
        private String owner;
        private String owngroup;
        private String subtype;
        private String type;
        private String url;
        private String userid;
        private List<String> shareuser;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getImgId() {
            if (imgId == null && url != null) {
                int position = url.indexOf("mkey=");
                imgId = url.substring(position + 5);
            }
            return imgId;
        }

        public void setImgId(String imgId) {
            this.imgId = imgId;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getOwngroup() {
            return owngroup;
        }

        public void setOwngroup(String owngroup) {
            this.owngroup = owngroup;
        }

        public String getSubtype() {
            return subtype;
        }

        public void setSubtype(String subtype) {
            this.subtype = subtype;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUserid() {
            return userid;
        }

        public void setUserid(String userid) {
            this.userid = userid;
        }

        public List<String> getShareuser() {
            return shareuser;
        }

        public void setShareuser(List<String> shareuser) {
            this.shareuser = shareuser;
        }
    }
}