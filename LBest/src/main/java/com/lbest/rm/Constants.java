package com.lbest.rm;

/**
 * Created by dell on 2017/10/27.
 */

public class Constants {

    public final static String DEVICENAME="晾霸智能电动晾衣机";
    public final static String INTENT_PRODUCTINFO="INTENT_PRODUCTINFO";
    public final static String INTENT_DEVICEUUID="INTENT_DEVICEUUID";
    public final static String INTENT_CROPPHOTOPATH="INTENT_CROPPHOTOPATH";
    public final static String INTENT_TAKEPHOTO_PATH="INTENT_TAKEPHOTO_TMP";
    public static final String INTENT_DEVICE = "INTENT_DEVICE";
    public static final String INTENT_SCENETIME = "INTENT_SCENETIME";
    public static final String INTENT_SCENEACTION = "INTENT_SCENEACTION";
    public static final String INTENT_SCENEREPEAT = "INTENT_SCENEREPEAT";
    public static final String INTENT_FRAGMENTINDEX = "INTENT_FRAGMENTINDEX";

    public static final String INTENT_REFRESHDATA = "INTENT_REFRESHDATA";

    public final static int REQUESTCODE_FROMGALLERY=100;
    public final static int REQUESTCODE_FROMCAMERA=101;
    public final static int REQUESTCODE_CROPIMAGE=102;

    public final static int REQUESTCODE_MODIFYDEVICE=103;
    public final static int REQUESTCODE_DEVICEPROPERTY=104;
    public final static int REQUESTCODE_SHAREDEVICE=105;
    public final static int REQUESTCODE_SCENEACTION=106;
    public final static int REQUESTCODE_SCENEREPEAT=107;

    public final static String TAKEICONPHOTO_NAME="takeicon_tmp.png";
    public final static String ALIDEVICEMANAGER_FLAG="0";


    public final static String LBESTOLDMODEL="LBESTGB_LIVING_AIRER_SD50_1_0_20184";

    public static class AliErrorCode{
        public final static int SUCCESS_CODE=1000;
    }

    public static class BLErrorCode{
        public final static int SUCCESS_CODE=0;
        public final static int NOLOGIN_CODE1=-3003;

        public final static int NOLOGIN_CODE2=-1012;
        public final static int NOLOGIN_CODE3=-1009;
        public final static int NOLOGIN_CODE4=-1000;
        public final static int NOLOGIN_CODE5=10011;
        public final static int NOLOGIN_CODE6=-3102;
    }


    public static class AliParamDefine{
        public final static String SWITCH="Switch";
    }

    public static class AliDeviceStatusDefine{
        public final static String SWITCH_ON="1";
        public final static String SWITCH_OFF="0";
    }

    public static class AliDeviceOnLineStateDefine{
        public final static String ONLINE="on";
        public final static String OFFLINE="off";
    }

    public static class AliOTAINFODefine{
        public final static String OTA_LATEAST="0";
        public final static String OTA_NEW="1";
        public final static String OTA_FORSE="1";
        public final static String OTA_NOFORSE="0";
        public final static String OTA_NOSTART="0";
        public final static String OTA_RESTART="1";
    }


    public static class AliProductTypeDefine{
        public final static int COMMON=0;
        public final static int GETWAY=1;
        public final static int SUBDEVICE=2;
    }
}
