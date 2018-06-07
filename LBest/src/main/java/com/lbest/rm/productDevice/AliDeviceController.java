package com.lbest.rm.productDevice;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.alink.business.alink.ALinkBusinessEx;
import com.aliyun.alink.business.devicecenter.api.add.DeviceInfo;
import com.aliyun.alink.business.devicecenter.api.discovery.IDiscoveryListener;
import com.aliyun.alink.business.devicecenter.api.discovery.LocalDeviceMgr;
import com.aliyun.alink.sdk.net.anet.api.AError;
import com.aliyun.alink.sdk.net.anet.api.transitorynet.TransitoryRequest;
import com.aliyun.alink.sdk.net.anet.api.transitorynet.TransitoryResponse;
import com.lbest.rm.BaseCallback;
import com.lbest.rm.data.db.FamilyDeviceModuleData;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.data.controller.BLDNADevice;
import cn.com.broadlink.sdk.data.controller.BLStdData;
import cn.com.broadlink.sdk.param.account.BLRegistParam;
import cn.com.broadlink.sdk.param.controller.BLStdControlParam;
import cn.com.broadlink.sdk.param.family.BLFamilyDeviceInfo;
import cn.com.broadlink.sdk.param.family.BLFamilyModuleInfo;
import cn.com.broadlink.sdk.result.account.BLBaseResult;
import cn.com.broadlink.sdk.result.controller.BLStdControlResult;
import cn.com.broadlink.sdk.result.family.BLModuleControlResult;

/**
 * Created by dell on 2017/7/27.
 */

public class AliDeviceController {
    public final static String METHOD_PRODUCTLIST="mtop.openalink.app.product.list";//产品列表
    public final static String METHOD_PRODUCTDETAIL="mtop.openalink.app.product.detail.get";//产品信息
    public final static String METHOD_GET_DEVICEDETAIL="mtop.openalink.app.core.device.getdetail";//获取设备详情
    public final static String METHOD_GET_OTAINFO="mtop.openalink.ota.upgrade.info.get";
    public final static String METHOD_OTA_UPGRADE="mtop.openalink.ota.device.upgrade";
    public final static String METHOD_GET_OTAUPGRADEPROCESS="mtop.openalink.ota.upgrade.status.get";
    //设备控制回调接口
    public abstract static class DeviceOperateCallBack extends BaseCallback {
        public DeviceOperateCallBack(Context mContext) {
            super(mContext);
        }
    }

    /**
     * 控制设备
     * paramKeyValues 设备属性键值对
     * devCallBack    控制回调接口
     * */
    public static BLStdControlResult setDeviceStatue(final String did,final String sdid,final Map<String,String> paramKeyValues){
        BLStdControlParam blStdControlParam=new BLStdControlParam();
        blStdControlParam.setAct("set");
        if(paramKeyValues!=null&&paramKeyValues.size()>0){
            Iterator<String> iterator=paramKeyValues.keySet().iterator();
            while(iterator.hasNext()){
                String key=iterator.next();
                String value=paramKeyValues.get(key);
                blStdControlParam.getParams().add(key);
                ArrayList<BLStdData.Value> values=new ArrayList<>();
                BLStdData.Value val=new BLStdData.Value();
                val.setIdx(0);
                val.setVal(value);
                values.add(val);
                blStdControlParam.getVals().add(values);
            }
        }
        return BLLet.Controller.dnaControl(did,sdid,blStdControlParam);
    }

    /**
     * 设备状态查询
     * paramNameList 要查询设备属性，null则为所有属性状态
     * devCallBack    查询回调接口
     * */
    public static BLStdControlResult getDeviceStatue(final String did,final String sdid,final List<String> paramNameList){
        BLStdControlParam blStdControlParam=new BLStdControlParam();
        blStdControlParam.setAct("get");
        if(paramNameList!=null&&paramNameList.size()>0){
            for(String paramName:paramNameList){
                blStdControlParam.getParams().add(paramName);
            }
        }
        return BLLet.Controller.dnaControl(did,sdid,blStdControlParam);
    }


    public static BLModuleControlResult addDeviceV1(FamilyDeviceModuleData familyDeviceModuleData){
       return FamilyManager.getInstance().addDeviceToFamilyV1(familyDeviceModuleData);
    }

    public static BLModuleControlResult addDeviceV2(FamilyDeviceModuleData familyDeviceModuleData){
        return FamilyManager.getInstance().addDeviceToFamilyV2(familyDeviceModuleData);
    }

    public static BLModuleControlResult moditfyDeviceNameV1(FamilyDeviceModuleData deviceModuleData, String name){
        return FamilyManager.getInstance().modifyFamilModuleNameV1(deviceModuleData.getModuleid(),name);
    }

    public static BLModuleControlResult moditfyDeviceNameV2(FamilyDeviceModuleData deviceModuleData, String name){
        return FamilyManager.getInstance().modifyFamilModuleNameV2(deviceModuleData.getModuleid(),name);
    }

    public static BLModuleControlResult moditfyDeviceIconV1(String moduleId, File iconFile){
        return FamilyManager.getInstance().modifyFamilModuleIconV1(moduleId,iconFile);
    }

    public static BLModuleControlResult moditfyDeviceIconV2(String moduleId, File iconFile){
        return FamilyManager.getInstance().modifyFamilModuleIconV2(moduleId,iconFile);
    }


    public static FamilyDeviceModuleData getBLDeviceDetail(String moduleId){
        FamilyDeviceModuleData familyDeviceModuleData=null;
        List<BLFamilyDeviceInfo> deviceInfoList= FamilyManager.getInstance().getFamilyDeviceModelV2();
        List<BLFamilyModuleInfo> moduleInfoList= FamilyManager.getInstance().getFamilyModuleV2();
        if(deviceInfoList!=null&&moduleInfoList!=null){
            int size_module=moduleInfoList.size();
            if(size_module>0){
                for(int i=0;i<moduleInfoList.size();i++){
                    BLFamilyModuleInfo moduleInfo=moduleInfoList.get(i);
                    if(moduleInfo.getModuleId().equals(moduleId)){
                        List<BLFamilyModuleInfo.ModuleDeviceInfo> moduleDeviceInfos = moduleInfo.getModuleDevs();
                        if (moduleDeviceInfos != null && moduleDeviceInfos.size() > 0) {
                            BLFamilyModuleInfo.ModuleDeviceInfo moduleDeviceInfo = moduleDeviceInfos.get(0);
                            String did=moduleDeviceInfo.getDid();
                            String sdid=moduleDeviceInfo.getSdid();
                            for( BLFamilyDeviceInfo deviceInfo:deviceInfoList){
                                if(did.equals(deviceInfo.getDid())){
                                    if(TextUtils.isEmpty(sdid)){
                                        familyDeviceModuleData=new FamilyDeviceModuleData();
                                        familyDeviceModuleData.setName(deviceInfo.getName());
                                        familyDeviceModuleData.setAeskey(deviceInfo.getAeskey());
                                        familyDeviceModuleData.setDid(deviceInfo.getDid());
                                        familyDeviceModuleData.setExtend(deviceInfo.getExtend());
                                        familyDeviceModuleData.setFamilyId(deviceInfo.getFamilyId());
                                        familyDeviceModuleData.setMac(deviceInfo.getMac());
                                        familyDeviceModuleData.setLatitude(deviceInfo.getLatitude());
                                        familyDeviceModuleData.setPid(deviceInfo.getPid());
                                        familyDeviceModuleData.setWifimac(deviceInfo.getWifimac());
                                        familyDeviceModuleData.setType(deviceInfo.getType());
                                        familyDeviceModuleData.setTerminalId(deviceInfo.getTerminalId());
                                        familyDeviceModuleData.setSubdeviceNum(deviceInfo.getSubdeviceNum());
                                        familyDeviceModuleData.setLock(deviceInfo.isLock());
                                        familyDeviceModuleData.setLongitude(deviceInfo.getLongitude());
                                        familyDeviceModuleData.setRoomId(deviceInfo.getRoomId());
                                        familyDeviceModuleData.setPassword(deviceInfo.getPassword());
                                        familyDeviceModuleData.setsDid(deviceInfo.getsDid());

                                        familyDeviceModuleData.setModuleIcon(moduleInfo.getIconPath());
                                        familyDeviceModuleData.setModuleid(moduleInfo.getModuleId());
                                        familyDeviceModuleData.setModuleName(moduleInfo.getName());
                                        break;
                                    }else if(sdid.equals(deviceInfo.getsDid())){
                                        familyDeviceModuleData=new FamilyDeviceModuleData();
                                        familyDeviceModuleData.setName(deviceInfo.getName());
                                        familyDeviceModuleData.setAeskey(deviceInfo.getAeskey());
                                        familyDeviceModuleData.setDid(deviceInfo.getDid());
                                        familyDeviceModuleData.setExtend(deviceInfo.getExtend());
                                        familyDeviceModuleData.setFamilyId(deviceInfo.getFamilyId());
                                        familyDeviceModuleData.setMac(deviceInfo.getMac());
                                        familyDeviceModuleData.setLatitude(deviceInfo.getLatitude());
                                        familyDeviceModuleData.setPid(deviceInfo.getPid());
                                        familyDeviceModuleData.setWifimac(deviceInfo.getWifimac());
                                        familyDeviceModuleData.setType(deviceInfo.getType());
                                        familyDeviceModuleData.setTerminalId(deviceInfo.getTerminalId());
                                        familyDeviceModuleData.setSubdeviceNum(deviceInfo.getSubdeviceNum());
                                        familyDeviceModuleData.setLock(deviceInfo.isLock());
                                        familyDeviceModuleData.setLongitude(deviceInfo.getLongitude());
                                        familyDeviceModuleData.setRoomId(deviceInfo.getRoomId());
                                        familyDeviceModuleData.setPassword(deviceInfo.getPassword());
                                        familyDeviceModuleData.setsDid(deviceInfo.getsDid());

                                        familyDeviceModuleData.setModuleIcon(moduleInfo.getIconPath());
                                        familyDeviceModuleData.setModuleid(moduleInfo.getModuleId());
                                        familyDeviceModuleData.setModuleName(moduleInfo.getName());
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        return familyDeviceModuleData;
    }
    /**
     * 设备状态查询
     * uuid 要查询设备的uuID
     * devCallBack    查询回调接口
     * */
    public static void getDeviceDetail(final String uuid, final DeviceOperateCallBack devCallBack){
        TransitoryRequest transitoryRequest = new TransitoryRequest();
        transitoryRequest.setMethod(METHOD_GET_DEVICEDETAIL);
        transitoryRequest.putParam("uuid", uuid);

        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                // do something for success
                if(devCallBack!=null){
                    int code=transitoryResponse.getError().getSubCode();
                    String msg=transitoryResponse.getError().getSubMsg();
                    devCallBack.callBack(code,msg,transitoryResponse.data);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                // do something for failed
                if(devCallBack!=null){
                    int code=aError.getSubCode();
                    String msg=aError.getSubMsg();
                    devCallBack.callBack(code,msg,null);

                }
            }
        });
    }


    public static void getOtaInfo(final String uuid, final  boolean isExtend,final DeviceOperateCallBack devCallBack){
        TransitoryRequest transitoryRequest = new TransitoryRequest();
        transitoryRequest.setMethod(METHOD_GET_OTAINFO);
        transitoryRequest.putParam("uuid", uuid);
        if(isExtend){
            transitoryRequest.putParam("extend", "1");
        }else{
            transitoryRequest.putParam("extend", "0");
        }
        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                // do something for success
                if(devCallBack!=null){
                    int code=transitoryResponse.getError().getSubCode();
                    String msg=transitoryResponse.getError().getSubMsg();
                    devCallBack.callBack(code,msg,transitoryResponse.data);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                // do something for failed
                if(devCallBack!=null){
                    int code=aError.getSubCode();
                    String msg=aError.getSubMsg();
                    devCallBack.callBack(code,msg,null);
                }
            }
        });
    }

    public static void upgradeOta(final String uuid, final String version,final DeviceOperateCallBack devCallBack){
        TransitoryRequest transitoryRequest = new TransitoryRequest();
        transitoryRequest.setMethod(METHOD_OTA_UPGRADE);
        transitoryRequest.putParam("version", version);
        transitoryRequest.putParam("uuid", uuid);

        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                // do something for success
                if(devCallBack!=null){
                    int code=transitoryResponse.getError().getSubCode();
                    String msg=transitoryResponse.getError().getSubMsg();
                    devCallBack.callBack(code,msg,transitoryResponse.data);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                // do something for failed
                if(devCallBack!=null){
                    int code=aError.getSubCode();
                    String msg=aError.getSubMsg();
                    devCallBack.callBack(code,msg,null);
                }
            }
        });
    }

    public static cn.com.broadlink.sdk.result.BLBaseResult upgradeBLOta(String did, String ota_url){
       return BLLet.Controller.updateFirmware(did,ota_url);
    }

    public static void getOtaUpgradeProcess(final String uuid, final String version,final DeviceOperateCallBack devCallBack){
        TransitoryRequest transitoryRequest = new TransitoryRequest();
        transitoryRequest.setMethod(METHOD_GET_OTAUPGRADEPROCESS);
        transitoryRequest.putParam("version", version);
        transitoryRequest.putParam("uuid", uuid);

        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                // do something for success
                if(devCallBack!=null){
                    int code=transitoryResponse.getError().getSubCode();
                    String msg=transitoryResponse.getError().getSubMsg();
                    devCallBack.callBack(code,msg,transitoryResponse.data);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                // do something for failed
                if(devCallBack!=null){
                    int code=aError.getSubCode();
                    String msg=aError.getSubMsg();
                    devCallBack.callBack(code,msg,null);
                }
            }
        });
    }

}
