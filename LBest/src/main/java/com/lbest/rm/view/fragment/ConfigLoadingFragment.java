package com.lbest.rm.view.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.aliyun.alink.business.devicecenter.api.add.AddDeviceBiz;
import com.aliyun.alink.business.devicecenter.api.add.DeviceInfo;
import com.aliyun.alink.business.devicecenter.api.add.IAddDeviceListener;
import com.aliyun.alink.business.devicecenter.base.DCErrorCode;
import com.lbest.rm.AccountMainActivity;
import com.lbest.rm.Constants;
import com.lbest.rm.DeviceConfigActivity;
import com.lbest.rm.ModifyDeviceNameActivity;
import com.lbest.rm.MyApplication;
import com.lbest.rm.R;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.StorageUtils;
import com.lbest.rm.data.DeviceDetailsInfo;
import com.lbest.rm.data.RefreshTokenResult;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.productDevice.AliDeviceController;
import com.lbest.rm.productDevice.DeviceManager;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.utils.Logutils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.data.controller.BLDNADevice;
import cn.com.broadlink.sdk.param.family.BLFamilyAllInfo;
import cn.com.broadlink.sdk.result.account.BLBaseResult;
import cn.com.broadlink.sdk.result.controller.BLDeviceConfigResult;
import cn.com.broadlink.sdk.result.controller.BLDownloadScriptResult;
import cn.com.broadlink.sdk.result.controller.BLDownloadUIResult;
import cn.com.broadlink.sdk.result.controller.BLPairResult;
import cn.com.broadlink.sdk.result.family.BLModuleControlResult;

import static com.aliyun.alink.business.devicecenter.api.add.DeviceInfo.JoinStep.EnrolleeProtocol_ACTIVEFAILURE;
import static com.aliyun.alink.business.devicecenter.api.add.DeviceInfo.JoinStep.EnrolleeProtocol_ACTVING;

/**
 * Created by dell on 2017/10/27.
 */

public class ConfigLoadingFragment extends BaseFragment {
    private DeviceConfigActivity mActivity;
    private ImageView todo_progress;
    private boolean isConfig = false;
    private boolean isDeviceActive = false;
    private String activeDevice_mac;
    //private String activeDevice_uuid;
    private String ssid;
    private String password;

    private long statrtTime;
    private final long TIMEOUT=81000;
    //开启查找设备定时器
    /**
     * 配置设备前缓存一份 本地的设备列表，用于配置没有newconfig字段标识的新设备判断
     **/
    private volatile ArrayList<BLDNADevice> mConfigSuccDevList = new ArrayList<>();
    private Timer mScanNewBLDeviceTimer;
    private Timer mScanNewDeviceTimer;
    private Handler mHandler;

    private BLAcountToAli blAcountToAli;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.configloading_fragment_layout, container, false);
        mActivity = (DeviceConfigActivity) getActivity();
        mActivity.setNextButtonListener(null);
        mActivity.setNextButton(getResources().getString(R.string.str_configiningwifi), Color.parseColor("#BBBBBB"), false);
        mHandler=new Handler();
        blAcountToAli=BLAcountToAli.getInstance();
        findView(view);
        initView();
        return view;
    }

    private void findView(View rootView) {
        todo_progress = (ImageView) rootView.findViewById(R.id.todo_progress);
    }

    private void initView() {
        RotateAnimation rotate  = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        LinearInterpolator lin = new LinearInterpolator();
        rotate.setInterpolator(lin);
        rotate.setDuration(1500);//设置动画持续周期
        rotate.setRepeatCount(-1);//设置重复次数
        rotate.setFillAfter(true);//动画执行完后是否停留在执行完的状态
        rotate.setStartOffset(0);//执行前的等待时间
        todo_progress.setAnimation(rotate);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mActivity.getProduct() != null){
            if(Constants.LBESTOLDMODEL.equals(mActivity.getProduct().getModel())){
                startBLConfig();
            }else{
                startConfig();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    public void stopConfig(){
                isConfig = false;
        isDeviceActive = false;
        activeDevice_mac = null;
        cancelScanNewDeviceTimer();
        cancelScanBLNewDeviceTimer();
        Product.stopConfigProductByBroadCast();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
//        if (!hidden) {
//            if (mActivity.getProduct() != null){
//                if(Constants.LBESTOLDMODEL.equals(mActivity.getProduct().getModel())){
//                    startBLConfig();
//                }else{
//                    startConfig();
//                }
//            }
//            mActivity.setNextButtonListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    mActivity.showConfigWifiFragment();
//                }
//            });
//        }
    }

    private void startBLConfig(){
        if (!isConfig) {
            Logutils.log_d("开始BL配网");
            isConfig=true;
            mConfigSuccDevList.clear();
            final String pid=Product.getBLPid(mActivity.getProduct().getModel());
            statrtTime=System.currentTimeMillis();
            startScanBLNewDeviceTimer();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String h5Path = StorageUtils.getH5IndexPath(pid);
                    if(TextUtils.isEmpty(h5Path)){
                        BLDownloadUIResult result1=BLLet.Controller.downloadUI(pid);
                        if(result1!=null){
                            Logutils.log_d("BLDownloadUIResult : " + JSON.toJSONString(result1));
                        }
                        BLDownloadScriptResult result2=  BLLet.Controller.downloadScript(pid);
                        if(result2!=null){
                            Logutils.log_d("BLDownloadScriptResult : " + JSON.toJSONString(result2));
                        }
                    }else{
                        File h5File = new File(h5Path);
                        if (!h5File.exists()) {
                            BLDownloadUIResult result1=BLLet.Controller.downloadUI(pid);
                            if(result1!=null){
                                Logutils.log_d("BLDownloadUIResult : " + JSON.toJSONString(result1));
                            }
                            BLDownloadScriptResult result2=  BLLet.Controller.downloadScript(pid);
                            if(result2!=null){
                                Logutils.log_d("BLDownloadScriptResult : " + JSON.toJSONString(result2));
                            }
                        }
                    }
                    BLDeviceConfigResult result=Product.configProductByBroadCastBL(mActivity,pid,ssid,password);
                    if(result!=null){
                        Logutils.log_d("configProductByBroadCastBL result:" + JSON.toJSONString(result));
                        if(result.succeed()){
                            activeDevice_mac=result.getMac();
                            Logutils.log_d("ActiveDevice Mac: " + activeDevice_mac);
                        }else{
                            isConfig=false;
                            BLLet.Controller.deviceConfigCancel();
                            cancelScanBLNewDeviceTimer();
                            Logutils.log_d("BL配网失败: " +JSON.toJSONString(result));
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mActivity.showConfigDeviceErrorFragment(ConfigDeviceErrorFragment.SHOWTYPE_CONFIGERR0R);
                                }
                            });
                        }
                    }else{
                        isConfig=false;
                        BLLet.Controller.deviceConfigCancel();
                        cancelScanBLNewDeviceTimer();
                        Logutils.log_d("BL配网失败");

                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mActivity.showConfigDeviceErrorFragment(ConfigDeviceErrorFragment.SHOWTYPE_CONFIGERR0R);
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void startConfig() {
        if (!isConfig) {
            Logutils.log_d("开始Ali配网");
            isDeviceActive = false;
            mConfigSuccDevList.clear();

            startScanNewDeviceTimer();

            Product.configProductByBroadCast(mActivity, mActivity.getProduct().getModel(), new IAddDeviceListener() {
                @Override
                public void onPreCheck(boolean b, DCErrorCode dcErrorCode) {
                    Logutils.log_d("onPreCheck");
                    isConfig = true;
                    //todo_progress.setProgress(5);
                }

                @Override
                public void onProvisionPrepare() {
                    Logutils.log_d("onProvisionPrepare:"+ssid+"   "+password);
                    AddDeviceBiz.getInstance().toggleProvision(ssid, password, 60);
                    //todo_progress.setProgress(10);
                }

                @Override
                public void onProvisioning() {
                    Logutils.log_d("onProvisioning");
                    //todo_progress.setProgress(20);
                }

                @Override
                public void onProvisionedResult(boolean b, DCErrorCode dcErrorCode) {
                    Logutils.log_d("onProvisionedResult " + b);
                    if (b) {
                        //todo_progress.setProgress(100);
                    } else {
                        isConfig = false;
                        mActivity.showConfigDeviceErrorFragment(ConfigDeviceErrorFragment.SHOWTYPE_CONFIGERR0R);
                    }
                }

                @Override
                public void onJoining(DeviceInfo.JoinStep joinStep) {
                    Logutils.log_d("onJoining:" + joinStep);
                    //step为 EnrolleeProtocol_ACTVING代表设备等待激活，需要提醒用户操作设备；
                    // step为EnrolleeProtocol_ACTIVEFAILURE代表设备激活失败，激活失败可以提醒用户再次尝试激活，直接再次调用startAddDevice即可直接进入再次激活流程。
                    if (joinStep == EnrolleeProtocol_ACTVING) {
                        mActivity.showActiveDeviceFragment();
                    } else if (joinStep == EnrolleeProtocol_ACTIVEFAILURE) {
                        //mActivity.showConfigWifiFragment();
                        isConfig = false;
                        mActivity.showConfigDeviceErrorFragment(ConfigDeviceErrorFragment.SHOWTYPE_ACTIVEERR0R);
                        //Toast.makeText(mActivity,"激活失败，请重试！",Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onJoinedResult(boolean b, final String s, DCErrorCode dcErrorCode) {
                    Logutils.log_d("onJoinedResult: " + b + "      " + s);
                    if (b && !TextUtils.isEmpty(s)) {
                        //activeDevice_uuid=s;
                        AliDeviceController.getDeviceDetail(s, new AliDeviceController.DeviceOperateCallBack(mActivity) {
                            @Override
                            public void callBack(int code, String msg, Object data) {
                                super.callBack(code, msg, data);
                                Logutils.log_d(" onJoinedResult  getDeviceDetail："+msg+"  "+code);
                                if (code == Constants.AliErrorCode.SUCCESS_CODE) {
                                    DeviceDetailsInfo deviceDetailsInfo = JSON.parseObject((String) data, DeviceDetailsInfo.class);
                                    if (deviceDetailsInfo != null) {
                                        activeDevice_mac = deviceDetailsInfo.getMac();
                                        Logutils.log_d("ActiveDevice Mac: " + activeDevice_mac);
                                        isDeviceActive = true;
                                    }
                                }
                                if(isDeviceActive){
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            isConfig = false;
                                        }
                                    },3000);
                                }else{
                                    isConfig = false;
                                }
                            }
                        });
                    } else {
                        isConfig = false;
                        mActivity.showConfigDeviceErrorFragment(ConfigDeviceErrorFragment.SHOWTYPE_ACTIVEERR0R);
                    }
                }
            });
        }
    }

    private void startScanBLNewDeviceTimer() {
        if (mScanNewBLDeviceTimer == null) {
            mScanNewBLDeviceTimer = new Timer();
            mScanNewBLDeviceTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    scanBLDevice();
                }
            }, 100, 3000);
        }
    }


    private void startScanNewDeviceTimer() {
        if (mScanNewDeviceTimer == null) {
            mScanNewDeviceTimer = new Timer();
            mScanNewDeviceTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    scanDevice();
                }
            }, 100, 3000);
        }
    }

    private void cancelScanNewDeviceTimer() {
        if (mScanNewDeviceTimer != null) {
            mScanNewDeviceTimer.cancel();
            mScanNewDeviceTimer = null;
        }
    }
    private void cancelScanBLNewDeviceTimer() {
        if (mScanNewBLDeviceTimer != null) {
            mScanNewBLDeviceTimer.cancel();
            mScanNewBLDeviceTimer = null;
        }
    }
    private synchronized void scanBLDevice(){
        // 就一直查找是否发现新设备
        List<BLDNADevice> localDeviceList = DeviceManager.getInstance().getLoaclWifiDeviceList();
        Logutils.log_d("局域网设备:"+JSON.toJSONString(localDeviceList));
        for (int i = 0; i < localDeviceList.size(); i++) {
            BLDNADevice device = localDeviceList.get(i);
            //Logutils.log_d("局域网 设备MAC:"+device.getMac()+"  "+device.getName());
            if (checkDevicePid(device) && !existConfigList(device.getDid())) {
                mConfigSuccDevList.add(device);
            }
        }

        boolean findDevice = false;
        for (BLDNADevice device : mConfigSuccDevList) {
            String deviceMac=device.getMac();
            if(deviceMac.equalsIgnoreCase(activeDevice_mac)){
                Logutils.log_d("发现设备:"+JSON.toJSONString(device));
                findDevice = true;
                new addDeviceToFamilyTask().executeOnExecutor(MyApplication.FULL_TASK_EXECUTOR, device);
                break;
            }
        }
        if((System.currentTimeMillis()-statrtTime)>TIMEOUT){
            if(findDevice){

            }else{
                Logutils.log_d("局域网没有发现对应MAC设备");
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.showConfigDeviceErrorFragment(ConfigDeviceErrorFragment.SHOWTYPE_CONFIGERR0R);
                    }
                });
            }
            isConfig=false;
            BLLet.Controller.deviceConfigCancel();
            cancelScanBLNewDeviceTimer();
        }else{
            if(findDevice){
                isConfig=false;
                BLLet.Controller.deviceConfigCancel();
                cancelScanBLNewDeviceTimer();
            }else{

            }
        }
    }

    private synchronized void scanDevice() {
        // 就一直查找是否发现新设备
        List<BLDNADevice> localDeviceList = DeviceManager.getInstance().getLoaclWifiDeviceList();
        for (int i = 0; i < localDeviceList.size(); i++) {
            BLDNADevice device = localDeviceList.get(i);
            if (checkDevicePid(device) && !existConfigList(device.getDid())) {
                mConfigSuccDevList.add(device);
            }
        }
        Logutils.log_d("scanDevice: " + "isConfig:"+isConfig+" isDeviceActive:"+isDeviceActive);
        if (!isConfig) {
            if (isDeviceActive) {
                if (mConfigSuccDevList.size() > 0) {
                    boolean findDevice = false;
                    for (BLDNADevice device : mConfigSuccDevList) {
                        String deviceMac=device.getMac();
                        if(deviceMac.equalsIgnoreCase(activeDevice_mac)){
                        findDevice = true;
                        new addDeviceToFamilyTask().executeOnExecutor(MyApplication.FULL_TASK_EXECUTOR, device);
                        break;
                        }
                    }
                    if (!findDevice) {
                        Logutils.log_d("局域网没有发现对应MAC设备");
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mActivity.showConfigDeviceErrorFragment(ConfigDeviceErrorFragment.SHOWTYPE_CONFIGERR0R);
                            }
                        });
                    }

                } else {
                    Logutils.log_d("局域网没有发现设备");
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mActivity.showConfigDeviceErrorFragment(ConfigDeviceErrorFragment.SHOWTYPE_CONFIGERR0R);
                        }
                    });
                }
            }
            cancelScanNewDeviceTimer();
        }
    }

    /**
     * 检查这个设备是否是新配置成功的设备
     **/
    private boolean checkDevicePid(BLDNADevice deviceInfo) {
        String model = mActivity.getProduct().getModel();
        if (Product.getBLPid(model).equals(deviceInfo.getPid())) {
            return true;
        }
        return false;
    }

    public boolean existConfigList(String did) {
        for (BLDNADevice device : mConfigSuccDevList) {
            if (device.getDid().equals(did)) {
                return true;
            }
        }
        return false;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    class addDeviceToFamilyTask extends AsyncTask<BLDNADevice, Void, BLModuleControlResult> {
        private BLDNADevice bldnaDevice;
        private RefreshTokenResult refreshTokenResult=null;

        @Override
        protected BLModuleControlResult doInBackground(BLDNADevice... params) {
            bldnaDevice = params[0];
                //配对设备
                boolean pairSuccess=false;
                for (int i = 0; i < 5; i++) {
                    BLPairResult pairResult = BLLet.Controller.pair(bldnaDevice);
                    if (pairResult.succeed()) {
                        bldnaDevice.setId(pairResult.getId());
                        bldnaDevice.setKey(pairResult.getKey());
                        pairSuccess=true;
                        break;
                    }
                }
                if(pairSuccess){
                    Logutils.log_d("配对成功！");
                    FamilyDeviceModuleData familyDeviceModuleData=new FamilyDeviceModuleData();
                    familyDeviceModuleData.setName(Constants.DEVICENAME);
                    familyDeviceModuleData.setAeskey(bldnaDevice.getKey());
                    familyDeviceModuleData.setExtend(bldnaDevice.getExtend());
                    familyDeviceModuleData.setMac(bldnaDevice.getMac());
                    familyDeviceModuleData.setPid(bldnaDevice.getPid());
                    familyDeviceModuleData.setType(bldnaDevice.getType());
                    familyDeviceModuleData.setTerminalId(bldnaDevice.getId());
                    familyDeviceModuleData.setLock(bldnaDevice.isLock());
                    familyDeviceModuleData.setPassword((int) bldnaDevice.getPassword());
                    if(TextUtils.isEmpty(bldnaDevice.getpDid())){
                        familyDeviceModuleData.setDid(bldnaDevice.getDid());
                    }else{
                        familyDeviceModuleData.setDid(bldnaDevice.getpDid());
                        familyDeviceModuleData.setsDid(bldnaDevice.getDid());
                    }
                    if(mActivity!=null&&mActivity.getProduct()!=null){
                        familyDeviceModuleData.setModuleIcon(mActivity.getProduct().getIcon());
                    }
                    familyDeviceModuleData.setModuleName(Constants.DEVICENAME);
                    BLModuleControlResult result=AliDeviceController.addDeviceV2(familyDeviceModuleData);
                    if(result==null||!result.succeed()){
                        String old_refresh_token = blAcountToAli.getBlUserInfo().getRefresh_token();
                        String old_access_token = blAcountToAli.getBlUserInfo().getAccess_token();
                        Logutils.log_d("addDeviceToFamilyTask refreshAccessToken old_refresh_token:"+old_refresh_token+"   old_access_token:"+old_access_token);
                        refreshTokenResult =  blAcountToAli.refreshToken(old_refresh_token);
                        if(refreshTokenResult.isSuccess()){
                            BLModuleControlResult controlResult = AliDeviceController.addDeviceV1(familyDeviceModuleData);
                            if(controlResult == null || controlResult.getStatus() == -2014){
                                BLFamilyAllInfo familyAllInfo = BLAcountToAli.getInstance().queryfamilyInfoV2();
                                if(familyAllInfo == null){
                                    return null;
                                }
                            }
                            return AliDeviceController.addDeviceV1(familyDeviceModuleData);


                        }else{
                            return null;
                        }
                    }else{
                        Logutils.log_d("addDeviceToFamilyTask success");
                        return result;
                    }
                }else{
                    Logutils.log_d("配对失败！");
                    return null;
                }
        }

        @Override
        protected void onPostExecute(final BLModuleControlResult blModuleControlResult) {
            super.onPostExecute(blModuleControlResult);
            if(mActivity==null||mActivity.isFinishing())
                return;

            if(refreshTokenResult!=null&&!refreshTokenResult.isSuccess()){
                BLBaseResult baseResult=refreshTokenResult.getResult();
                int code=refreshTokenResult.getCode();
                if(code==-3){
                    if(baseResult!=null){
                        Toast.makeText(mActivity,"error:"+baseResult.getError(),Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(mActivity,mActivity.getResources().getString(R.string.err_network),Toast.LENGTH_SHORT).show();
                    }
                }else {
                    if(code==-1){
                        Toast.makeText(mActivity,mActivity.getResources().getString(R.string.str_loginfail),Toast.LENGTH_SHORT).show();
                    }else if(code==-2){
                        Toast.makeText(mActivity,"token is null",Toast.LENGTH_SHORT).show();
                    }
//                    Intent intent = new Intent();
//                    intent.setClass(mActivity, AccountMainActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    mActivity.finish();
                }
                return;
            }

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(blModuleControlResult!=null){
                        Logutils.log_d("添加设备到家庭:"+JSON.toJSONString(blModuleControlResult));
                    }
                    if (blModuleControlResult != null && blModuleControlResult.getStatus() == Constants.BLErrorCode.SUCCESS_CODE) {
                        mActivity.showConfigDeviceErrorFragment(ConfigDeviceErrorFragment.SHOWTYPE_ACTIVESUCCESS);
                        //下载UI和脚本
                        String model = mActivity.getProduct().getModel();
                        String pid = bldnaDevice.getPid();
                        Logutils.log_d("绑定成功开始下载UI和脚本 model：" + model + "   pid:" + pid);
                        String h5Path = StorageUtils.getH5IndexPath(pid);
                        if(TextUtils.isEmpty(h5Path)){
                            Product.downloadBLUIAndScript(pid);
                        }else{
                            File h5File = new File(h5Path);
                            if (!h5File.exists()) {
                                Product.downloadBLUIAndScript(pid);
                            }
                        }
                    } else {
                        Logutils.log_d("添加设备到家庭失败");
                        mActivity.showConfigDeviceErrorFragment(ConfigDeviceErrorFragment.SHOWTYPE_ACTIVEERR0R);
                    }
                }
            });
        }
    }
}
