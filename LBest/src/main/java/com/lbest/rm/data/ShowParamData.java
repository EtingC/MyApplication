package com.lbest.rm.data;

import java.util.ArrayList;

/**
 * Created by dell on 2017/12/8.
 */

public class ShowParamData {
    private String pid;
    private ArrayList<ParamData> param_list;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public ArrayList<ParamData> getParam_list() {
        return param_list;
    }

    public void setParam_list(ArrayList<ParamData> param_list) {
        this.param_list = param_list;
    }

    public static class ParamData{
        public String param;
        public String name;
        public String show_condition_val;
        public int show_grade;
        public String judge_way;
        public String util;
    }
}
