package com.lbest.rm.data;

import cn.com.broadlink.sdk.result.account.BLBaseResult;

/**
 * Created by dell on 2017/12/6.
 */

public class BLSubmitPicResult extends BLBaseResult{
    private String picpath;

    public String getPicpath() {
        return picpath;
    }

    public void setPicpath(String picpath) {
        this.picpath = picpath;
    }
}
