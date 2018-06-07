package com.lbest.rm.common;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.lbest.rm.data.FwVersionInfo;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.utils.http.HttpGetAccessor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.result.controller.BLFirmwareVersionResult;

/**
 * Created by dell on 2017/11/29.
 */

public class BLFwVersionParser implements BLFwVersionModule{
    /**固件版本**/
    public static final String FW_VERSION = "http://fwversions.ibroadlink.com/getfwversion?devicetype=%1$s";
    @Override
    public BLFirmwareVersionResult queryFwVersion(String did) {
        return BLLet.Controller.queryFirmwareVersion(did);
    }

    @Override
    public ArrayList<FwVersionInfo> queryCloudFwVersion(Context context, String fwVersion, int deviceType) {
        ArrayList<FwVersionInfo> versionList = new ArrayList<>();
        HttpGetAccessor blHttpGetAccessor = new HttpGetAccessor();
        String url = String.format(FW_VERSION, deviceType);
        String versionResult = blHttpGetAccessor.execute(url, null, String.class);
        if(!TextUtils.isEmpty(versionResult)){
            try {
                int locationVersion = Integer.parseInt(fwVersion);
                int firmWareX = locationVersion / 10000;
                int firmWareY = locationVersion % 10000;
                JsonParser mJsonParser = new JsonParser();
                JsonObject versionObject = mJsonParser.parse(versionResult).getAsJsonObject().get(String.valueOf(firmWareX)).getAsJsonObject();
                JsonArray array = versionObject.get("versions").getAsJsonArray();
                Gson gson = new Gson();
                Type listType = new TypeToken<ArrayList<FwVersionInfo>>() {}.getType();
                List<FwVersionInfo> list = (ArrayList<FwVersionInfo>) gson.fromJson(array, listType);
                for (FwVersionInfo versionInfo : list) {
                    int version = Integer.parseInt(versionInfo.getVersion()) % 10000 - firmWareY;
                    if(version > 0) {
                        versionList.add(versionInfo);
                    }
                }
            } catch (Exception e) {
                Log.e("BLFwVersionParser",e.getMessage() ,e);
            }
        }

        return versionList;
    }

    @Override
    public FwVersionInfo queryCloudNewFwVersion(Context context, FamilyDeviceModuleData deviceInfo) {
        BLFirmwareVersionResult result = queryFwVersion(deviceInfo.getDid());
        if(result != null && result.succeed()){
            ArrayList<FwVersionInfo> versionList = queryCloudFwVersion(context, result.getVersion(), deviceInfo.getType());
            if(!versionList.isEmpty()){
                FwVersionInfo newVersion = versionList.get(versionList.size() - 1);
                if(!newVersion.getVersion().equals(result.getVersion())){
                    return newVersion;
                }
            }
        }
        return null;
    }

}
