package com.lbest.rm.plugin;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.lbest.rm.DNAH5Activity;
import com.lbest.rm.plugin.data.NativeTitleInfo;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by YeJn on 2016/6/24.
 */
public class Navbar extends CordovaPlugin {

    private final String ACT_CUSTOM = "custom";

    @Override
    public boolean execute(String action, JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
        Log.i("Navbar", "action:" + action);
        if(jsonArray != null) Log.i("Navbar", "jsonArray:" + jsonArray.toString());

        if (action.equals(ACT_CUSTOM)) {
            paserBtnHandler(jsonArray);
            callbackContext.success();
        }

        return true;
    }

    private void paserBtnHandler(JSONArray jsonArray) {
        DNAH5Activity mActivity = (DNAH5Activity) cordova.getActivity();
        NativeTitleInfo cordovaBtnHandler = null;
        try {
            if (jsonArray != null && jsonArray.length() > 0) {
                JSONObject handlerJsonObject = jsonArray.optJSONObject(0);
                if(handlerJsonObject != null){
                    String handlerJson = handlerJsonObject.toString();
                    Log.i("Navbar btn handlerJson", handlerJson);
                    cordovaBtnHandler  = JSON.parseObject(handlerJson, NativeTitleInfo.class);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            mActivity.pushHander(cordovaBtnHandler);
        }
    }

}
