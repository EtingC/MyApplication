package com.lbest.rm.data;

import java.util.ArrayList;

/**
 * Created by dell on 2017/11/3.
 */

public class BLCMDData {
    private String did;
    private String act;
    private String srv;
    private ArrayList<String> params;
    private ArrayList<ArrayList<BLValueData>> vals;


    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getAct() {
        return act;
    }

    public void setAct(String act) {
        this.act = act;
    }

    public String getSrv() {
        return srv;
    }

    public void setSrv(String srv) {
        this.srv = srv;
    }

    public ArrayList<String> getParams() {
        return params;
    }

    public void setParams(ArrayList<String> params) {
        this.params = params;
    }

    public ArrayList<ArrayList<BLValueData>> getVals() {
        return vals;
    }

    public void setVals(ArrayList<ArrayList<BLValueData>> vals) {
        this.vals = vals;
    }
}
