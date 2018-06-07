package com.lbest.rm.common;

import android.content.Context;
import android.os.Environment;

import com.alibaba.fastjson.JSON;
import com.lbest.rm.utils.CommonUtils;

import java.io.File;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.param.controller.BLConfigParam;

/**
 * Created by dell on 2017/11/1.
 */

public class StorageUtils {

    private static final String APP_NAME = "update.apk";
    private static final String TMPAPP_NAME = "update.apk.tmp";
    /**H5 html主页面**/
    private static final String H5_INDEX_PAGE = "app.html";

    public static String BASE_FILE_PATH;

    public static String CACHE_FILE_PATH;

    public static String BROADLINK_FILE_PATH;
    public static String BROADLINK_UI_PATH;
    public static String BROADLINK_SCRIPT_PATH;
    public static String MODEL_PID_PATH;
    public static String BLALI_CODE_PATH;
    private StorageUtils() {
    }

    public static void init(Context context) {

        // 存在SDCARD的时候，路径设置到SDCARD
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            BASE_FILE_PATH = context.getExternalCacheDir().getParentFile().getPath();
            // 不存在SDCARD的时候，路径设置到ROM
        } else {
            BASE_FILE_PATH = context.getCacheDir().getAbsolutePath();
        }
        CACHE_FILE_PATH = BASE_FILE_PATH + File.separator + "cache";
        BROADLINK_FILE_PATH = BASE_FILE_PATH;
        BROADLINK_UI_PATH = BROADLINK_FILE_PATH + File.separator + "ui";
        BROADLINK_SCRIPT_PATH = BROADLINK_FILE_PATH + File.separator + "script";
        MODEL_PID_PATH = BROADLINK_FILE_PATH + File.separator + "model_pid";
        BLALI_CODE_PATH=BROADLINK_FILE_PATH + File.separator + "blalicode_map.txt";
        File file1=new File(CACHE_FILE_PATH);
        if(!file1.exists()){
            file1.mkdir();
        }

        File file2=new File(BROADLINK_UI_PATH);
        if(!file2.exists()){
            file2.mkdirs();
        }

        File file3=new File(BROADLINK_SCRIPT_PATH);
        if(!file3.exists()){
            file3.mkdirs();
        }

        File file4=new File(MODEL_PID_PATH);
        if(!file4.exists()){
            file4.mkdirs();
        }
    }



    /**
     * 获取设备H5主显示页面
     *
     * @param pid
     * 			设备产品id
     * @return
     */
    public static String getH5IndexPath(String pid) {
        String folderPath = languageFolder(pid);
        if(folderPath != null){
            return folderPath + File.separator + H5_INDEX_PAGE;
        }
        return null;
    }

    /***
     * 返回语言包的文件夹夹
     * 	例如 sdcard/broadlink/pid/zh
     * @param pid
     * @return
     */
    public static String languageFolder(String pid){
        String language = CommonUtils.getLanguage();
        String folderPath = getH5Folder(pid) + File.separator + language;
        File languageFolder = new File(folderPath);
        if(languageFolder.exists()){
            return folderPath;
        }else{
            String[] languages = language.split("-");
            String countryPath = getH5Folder(pid) + File.separator + languages[0];
            File countryFolder = new File(countryPath);
            if(countryFolder.exists()){
                return countryPath;
            }
        }

        //解析本地desc。json文件，获取默认语言文件夹
        String content = BLFileUtils.readTextFileContent(getH5Folder(pid) + "/desc.json");
        if(content != null){
            DrpDescInfo descInfo = JSON.parseObject(content, DrpDescInfo.class);
            if(descInfo != null){
                return BLLet.Controller.queryUIPath(pid) + File.separator +  descInfo.getDefault_lang();
            }
        }

        return null;
    }


    /**
     * 获取设备UI文件夹
     *
     * @param pid
     * 			设备产品id
     * @return
     */
    public static String getH5Folder(String pid) {
        return BROADLINK_UI_PATH+File.separator+pid;
    }

    public static String getScriptAbsolutePath(String pid){
        return BROADLINK_SCRIPT_PATH+File.separator+pid;
    }

    /**
     * 获取Model_Pid文件
     *
     * @param model
     * 			设备产品model
     * @return
     */
    public static String getModel_PidFilePath(String model) {
        return MODEL_PID_PATH+File.separator+model+".txt";
    }


    public static String getBlalicode_mapFilePath() {
        return BLALI_CODE_PATH;
    }

    public static String getAppPath() {
        return CACHE_FILE_PATH+File.separator+APP_NAME;
    }
    public static String getTMPAppPath() {
        return CACHE_FILE_PATH+File.separator+TMPAPP_NAME;
    }
}
