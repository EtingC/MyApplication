package com.lbest.rm.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dell on 2017/11/8.
 */

public class ParamData {
    public final static int TYPE_Enum=1;
    public final static int TYPE_Continuous=2;
    public final static String ACT_READ="1";
    public final static String ACT_WRITE="2";
    public final static String ACT_READWRITE="3";
    private String key;
    private String name;
    private String act;//1: read only 2: write only 3:read & write
    private int type;//参数类型 1表示枚举型  2表示连续型
    private List<Enum> value_range1;
    private Continuous value_range2;
    private String unit;
    private int mulriple;
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<Enum> getValue_range1() {
        return value_range1;
    }

    public void setValue_range1(List<Enum> value_range1) {
        this.value_range1 = value_range1;
    }

    public Continuous getValue_range2() {
        return value_range2;
    }

    public void setValue_range2(Continuous value_range2) {
        this.value_range2 = value_range2;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getMulriple() {
        return mulriple;
    }

    public void setMulriple(int mulriple) {
        this.mulriple = mulriple;
    }

    public String getAct() {
        return act;
    }

    public void setAct(String act) {
        this.act = act;
    }

    public static class Enum{
        public String value;
        public String correspond_value;
        public String name;
    }

    public static class Continuous{
        public int  min;
        public int  max;
        public int  step;
        public int  mulriple;
    }
}
