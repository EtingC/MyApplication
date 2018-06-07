package com.lbest.rm.data;

import com.lbest.rm.account.broadlink.BLHttpErrCode;

/**
 * Created by dell on 2017/11/30.
 */

public class BaseResult {
    private int error = -1;

    private String msg;

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isSuccess(){
        return error == BLHttpErrCode.SUCCESS;
    }
}
