package com.lbest.rm.data;


import cn.com.broadlink.sdk.result.account.BLBaseResult;

/**
 * Created by dell on 2018/4/16.
 */

public class RefreshTokenResult{
    private boolean isSuccess=false;
    private BLBaseResult result;
    private int code=0;//0:成功 -1登录失败   -2 token為null  -3刷新token失败

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public BLBaseResult getResult() {
        return result;
    }

    public void setResult(BLBaseResult result) {
        this.result = result;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
