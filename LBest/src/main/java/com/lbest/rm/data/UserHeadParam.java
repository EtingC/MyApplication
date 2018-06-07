package com.lbest.rm.data;

import com.lbest.rm.utils.EncryptUtils;

import cn.com.broadlink.sdk.BLLet;

/**
 * Created by dell on 2017/11/29.
 */

public class UserHeadParam extends BaseHeadParam {

    public UserHeadParam() {
    }

    public UserHeadParam(String timestamp, String token) {
        super(timestamp, token);
    }

    private String loginsession;

    private String familyid;

    private String sign;

    public String getLoginsession() {
        return loginsession;
    }

    public void setLoginsession(String loginsession) {
        this.loginsession = loginsession;
    }


    public String getFamilyid() {
        return familyid;
    }

    public void setFamilyid(String familyid) {
        this.familyid = familyid;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public void setSign(String url, String keys) {
        this.sign = EncryptUtils.SHA1(url + this.getTimestamp() + (keys==null?"broadlinkappmanage@":keys) + BLLet.getLicenseId());
    }
}
