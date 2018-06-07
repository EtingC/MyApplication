package com.lbest.rm.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dell on 2017/11/3.
 */

public class AliDeviceStatusResult {
    private String ErrorCode;
    private Map<String,String> status=new HashMap<>();
    private String onlineState;
    private String onlineState_when;
    private String  uuid;

    public String getErrorCode() {
        return ErrorCode;
    }

    public void setErrorCode(String errorCode) {
        ErrorCode = errorCode;
    }

    public Map<String, String> getStatus() {
        return status;
    }

    public void setStatus(Map<String, String> status) {
        this.status = status;
    }

    public String getOnlineState() {
        return onlineState;
    }

    public void setOnlineState(String onlineState) {
        this.onlineState = onlineState;
    }

    public String getOnlineState_when() {
        return onlineState_when;
    }

    public void setOnlineState_when(String onlineState_when) {
        this.onlineState_when = onlineState_when;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
