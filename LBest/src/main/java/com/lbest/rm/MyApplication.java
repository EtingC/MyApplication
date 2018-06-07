package com.lbest.rm;

import android.app.Application;
import android.util.Log;

import com.alibaba.wireless.security.open.SecurityGuardManager;
import com.alibaba.wireless.security.open.staticdatastore.IStaticDataStoreComponent;
import com.aliyun.alink.AlinkSDK;
import com.aliyun.alink.bone.CdnEnv;
import com.aliyun.alink.business.alink.ALinkEnv;
//import com.broadlink.log.CrashInterface;
//import com.broadlink.log.LogcatHelper;
import com.broadlink.log.CrashInterface;
import com.broadlink.log.LogcatHelper;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.BLFileUtils;
import com.lbest.rm.common.StorageUtils;
import com.lbest.rm.productDevice.DeviceManager;
import com.lbest.rm.utils.Logutils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.param.controller.BLConfigParam;

/**
 * Created by dell on 2017/7/26.
 */

public class MyApplication extends Application {
    private static final String TAG = "Lbest";
    private ALinkEnv alinkEnv = ALinkEnv.Online;
    private CdnEnv cdnEnv = CdnEnv.Release;
    private String AppKey;
    //线程池
    public static ExecutorService FULL_TASK_EXECUTOR;
    @Override
    public void onCreate() {
        super.onCreate();

//        LogcatHelper.init(getApplicationContext(),LogcatHelper. LEVEL_DEBUG, TAG);
//        LogcatHelper.start();
//        LogcatHelper.setCallBack(new CrashInterface() {
//            @Override
//            public void crashCallBack(boolean b, String s, String s1) {
//                System.exit(1);
//            }
//        });

        Logutils.setLogTag(TAG);
         //创建一个线程池供APP使用
        FULL_TASK_EXECUTOR = (ExecutorService) Executors.newCachedThreadPool();
        StorageUtils.init(getApplicationContext());
        initBLSdk();
        DeviceManager.getInstance().init();


        BLAcountToAli.getInstance().init(MyApplication.this.getApplicationContext());

        initEnv();
        getAppKey();
        initAliSdk();

        new Thread(new Runnable() {
            @Override
            public void run() {
                copyAssertJSToAPP();
            }
        }).start();
    }

    private void initEnv() {
        EnvConfigure.init(this);
        if (EnvConfigure.aLinkEnv !=null){
            alinkEnv = EnvConfigure.aLinkEnv;
        }else {
            EnvConfigure.saveAlinkEnv(this,alinkEnv);
        }
        if (EnvConfigure.cdnEnv !=null){
            cdnEnv = EnvConfigure.cdnEnv;
        } else {
            EnvConfigure.saveCDNEnv(this, cdnEnv);
        }
    }

    private void initAliSdk() {
        // 配置Log输出
        //AlinkSDK.setLogLevel(ALog.LEVEL_DEBUG);
        // 非阿里内网网络环境下，只能用线上环境
        AlinkSDK.setEnv(this, ALinkEnv.Online);
        // init account & open sdk
        AlinkSDK.init(MyApplication.this.getApplicationContext(), AppKey, BLAcountToAli.alinkLoginAdaptor);
    }

    private void initBLSdk(){
        BLConfigParam configParam = new BLConfigParam();
        configParam.put(BLConfigParam.SDK_FILE_PATH,StorageUtils.BASE_FILE_PATH);
        configParam.put(BLConfigParam.CONTROLLER_LOCAL_TIMEOUT, "3000");
        configParam.put(BLConfigParam.CONTROLLER_REMOTE_TIMEOUT, "5000");
//        configParam.put(BLConfigParam.CONTROLLER_SEND_COUNT, "2");
        configParam.put(BLConfigParam.CONTROLLER_JNI_LOG_LEVEL, "0");
        BLLet.init(MyApplication.this, BuildConfig.License, BuildConfig.ChannelID, configParam);
        BLLet.DebugLog.on();
        Log.d("sdkversion",BLLet.getSDKVersion());
    }

    private void getAppKey() {
        try {
            int index;
            if (alinkEnv == ALinkEnv.Daily) {
                index = 2;
            } else {
                index = 0;
            }
            SecurityGuardManager sgMgr = SecurityGuardManager.getInstance(this);
            if (sgMgr !=null) {
                IStaticDataStoreComponent sdsComp = sgMgr.getStaticDataStoreComp();
                if (sdsComp !=null) {
                    AppKey = sdsComp.getAppKeyByIndex(index, "");
                    Logutils.log_i("ali AppKey:"+AppKey);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    //将assert目录下的文件拷贝到本地文件夹
    public void copyAssertJSToAPP() {
        String trgPath = StorageUtils.BROADLINK_UI_PATH + File.separator + "cordova.js";
        String srcPath = "js/cordova.js";
        BLFileUtils.copyAssertFilesToSDCard(getApplicationContext(), srcPath , trgPath);

//        trgPath = StorageUtils.BROADLINK_FILE_PATH;
//        srcPath = "broadlink";
//        BLFileUtils.copyAssertDirToSDCard(getApplicationContext(), srcPath , trgPath);
//
        trgPath = StorageUtils.MODEL_PID_PATH;
        srcPath = "model_pid";
        BLFileUtils.copyAssertDirToSDCard(getApplicationContext(), srcPath , trgPath);

        trgPath = StorageUtils.BLALI_CODE_PATH;
        srcPath = "blalicode_map.txt";
        BLFileUtils.copyAssertFilesToSDCard(getApplicationContext(), srcPath , trgPath);

    }

}
