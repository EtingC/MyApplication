package com.lbest.rm.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.aliyun.alink.business.login.AlinkLoginBusiness;
import com.aliyun.alink.business.login.IAlinkLoginCallback;

import com.lbest.rm.BaseCallback;
import com.lbest.rm.account.broadlink.BLUserInfo;
import com.lbest.rm.data.RefreshTokenResult;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.productDevice.FamilyManager;
import com.lbest.rm.utils.Logutils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.param.family.BLFamilyAllInfo;
import cn.com.broadlink.sdk.param.family.BLFamilyDeviceInfo;
import cn.com.broadlink.sdk.param.family.BLFamilyModuleInfo;
import cn.com.broadlink.sdk.result.account.BLBaseResult;
import cn.com.broadlink.sdk.result.account.BLGetUserInfoResult;
import cn.com.broadlink.sdk.result.account.BLLoginResult;
import cn.com.broadlink.sdk.result.account.BLModifyUserIconResult;
import cn.com.broadlink.sdk.result.account.BLOauthResult;

/**
 * Created by dell on 2017/10/18.
 */

public class BLAcountToAli {
    public final static String METHOD_UNBINDDEVICE = "mtop.openalink.app.core.user.unbinddevice";//解绑设备
    public final static String METHOD_SHAREDEVICE_QR = "mtop.openalink.app.core.user.saveqr";//分享设备
    public final static String METHOD_PARSEDEVICE_QR = "mtop.openalink.app.core.user.scanqr";//分享设备
    public final static String METHOD_UNBINDUSERDEVICE = "mtop.openalink.uc.unbind.by.manager";

    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String EXPIRES_IN = "expires_in";
    private static final String BL_LOGINSESSION = "bl_loginsession";
    private static final String BL_NICKNAME = "bl_nickname";
    private static final String BL_USERID = "bl_userid";
    private static final String BL_ICON = "bl_icon";

    private final String client_id = "0657c5f338ff5a06707126ef69ad7647";
    private final String SecretKey = "f7d604e76394e3a4e69c99a76f09b65d";

    public abstract static class userDeviceListCallBack extends BaseCallback {
        public userDeviceListCallBack(Context mContext) {
            super(mContext);
        }
    }


    public static MyIAlinkLoginAdaptor alinkLoginAdaptor;
    private BLUserInfo blUserInfo;
    private static Context mContext;
    private static BLAcountToAli blAcountToAli;

    private boolean hasinit = false;

    private BLAcountToAli() {
    }


    public static BLAcountToAli getInstance() {
        if (blAcountToAli == null) {
            synchronized (BLAcountToAli.class) {
                if (blAcountToAli == null) {
                    blAcountToAli = new BLAcountToAli();
                }
            }
        }
        return blAcountToAli;
    }

    public void init(Context mContext) {
        if (!hasinit) {
            this.mContext = mContext;
            alinkLoginAdaptor = new MyIAlinkLoginAdaptor();
            blUserInfo = new BLUserInfo();
            SharedPreferences mUserInfoPreferences = mContext.getSharedPreferences("BLUserInfoFile", Context.MODE_PRIVATE);
            blUserInfo.setRefresh_token(mUserInfoPreferences.getString(REFRESH_TOKEN, null));
            blUserInfo.setAccess_token(mUserInfoPreferences.getString(ACCESS_TOKEN, null));
            blUserInfo.setExpires_in(mUserInfoPreferences.getString(EXPIRES_IN, null));
            blUserInfo.setBl_loginsession(mUserInfoPreferences.getString(BL_LOGINSESSION, null));
            blUserInfo.setBl_nickname(mUserInfoPreferences.getString(BL_NICKNAME, null));
            blUserInfo.setBl_userid(mUserInfoPreferences.getString(BL_USERID, null));
            blUserInfo.setBl_icon(mUserInfoPreferences.getString(BL_ICON, null));
            hasinit = true;
        }
    }

    public RefreshTokenResult refreshToken(String oldtoken) {
        RefreshTokenResult refreshTokenResult=new RefreshTokenResult();
        BLOauthResult blOauthResult = BLLet.Account.refreshAccessToken(oldtoken, client_id, SecretKey);
        refreshTokenResult.setResult(blOauthResult);
        if (blOauthResult != null && blOauthResult.succeed()) {
            String refresh_token = blOauthResult.getRefreshToken();
            String token = blOauthResult.getAccessToken();
            int expires = blOauthResult.getExpires_in();
            Logutils.log_d("refreshAccessToken success:" + refresh_token + "    " + token + "     " + expires);
            if (!TextUtils.isEmpty(token)) {
                BLLoginResult blLoginResult = BLLet.Account.oauthByIhc(token);
                refreshTokenResult.setResult(blLoginResult);
                if (blLoginResult != null && blLoginResult.succeed()) {
                    ArrayList<String> useridList = new ArrayList<>();
                    useridList.add(blLoginResult.getUserid());
                    BLGetUserInfoResult userInfo = BLLet.Account.getUserInfo(useridList);
                    String icon = null;
                    String nickName = null;
                    if (userInfo != null && userInfo.getInfo() != null && userInfo.getInfo().size() > 0) {
                        icon = userInfo.getInfo().get(0).getIcon();
                        nickName = userInfo.getInfo().get(0).getNickname();
                    }
                    refreshTokenResult.setCode(0);
                    refreshTokenResult.setSuccess(true);
                    BLAcountToAli.getInstance().saveUserInfo(refresh_token,token, String.valueOf(expires), blLoginResult.getLoginsession(), nickName, blLoginResult.getUserid(), icon);

                    BLUserInfo blUserInfo = BLAcountToAli.getInstance().getBlUserInfo();
                    BLLoginResult loginresult = new BLLoginResult();
                    loginresult.setLoginsession(blUserInfo.getBl_loginsession());
                    loginresult.setUserid(blUserInfo.getBl_userid());
                    BLLoginResult loginres = BLLet.Account.localLogin(loginresult);
                    BLLet.DebugLog.on();
                    if (loginres != null) {
                        Logutils.log_d("local login:" + JSON.toJSONString(loginres));
                    } else {
                        Logutils.log_d("local login:null");
                    }

                }else{
                    refreshTokenResult.setCode(-1);
                    if(blLoginResult != null ){
                        Logutils.log_d("oauthByIhc falill:"+ JSON.toJSONString(blLoginResult));
                    }else{
                        Logutils.log_d("oauthByIhc falill is null");
                    }
                }
            }else{
                refreshTokenResult.setCode(-2);
                Logutils.log_d("refreshAccessToken success but token is null old token");
            }
        }else{
            refreshTokenResult.setCode(-3);
            if(blOauthResult != null ){
                Logutils.log_d("refreshAccessToken falill:"+JSON.toJSONString(blOauthResult));
            }else{
                Logutils.log_d("refreshAccessToken falill is null");
            }
        }
        return refreshTokenResult;
    }

    public void login(IAlinkLoginCallback iAlinkLoginCallback) {
        AlinkLoginBusiness.getInstance().login(mContext, iAlinkLoginCallback);
    }

    public BLLoginResult login(String blid, String password) {
        BLLoginResult result = BLLet.Account.login(blid, password);
        return result;
    }

    public void logout(IAlinkLoginCallback iAlinkLoginCallback) {
        AlinkLoginBusiness.getInstance().logout(mContext, iAlinkLoginCallback);
    }

    public BLBaseResult modifyUserNickname(String newName) {
        BLBaseResult result = BLLet.Account.modifyUserNickname(newName);
        return result;
    }


    public BLModifyUserIconResult modifyUserIcon(File iconFile) {
        BLModifyUserIconResult result = BLLet.Account.modifyUserIcon(iconFile);
        return result;
    }


    public BLBaseResult modifyPassword(String oldpassword, String newpassword) {
        BLBaseResult result = BLLet.Account.modifyPassword(oldpassword, newpassword);
        return result;
    }


    public BLUserInfo getBlUserInfo() {
        return blUserInfo;
    }


    public void saveUserInfo(String refrsh_token,String access_token, String expires_in, String bl_loginsession, String bl_nickname, String bl_userid, String bl_icon) {
        SharedPreferences mUserInfoPreferences = mContext.getSharedPreferences("BLUserInfoFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mUserInfoPreferences.edit();
        editor.putString(REFRESH_TOKEN, refrsh_token);
        editor.putString(ACCESS_TOKEN, access_token);
        editor.putString(EXPIRES_IN, expires_in);
        editor.putString(BL_LOGINSESSION, bl_loginsession);
        editor.putString(BL_NICKNAME, bl_nickname);
        editor.putString(BL_USERID, bl_userid);
        editor.putString(BL_ICON, bl_icon);
        editor.commit();
        blUserInfo.setRefresh_token(refrsh_token);
        blUserInfo.setAccess_token(access_token);
        blUserInfo.setExpires_in(expires_in);
        blUserInfo.setBl_loginsession(bl_loginsession);
        blUserInfo.setBl_nickname(bl_nickname);
        blUserInfo.setBl_userid(bl_userid);
        blUserInfo.setBl_icon(bl_icon);
    }

    public void saveUserIcon(String icon_path) {
        SharedPreferences mUserInfoPreferences = mContext.getSharedPreferences("BLUserInfoFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mUserInfoPreferences.edit();
        editor.putString(BL_ICON, icon_path);
        editor.commit();
        blUserInfo.setBl_icon(icon_path);
    }

    public void saveUserNickname(String nickname) {
        SharedPreferences mUserInfoPreferences = mContext.getSharedPreferences("BLUserInfoFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mUserInfoPreferences.edit();
        editor.putString(BL_NICKNAME, nickname);
        editor.commit();
        blUserInfo.setBl_nickname(nickname);
    }

    /**
     * 退出登录，清空本地信息
     */
    public void cleanUserInfo() {
        Logutils.log_d("cleanUserInfo");
        SharedPreferences mUserInfoPreferences = mContext.getSharedPreferences("BLUserInfoFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mUserInfoPreferences.edit();
        editor.putString(REFRESH_TOKEN, null);
        editor.putString(ACCESS_TOKEN, null);
        editor.putString(EXPIRES_IN, null);
        editor.putString(BL_LOGINSESSION, null);
        editor.putString(BL_NICKNAME, null);
        editor.putString(BL_USERID, null);
        editor.putString(BL_ICON, null);
        editor.commit();
        blUserInfo.setRefresh_token(null);
        blUserInfo.setAccess_token(null);
        blUserInfo.setExpires_in(null);
        blUserInfo.setBl_loginsession(null);
        blUserInfo.setBl_nickname(null);
        blUserInfo.setBl_userid(null);
        blUserInfo.setBl_icon(null);

        //删除家庭设备信息
        FamilyManager.getInstance().cleanData();
    }


    public List<FamilyDeviceModuleData> reloadUserDeviceList() {
        FamilyManager.getInstance().readdBLNetWork();
        return getUserDeviceListV2();
    }

    public List<FamilyDeviceModuleData> refreshUserDeviceListV1() {
        if (FamilyManager.getInstance().refreshFamilyDataV1()) {
            return getUserDeviceListV1();
        }
        return null;
    }

    public List<FamilyDeviceModuleData> refreshUserDeviceListV2() {
        if (FamilyManager.getInstance().refreshFamilyDataV2()) {
            return getUserDeviceListV2();
        }
        return null;
    }

    public List<FamilyDeviceModuleData> getUserDeviceListV1() {
        List<FamilyDeviceModuleData> familyDeviceModuleDataList = new ArrayList<>();
        List<BLFamilyDeviceInfo> deviceInfoList = FamilyManager.getInstance().getFamilyDeviceModelV1();
        List<BLFamilyModuleInfo> moduleInfoList = FamilyManager.getInstance().getFamilyModuleV1();
        if (deviceInfoList != null) {
            int size_device = deviceInfoList.size();
            if (size_device > 0) {
                for (int i = 0; i < deviceInfoList.size(); i++) {
                    BLFamilyDeviceInfo deviceInfo = deviceInfoList.get(i);

                    FamilyDeviceModuleData familyDeviceModuleData = new FamilyDeviceModuleData();
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
                    String did = deviceInfo.getDid();
                    String sdid = deviceInfo.getsDid();
                    if (moduleInfoList != null) {
                        for (BLFamilyModuleInfo moduleInfo : moduleInfoList) {
                            List<BLFamilyModuleInfo.ModuleDeviceInfo> moduleDeviceInfos = moduleInfo.getModuleDevs();
                            if (moduleDeviceInfos != null && moduleDeviceInfos.size() > 0) {
                                BLFamilyModuleInfo.ModuleDeviceInfo moduleDeviceInfo = moduleDeviceInfos.get(0);
                                if (did.equals(moduleDeviceInfo.getDid())) {
                                    if (!TextUtils.isEmpty(sdid)) {
                                        if (sdid.equals(moduleDeviceInfo.getSdid())) {
                                            familyDeviceModuleData.setModuleIcon(moduleInfo.getIconPath());
                                            familyDeviceModuleData.setModuleid(moduleInfo.getModuleId());
                                            familyDeviceModuleData.setModuleName(moduleInfo.getName());
                                            familyDeviceModuleData.setExtend(moduleInfo.getExtend());
                                            break;
                                        }
                                    } else {
                                        familyDeviceModuleData.setModuleIcon(moduleInfo.getIconPath());
                                        familyDeviceModuleData.setModuleid(moduleInfo.getModuleId());
                                        familyDeviceModuleData.setModuleName(moduleInfo.getName());
                                        familyDeviceModuleData.setExtend(moduleInfo.getExtend());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    familyDeviceModuleDataList.add(familyDeviceModuleData);
                }
            }
        }
        return familyDeviceModuleDataList;
    }


    public List<FamilyDeviceModuleData> getUserDeviceListV2() {
        List<FamilyDeviceModuleData> familyDeviceModuleDataList = new ArrayList<>();
        List<BLFamilyDeviceInfo> deviceInfoList = FamilyManager.getInstance().getFamilyDeviceModelV2();
        List<BLFamilyModuleInfo> moduleInfoList = FamilyManager.getInstance().getFamilyModuleV2();
        if (deviceInfoList != null) {
            int size_device = deviceInfoList.size();
            if (size_device > 0) {
                for (int i = 0; i < deviceInfoList.size(); i++) {
                    BLFamilyDeviceInfo deviceInfo = deviceInfoList.get(i);

                    FamilyDeviceModuleData familyDeviceModuleData = new FamilyDeviceModuleData();
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
                    String did = deviceInfo.getDid();
                    String sdid = deviceInfo.getsDid();
                    if (moduleInfoList != null) {
                        for (BLFamilyModuleInfo moduleInfo : moduleInfoList) {
                            List<BLFamilyModuleInfo.ModuleDeviceInfo> moduleDeviceInfos = moduleInfo.getModuleDevs();
                            if (moduleDeviceInfos != null && moduleDeviceInfos.size() > 0) {
                                BLFamilyModuleInfo.ModuleDeviceInfo moduleDeviceInfo = moduleDeviceInfos.get(0);
                                if (did.equals(moduleDeviceInfo.getDid())) {
                                    if (!TextUtils.isEmpty(sdid)) {
                                        if (sdid.equals(moduleDeviceInfo.getSdid())) {
                                            familyDeviceModuleData.setModuleIcon(moduleInfo.getIconPath());
                                            familyDeviceModuleData.setModuleid(moduleInfo.getModuleId());
                                            familyDeviceModuleData.setModuleName(moduleInfo.getName());
                                            familyDeviceModuleData.setExtend(moduleInfo.getExtend());
                                            break;
                                        }
                                    } else {
                                        familyDeviceModuleData.setModuleIcon(moduleInfo.getIconPath());
                                        familyDeviceModuleData.setModuleid(moduleInfo.getModuleId());
                                        familyDeviceModuleData.setModuleName(moduleInfo.getName());
                                        familyDeviceModuleData.setExtend(moduleInfo.getExtend());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    familyDeviceModuleDataList.add(familyDeviceModuleData);
                }
            }
        }
        return familyDeviceModuleDataList;
    }


    public cn.com.broadlink.sdk.result.BLBaseResult delDeviveV1(String did) {
        return FamilyManager.getInstance().deleteDeviceToFamilyV1(did);
    }

    public cn.com.broadlink.sdk.result.BLBaseResult delDeviveV2(String did) {
        return FamilyManager.getInstance().deleteDeviceToFamilyV2(did);
    }

    public BLFamilyAllInfo queryfamilyInfoV2() {
        return FamilyManager.getInstance().queryFamilyInfoV2();
    }
}
