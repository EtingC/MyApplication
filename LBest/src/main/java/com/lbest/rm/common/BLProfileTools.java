package com.lbest.rm.common;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.result.controller.BLProfileStringResult;

/**
 * Created by dell on 2017/11/29.
 */

public class BLProfileTools {
    private static String queryProfileStrByDid(String did){
        BLProfileStringResult result = BLLet.Controller.queryProfile(did);
        if(result != null &&  result.succeed()){
            return result.getProfile();
        }

        return null;
    }

    public static BLDevProfileInfo queryProfileInfoByDid(String did){
        String profileStr = queryProfileStrByDid(did);
        if(!TextUtils.isEmpty(profileStr)){
            return parseObject(profileStr);
        }

        return  null;
    }

    public static String queryProfileStrByPid(String pid){
        BLProfileStringResult result = BLLet.Controller.queryProfileByPid(pid);
        if(result != null &&  result.succeed()){
            return result.getProfile();
        }

        return null;
    }

    public static BLDevProfileInfo queryProfileInfoByPid(String pid){
        String profileStr = queryProfileStrByPid(pid);
        if(!TextUtils.isEmpty(profileStr)){
            return parseObject(profileStr);
        }

        return  null;
    }

    private static BLDevProfileInfo parseObject(String profileStr){
        try {
            BLDevProfileInfo devProfileInfo = new BLDevProfileInfo();
            JSONObject jsonObject = new JSONObject(profileStr);
            JSONObject descOject = jsonObject.optJSONObject("desc");
            int issubdev = jsonObject.optInt("issubdev", 0);
            JSONArray protocolArray = jsonObject.optJSONArray("protocol");
            JSONArray srvsArray = jsonObject.optJSONArray("srvs");
            int subscribable = jsonObject.optInt("subscribable", 0);
            int wificonfigtype = jsonObject.optInt("wificonfigtype", 0);
            JSONArray suidsArray = jsonObject.optJSONArray("suids");
//            JSONArray timeoutArray = jsonObject.optJSONArray("timeout");
            String ver = jsonObject.optString("ver");
            JSONObject limitsObject = jsonObject.optJSONObject("limits");

            BLDevDescInfo devDescInfo = new BLDevDescInfo();
            devDescInfo.setCat(descOject.optString("cat"));
            devDescInfo.setModel(descOject.optString("model"));
            devDescInfo.setPid(descOject.optString("pid"));
            devDescInfo.setVendor(descOject.optString("vendor"));

            devProfileInfo.setLimits(limitsObject);
            devProfileInfo.setVer(ver);
            devProfileInfo.setIssubdev(issubdev);
            devProfileInfo.setSubscribable(subscribable);
            devProfileInfo.setWificonfigtype(wificonfigtype);
            devProfileInfo.setDesc(devDescInfo);

            if(protocolArray != null){
                for (int i = 0; i < protocolArray.length(); i++) {
                    devProfileInfo.getProtocol().add(protocolArray.optString(i));
                }
            }

            if(srvsArray != null){
                for (int i = 0; i < srvsArray.length(); i++) {
                    devProfileInfo.getSrvs().add(srvsArray.optString(i));
                }
            }

            if(suidsArray != null){
                for (int i = 0; i < suidsArray.length(); i++) {
                    JSONObject item = suidsArray.optJSONObject(i);
                    BLDevSuidInfo devSuidInfo = new BLDevSuidInfo();
                    devSuidInfo.setSuid(item.optString("suid"));
                    devSuidInfo.setIntfs(item.optJSONObject("intfs"));
                    devProfileInfo.getSuids().add(devSuidInfo);
                }
            }

            return devProfileInfo;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
