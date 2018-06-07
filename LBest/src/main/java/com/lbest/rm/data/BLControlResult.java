package com.lbest.rm.data;

import java.util.ArrayList;

/**
 * Created by dell on 2017/11/3.
 */

public class BLControlResult {
    private int status;
    private String msg;
    private Data data;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data{
        public ArrayList<String> params=new ArrayList<>();
        public ArrayList<ArrayList<BLValueData>> vals=new ArrayList<>();
    }
}
