package com.lbest.rm.common;

import android.content.Context;

import com.lbest.rm.data.FwVersionInfo;
import com.lbest.rm.data.db.FamilyDeviceModuleData;

import java.util.ArrayList;

import cn.com.broadlink.sdk.result.controller.BLFirmwareVersionResult;

/**
 * Created by dell on 2017/11/29.
 */

public interface BLFwVersionModule {
    //查询固件版本
    BLFirmwareVersionResult queryFwVersion(String did);
    //查询固件云端版本列表
    ArrayList<FwVersionInfo> queryCloudFwVersion(Context context, String fwVersion, int deviceType);
    //获取云端是否有比当前固件更新的版本
    FwVersionInfo queryCloudNewFwVersion(Context context, FamilyDeviceModuleData deviceInfo);
}
