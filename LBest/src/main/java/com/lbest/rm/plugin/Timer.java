package com.lbest.rm.plugin;

import android.text.TextUtils;

import com.lbest.rm.DNAH5Activity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by tonghaibo on 2016/5/20.
 * 定时跳转bridge
 */
public class Timer extends CordovaPlugin implements BLPluginInterfacer{

    @Override
    public boolean execute(String action, JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
        //判断Action是否为空，为空不处理
        if(TextUtils.isEmpty(action)){
            return false;
        }
        if(action.equals(OPEN_TIMER)){
            //
        }else if(action.equals(CLOSE_WEBVIEW)){
            closeWebViewActivity();
        }

        return false;
    }

    private void closeWebViewActivity(){
        DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
        activity.finish();
    }

}
