package com.lbest.rm.productDevice;

import com.alibaba.fastjson.JSON;
import com.lbest.rm.data.BaseDeviceInfo;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.utils.Logutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.data.controller.BLDNADevice;
import cn.com.broadlink.sdk.interfaces.controller.BLDeviceScanListener;
import cn.com.broadlink.sdk.param.family.BLFamilyDeviceInfo;
import cn.com.broadlink.sdk.param.family.BLFamilyModuleInfo;
import cn.com.broadlink.sdk.result.family.BLModuleControlResult;

/**
 * Created by dell on 2017/11/2.
 */

public class DeviceManager {
    private static  DeviceManager deviceManager;

    private ConcurrentHashMap<String,FamilyDeviceModuleData> devices=new ConcurrentHashMap<>();
    /**当前wifi下搜索到的设备列表**/
    private  volatile HashMap<String, BLDNADevice> mLocalDeviceList = new HashMap<>();
    /**记录设备最后一次扫描到的时间*/
    private  volatile HashMap<String, Long> mDevScanLastTime = new HashMap<>();

    private DeviceManager(){}

    public static DeviceManager getInstance(){
        if(deviceManager==null){
            synchronized (DeviceManager.class){
                if(deviceManager==null){
                    deviceManager=new DeviceManager();
                }
            }
        }
        return deviceManager;
    }


    public  FamilyDeviceModuleData getDevice(String deviceId){
        return devices.get(deviceId);
    }

    public  void addDevice(FamilyDeviceModuleData deviceInfo){
        devices.put(deviceInfo.getDid(),deviceInfo);
    }

    public void init(){
        //添加设备回调
        BLLet.Controller.startProbe();
        BLLet.Controller.setOnDeviceScanListener(new BLDeviceScanListener() {

            @Override
            public void onDeviceUpdate(BLDNADevice dnaDevice, boolean isNewDevice) {
                //Logutils.log_d("Probe device:"+ JSON.toJSONString(dnaDevice)+"  isNewDevice:"+isNewDevice);
                mDevScanLastTime.put(dnaDevice.getDid(), System.currentTimeMillis());
                mLocalDeviceList.put(dnaDevice.getDid(), dnaDevice);
            }

            @Override
            public boolean shouldAdd(BLDNADevice bldnaDevice) {
                return true;
            }
        });
    }

    public  void refreshDeviceList(List<FamilyDeviceModuleData> datas){
        devices.clear();
        if(datas!=null){
            for(FamilyDeviceModuleData deviceInfo:datas){
                devices.put(deviceInfo.getDid(),deviceInfo);
            }
        }
    }

    public ConcurrentHashMap<String, FamilyDeviceModuleData> getDevices() {
        return devices;
    }


    /**
     * 获取本地wifi下搜索到的设备列表
     *
     */
    public synchronized List<BLDNADevice> getLoaclWifiDeviceList(){
        final List<BLDNADevice> tempList = new ArrayList<>();
        HashMap<String, BLDNADevice> deviceList = mLocalDeviceList;
        Iterator<Map.Entry<String, BLDNADevice>> iter1 = deviceList.entrySet().iterator();
        while (iter1.hasNext()) {
            BLDNADevice deviceInfo = iter1.next().getValue();
            Long sceneTime = mDevScanLastTime.get(deviceInfo.getDid());
            if(sceneTime != null && System.currentTimeMillis() - sceneTime < 15000){
                tempList.add(deviceInfo);
            }
        }
        return tempList;
    }
}
