package com.lbest.rm.data;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by dell on 2017/10/31.
 */

public class TimeScene extends SceneModel{
    public final static int FIRSTACTION=1;
    public final static int SECONDACTION=2;

    public final static String SCENEWEEK_1="2";
    public final static String SCENEWEEK_2="3";
    public final static String SCENEWEEK_3="4";
    public final static String SCENEWEEK_4="5";
    public final static String SCENEWEEK_5="6";
    public final static String SCENEWEEK_6="7";
    public final static String SCENEWEEK_7="1";


    JSONObject jsonValues;

    public TimeScene(){
        super();
    }

    public TimeScene(String templateId) {
        super(templateId);
    }

    public JSONObject getJsonValues() {
        return jsonValues;
    }

    public void setJsonValues(JSONObject jsonValues) {
        this.jsonValues = jsonValues;
    }
}
