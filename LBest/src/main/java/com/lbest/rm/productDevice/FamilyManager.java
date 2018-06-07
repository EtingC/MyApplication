package com.lbest.rm.productDevice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lbest.rm.AccountMainActivity;
import com.lbest.rm.BuildConfig;
import com.lbest.rm.Constants;
import com.lbest.rm.LoadingActivity;
import com.lbest.rm.R;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.account.broadlink.BLUserInfo;
import com.lbest.rm.data.BLSubmitPicResult;
import com.lbest.rm.data.BaseDeviceInfo;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.utils.Logutils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.data.controller.BLDNADevice;
import cn.com.broadlink.sdk.interfaces.controller.BLDeviceScanListener;
import cn.com.broadlink.sdk.param.family.BLFamilyAllInfo;
import cn.com.broadlink.sdk.param.family.BLFamilyDeviceInfo;
import cn.com.broadlink.sdk.param.family.BLFamilyIdInfo;
import cn.com.broadlink.sdk.param.family.BLFamilyInfo;
import cn.com.broadlink.sdk.param.family.BLFamilyModuleInfo;
import cn.com.broadlink.sdk.result.BLBaseResult;
import cn.com.broadlink.sdk.result.account.BLLoginResult;
import cn.com.broadlink.sdk.result.family.BLAllFamilyInfoResult;
import cn.com.broadlink.sdk.result.family.BLFamilyIdListGetResult;
import cn.com.broadlink.sdk.result.family.BLFamilyInfoResult;
import cn.com.broadlink.sdk.result.family.BLModuleControlResult;

/**
 * Created by dell on 2017/11/2.
 */

public class FamilyManager {

    public final int FAMILY_EXIST = 0;
    public final int FAMILY_NO = 1;
    public final int FAMILY_NOTSURE = 2;

    private Activity mActivity;
    private static FamilyManager familyManager;
    private BLFamilyAllInfo currentFamilyAllInfo;
    private BLFamilyInfo currentFamily;
    private String familyName;

    public String getCurrentFamilyID() {
        if (currentFamily != null) {
            return currentFamily.getFamilyId();
        }
        return null;
    }

    private FamilyManager() {
    }


    public static FamilyManager getInstance() {
        if (familyManager == null) {
            synchronized (FamilyManager.class) {
                if (familyManager == null) {
                    familyManager = new FamilyManager();
                }
            }
        }
        return familyManager;
    }


    public void init(Activity mActivity) {
        this.mActivity = mActivity;
        familyName = "*#com_lbest_rm_sfgxh#*";
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (existDefaultFamily() == FAMILY_NO) {
                    createDefaultFamily();
                }
            }
        }).start();
    }

    public void cleanData(){
        currentFamily=null;
        currentFamilyAllInfo=null;
    }

    private synchronized BLFamilyInfo createDefaultFamily() {
        if (currentFamily != null) {
            return currentFamily;
        }
        BLFamilyInfo blFamilyInfo = new BLFamilyInfo();
        blFamilyInfo.setFamilyName(familyName);
        blFamilyInfo.setFamilyDescription("");
        BLFamilyInfoResult blFamilyInfoResult = BLLet.Family.createNewFamily(blFamilyInfo, null);
        if (blFamilyInfoResult != null) {
            Logutils.log_w("createDefaultFamily:" + JSON.toJSONString(blFamilyInfoResult));
            int code = blFamilyInfoResult.getStatus();
            if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                currentFamilyAllInfo = queryFamilyInfoV1();
                currentFamily = currentFamilyAllInfo.getFamilyInfo();
            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE1
                    || code == Constants.BLErrorCode.NOLOGIN_CODE6) {
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.setClass(mActivity, LoadingActivity.class);
                            mActivity.startActivity(intent);
                        }
                    });
                }

            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE2
                    || code == Constants.BLErrorCode.NOLOGIN_CODE3
                    || code == Constants.BLErrorCode.NOLOGIN_CODE4
                    || code == Constants.BLErrorCode.NOLOGIN_CODE5) {
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Logutils.log_w("AKErrorLoginTokenIllegalError");
                            Toast.makeText(mActivity, mActivity.getResources().getString(R.string.hasnotlogin), Toast.LENGTH_LONG).show();
                            BLAcountToAli.getInstance().cleanUserInfo();
                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.setClass(mActivity, AccountMainActivity.class);
                            mActivity.startActivity(intent);
                        }
                    });
                }
            }
        }
        return currentFamily;
    }

    private BLFamilyAllInfo queryFamilyInfoV1() {
        BLFamilyAllInfo familyAllInfo = null;
        BLFamilyIdListGetResult idList = BLLet.Family.queryLoginUserFamilyIdList();
        if (idList != null) {
            int code = idList.getStatus();
            Logutils.log_d("queryFamilyInfo 家庭列表：" + JSON.toJSONString(idList));
            if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                List<BLFamilyIdInfo> familyIdInfoList = idList.getIdInfoList();
                if (familyIdInfoList != null && familyIdInfoList.size() > 0) {

                    for (BLFamilyIdInfo familyIdInfo : familyIdInfoList) {
                        if (familyName.equals(familyIdInfo.getFamilyName())) {
                            String familyID = familyIdInfo.getFamilyId();
                            String[] ids = {familyID};
                            BLAllFamilyInfoResult blAllFamilyInfoResult = BLLet.Family.queryAllFamilyInfos(ids);
                            if (blAllFamilyInfoResult != null) {
                                Logutils.log_d("queryFamilyInfo 家庭信息：" + JSON.toJSONString(blAllFamilyInfoResult));
                                if (blAllFamilyInfoResult.getStatus() == Constants.BLErrorCode.SUCCESS_CODE) {
                                    //Logutils.log_d("remove  All  Device from bllet--->");
                                    //BLLet.Controller.removeAllDevice();
                                    List<BLFamilyAllInfo> allInfos = blAllFamilyInfoResult.getAllInfos();
                                    if (allInfos != null && allInfos.size() > 0) {
                                        BLFamilyAllInfo blFamilyAllInfo = allInfos.get(0);
                                        addFamilyDeviceToNetWork(blFamilyAllInfo);
                                        familyAllInfo = blFamilyAllInfo;
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE1
                    || code == Constants.BLErrorCode.NOLOGIN_CODE6) {
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.setClass(mActivity, LoadingActivity.class);
                            mActivity.startActivity(intent);
                        }
                    });
                }

            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE2
                    || code == Constants.BLErrorCode.NOLOGIN_CODE3
                    || code == Constants.BLErrorCode.NOLOGIN_CODE4
                    || code == Constants.BLErrorCode.NOLOGIN_CODE5) {
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Logutils.log_w("AKErrorLoginTokenIllegalError");
                            Toast.makeText(mActivity, mActivity.getResources().getString(R.string.hasnotlogin), Toast.LENGTH_LONG).show();
                            BLAcountToAli.getInstance().cleanUserInfo();
                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.setClass(mActivity, AccountMainActivity.class);
                            mActivity.startActivity(intent);
                        }
                    });
                }
            }
        }
        if (familyAllInfo != null) {
            currentFamilyAllInfo = familyAllInfo;
            currentFamily = currentFamilyAllInfo.getFamilyInfo();
        }
        return familyAllInfo;
    }


    public synchronized BLFamilyAllInfo queryFamilyInfoV2() {
        BLFamilyAllInfo familyAllInfo = null;
        BLFamilyIdListGetResult idList = BLLet.Family.queryLoginUserFamilyIdList();
        if (idList != null) {
            int code = idList.getStatus();
            Logutils.log_d("queryFamilyInfo 家庭列表：" + JSON.toJSONString(idList));
            if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                List<BLFamilyIdInfo> familyIdInfoList = idList.getIdInfoList();
                if (familyIdInfoList != null && familyIdInfoList.size() > 0) {

                    for (BLFamilyIdInfo familyIdInfo : familyIdInfoList) {
                        if (familyName.equals(familyIdInfo.getFamilyName())) {
                            String familyID = familyIdInfo.getFamilyId();
                            String[] ids = {familyID};
                            BLAllFamilyInfoResult blAllFamilyInfoResult = BLLet.Family.queryAllFamilyInfos(ids);
                            if (blAllFamilyInfoResult != null) {
                                Logutils.log_d("queryFamilyInfo 家庭信息：" + JSON.toJSONString(blAllFamilyInfoResult));
                                if (blAllFamilyInfoResult.getStatus() == Constants.BLErrorCode.SUCCESS_CODE) {
                                    //Logutils.log_d("remove  All  Device from bllet--->");
                                    //BLLet.Controller.removeAllDevice();
                                    List<BLFamilyAllInfo> allInfos = blAllFamilyInfoResult.getAllInfos();
                                    if (allInfos != null && allInfos.size() > 0) {
                                        BLFamilyAllInfo blFamilyAllInfo = allInfos.get(0);
                                        addFamilyDeviceToNetWork(blFamilyAllInfo);
                                        familyAllInfo = blFamilyAllInfo;
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (familyAllInfo != null) {
            currentFamilyAllInfo = familyAllInfo;
            currentFamily = currentFamilyAllInfo.getFamilyInfo();
        }
        return familyAllInfo;
    }

    private int existDefaultFamily() {
        if (currentFamilyAllInfo != null && currentFamily != null) {
            return FAMILY_EXIST;
        }
        int family_code = FAMILY_NO;
        BLFamilyIdListGetResult blFamilyIdListGetResult = BLLet.Family.queryLoginUserFamilyIdList();
        if (blFamilyIdListGetResult != null) {
            Logutils.log_d("existDefaultFamily ：" + JSON.toJSONString(blFamilyIdListGetResult));
            int code = blFamilyIdListGetResult.getStatus();
            if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                List<BLFamilyIdInfo> familyIdInfoList = blFamilyIdListGetResult.getIdInfoList();
                if (familyIdInfoList != null && familyIdInfoList.size() > 0) {
                    for (BLFamilyIdInfo familyIdInfo : familyIdInfoList) {
                        if (familyName.equals(familyIdInfo.getFamilyName())) {
                            String familyID = familyIdInfo.getFamilyId();
                            String[] ids = {familyID};
                            BLAllFamilyInfoResult blAllFamilyInfoResult = BLLet.Family.queryAllFamilyInfos(ids);
                            if (blAllFamilyInfoResult != null) {
                                Logutils.log_d("existDefaultFamily queryAllFamilyInfos：" + JSON.toJSONString(blAllFamilyInfoResult));
                            }
                            if (blAllFamilyInfoResult != null && blAllFamilyInfoResult.getStatus() == Constants.BLErrorCode.SUCCESS_CODE) {
                                List<BLFamilyAllInfo> allInfos = blAllFamilyInfoResult.getAllInfos();
                                if (allInfos != null && allInfos.size() > 0) {
                                    currentFamilyAllInfo = allInfos.get(0);
                                    currentFamily = currentFamilyAllInfo.getFamilyInfo();
                                    Logutils.log_d("addFamilyDeviceToNetWork from existDefaultFamily");
                                    addFamilyDeviceToNetWork(currentFamilyAllInfo);
                                }
                            }
                            family_code = FAMILY_EXIST;
                            break;
                        }
                    }
                } else {
                    family_code = FAMILY_NO;
                }
            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE1
                    || code == Constants.BLErrorCode.NOLOGIN_CODE6) {
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.setClass(mActivity, LoadingActivity.class);
                            mActivity.startActivity(intent);
                        }
                    });
                }

            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE2
                    || code == Constants.BLErrorCode.NOLOGIN_CODE3
                    || code == Constants.BLErrorCode.NOLOGIN_CODE4
                    || code == Constants.BLErrorCode.NOLOGIN_CODE5) {
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Logutils.log_w("AKErrorLoginTokenIllegalError");
                            Toast.makeText(mActivity, mActivity.getResources().getString(R.string.hasnotlogin), Toast.LENGTH_SHORT).show();
                            BLAcountToAli.getInstance().cleanUserInfo();
                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.setClass(mActivity, AccountMainActivity.class);
                            mActivity.startActivity(intent);
                        }
                    });
                }
            }
        } else {
            family_code = FAMILY_NOTSURE;
        }
        return family_code;
    }

    public synchronized BLModuleControlResult addDeviceToFamilyV1(FamilyDeviceModuleData familyDeviceModuleData) {
        int family_code = existDefaultFamily();
        if (family_code == FAMILY_NO) {
            createDefaultFamily();
        }
        if (currentFamilyAllInfo == null || currentFamily == null)
            return null;
        BLFamilyModuleInfo familyModuleInfo = new BLFamilyModuleInfo();
        BLFamilyDeviceInfo familyDeviceInfo = new BLFamilyDeviceInfo();
        BLFamilyModuleInfo.ModuleDeviceInfo moduleDeviceInfo = new BLFamilyModuleInfo.ModuleDeviceInfo();
        List<BLFamilyModuleInfo.ModuleDeviceInfo> moduleDeviceInfoList = new ArrayList<BLFamilyModuleInfo.ModuleDeviceInfo>();

        moduleDeviceInfo.setDid(familyDeviceModuleData.getDid());
        moduleDeviceInfo.setOrder(0);
        moduleDeviceInfoList.add(moduleDeviceInfo);

        familyModuleInfo.setModuleDevs(moduleDeviceInfoList);
        familyModuleInfo.setFamilyId(currentFamily.getFamilyId());
        familyModuleInfo.setModuleType(1);
        familyModuleInfo.setIconPath(familyDeviceModuleData.getModuleIcon());
        familyModuleInfo.setName(familyDeviceModuleData.getModuleName());
        familyModuleInfo.setOrder(1);
        familyModuleInfo.setFlag(1);
        familyModuleInfo.setFollowDev(1);
        familyModuleInfo.setExtend(familyDeviceModuleData.getExtend());


        familyDeviceInfo.setFamilyId(currentFamily.getFamilyId());
        familyDeviceInfo.setPid(familyDeviceModuleData.getPid());
        familyDeviceInfo.setDid(familyDeviceModuleData.getDid());
        familyDeviceInfo.setLock(familyDeviceModuleData.isLock());
        familyDeviceInfo.setPassword(familyDeviceModuleData.getPassword());
        familyDeviceInfo.setName(familyDeviceModuleData.getName());
        familyDeviceInfo.setMac(familyDeviceModuleData.getMac());
        familyDeviceInfo.setTerminalId(familyDeviceModuleData.getTerminalId());
        familyDeviceInfo.setAeskey(familyDeviceModuleData.getAeskey());
        familyDeviceInfo.setType(familyDeviceModuleData.getType());
        familyDeviceInfo.setLatitude("");
        familyDeviceInfo.setLongitude("");
        familyDeviceInfo.setSubdeviceNum(0);

        BLModuleControlResult moduleControlResult = BLLet.Family.addModuleToFamily(familyModuleInfo,
                currentFamily, familyDeviceInfo, null);

        if (moduleControlResult != null) {
            int code = moduleControlResult.getStatus();
            Logutils.log_d("addDeviceToFamily 添加设备到家庭：" + JSON.toJSONString(moduleControlResult));
            if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                BLDNADevice bldnaDevice = new BLDNADevice();
                bldnaDevice.setDid(familyDeviceModuleData.getDid());
                bldnaDevice.setMac(familyDeviceModuleData.getMac());
                bldnaDevice.setId(familyDeviceModuleData.getTerminalId());
                bldnaDevice.setKey(familyDeviceModuleData.getAeskey());
                bldnaDevice.setLock(familyDeviceModuleData.isLock());
                bldnaDevice.setPid(familyDeviceModuleData.getPid());
                bldnaDevice.setpDid(familyDeviceModuleData.getDid());
                bldnaDevice.setName(familyDeviceModuleData.getName());
                bldnaDevice.setPassword(familyDeviceModuleData.getPassword());
                bldnaDevice.setType(familyDeviceModuleData.getType());
                bldnaDevice.setExtend(familyDeviceModuleData.getExtend());
                BLLet.Controller.addDevice(bldnaDevice);

                BLFamilyAllInfo familyAllInfo = queryFamilyInfoV1();
                if (familyAllInfo != null) {
                    currentFamilyAllInfo = familyAllInfo;
                    currentFamily = currentFamilyAllInfo.getFamilyInfo();
                }
            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE1
                    || code == Constants.BLErrorCode.NOLOGIN_CODE6) {
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.setClass(mActivity, LoadingActivity.class);
                            mActivity.startActivity(intent);
                        }
                    });
                }

            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE2
                    || code == Constants.BLErrorCode.NOLOGIN_CODE3
                    || code == Constants.BLErrorCode.NOLOGIN_CODE4
                    || code == Constants.BLErrorCode.NOLOGIN_CODE5) {
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Logutils.log_w("AKErrorLoginTokenIllegalError");
                            Toast.makeText(mActivity, mActivity.getResources().getString(R.string.hasnotlogin), Toast.LENGTH_SHORT).show();
                            BLAcountToAli.getInstance().cleanUserInfo();
                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.setClass(mActivity, AccountMainActivity.class);
                            mActivity.startActivity(intent);
                        }
                    });
                }
            }
        } else {
            Logutils.log_d("添加设备到家庭失败");
        }
        return moduleControlResult;
    }


    public synchronized BLModuleControlResult addDeviceToFamilyV2(FamilyDeviceModuleData familyDeviceModuleData) {
        if (currentFamilyAllInfo == null || currentFamily == null)
            return null;
        BLFamilyModuleInfo familyModuleInfo = new BLFamilyModuleInfo();
        BLFamilyDeviceInfo familyDeviceInfo = new BLFamilyDeviceInfo();
        BLFamilyModuleInfo.ModuleDeviceInfo moduleDeviceInfo = new BLFamilyModuleInfo.ModuleDeviceInfo();
        List<BLFamilyModuleInfo.ModuleDeviceInfo> moduleDeviceInfoList = new ArrayList<BLFamilyModuleInfo.ModuleDeviceInfo>();

        moduleDeviceInfo.setDid(familyDeviceModuleData.getDid());
        moduleDeviceInfo.setOrder(0);
        moduleDeviceInfoList.add(moduleDeviceInfo);

        familyModuleInfo.setModuleDevs(moduleDeviceInfoList);
        familyModuleInfo.setFamilyId(currentFamily.getFamilyId());
        familyModuleInfo.setModuleType(1);
        familyModuleInfo.setIconPath(familyDeviceModuleData.getModuleIcon());
        familyModuleInfo.setName(familyDeviceModuleData.getModuleName());
        familyModuleInfo.setOrder(1);
        familyModuleInfo.setFlag(1);
        familyModuleInfo.setFollowDev(1);
        familyModuleInfo.setExtend(familyDeviceModuleData.getExtend());


        familyDeviceInfo.setFamilyId(currentFamily.getFamilyId());
        familyDeviceInfo.setPid(familyDeviceModuleData.getPid());
        familyDeviceInfo.setDid(familyDeviceModuleData.getDid());
        familyDeviceInfo.setLock(familyDeviceModuleData.isLock());
        familyDeviceInfo.setPassword(familyDeviceModuleData.getPassword());
        familyDeviceInfo.setName(familyDeviceModuleData.getName());
        familyDeviceInfo.setMac(familyDeviceModuleData.getMac());
        familyDeviceInfo.setTerminalId(familyDeviceModuleData.getTerminalId());
        familyDeviceInfo.setAeskey(familyDeviceModuleData.getAeskey());
        familyDeviceInfo.setType(familyDeviceModuleData.getType());
        familyDeviceInfo.setLatitude("");
        familyDeviceInfo.setLongitude("");
        familyDeviceInfo.setSubdeviceNum(0);

        BLModuleControlResult moduleControlResult = BLLet.Family.addModuleToFamily(familyModuleInfo,
                currentFamily, familyDeviceInfo, null);

        if (moduleControlResult != null) {
            int code = moduleControlResult.getStatus();
            Logutils.log_d("addDeviceToFamily 添加设备到家庭：" + JSON.toJSONString(moduleControlResult));
            if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                BLDNADevice bldnaDevice = new BLDNADevice();
                bldnaDevice.setDid(familyDeviceModuleData.getDid());
                bldnaDevice.setMac(familyDeviceModuleData.getMac());
                bldnaDevice.setId(familyDeviceModuleData.getTerminalId());
                bldnaDevice.setKey(familyDeviceModuleData.getAeskey());
                bldnaDevice.setLock(familyDeviceModuleData.isLock());
                bldnaDevice.setPid(familyDeviceModuleData.getPid());
                bldnaDevice.setpDid(familyDeviceModuleData.getDid());
                bldnaDevice.setName(familyDeviceModuleData.getName());
                bldnaDevice.setPassword(familyDeviceModuleData.getPassword());
                bldnaDevice.setType(familyDeviceModuleData.getType());
                bldnaDevice.setExtend(familyDeviceModuleData.getExtend());
                BLLet.Controller.addDevice(bldnaDevice);

                BLFamilyAllInfo familyAllInfo = queryFamilyInfoV2();
                if (familyAllInfo != null) {
                    currentFamilyAllInfo = familyAllInfo;
                    currentFamily = currentFamilyAllInfo.getFamilyInfo();
                }
            }
        } else {
            Logutils.log_d("添加设备到家庭失败");
        }
        return moduleControlResult;
    }


    public synchronized BLBaseResult deleteDeviceToFamilyV1(String did) {
        if (currentFamily == null)
            return null;

        BLBaseResult baseResult = BLLet.Family.removeDeviceFromFamily(did, currentFamily.getFamilyId(), currentFamily.getFamilyVersion());
        if (baseResult != null) {
            int code = baseResult.getStatus();
            Logutils.log_d("deleteDeviceToFamily ：" + JSON.toJSONString(baseResult));
            if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                BLFamilyAllInfo familyAllInfo = queryFamilyInfoV1();
                if (familyAllInfo != null) {
                    currentFamilyAllInfo = familyAllInfo;
                    currentFamily = currentFamilyAllInfo.getFamilyInfo();
                }
            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE1
                    || code == Constants.BLErrorCode.NOLOGIN_CODE6) {
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.setClass(mActivity, LoadingActivity.class);
                            mActivity.startActivity(intent);
                        }
                    });
                }

            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE2
                    || code == Constants.BLErrorCode.NOLOGIN_CODE3
                    || code == Constants.BLErrorCode.NOLOGIN_CODE4
                    || code == Constants.BLErrorCode.NOLOGIN_CODE5) {
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Logutils.log_w("AKErrorLoginTokenIllegalError");
                            Toast.makeText(mActivity, mActivity.getResources().getString(R.string.hasnotlogin), Toast.LENGTH_SHORT).show();
                            BLAcountToAli.getInstance().cleanUserInfo();
                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.setClass(mActivity, AccountMainActivity.class);
                            mActivity.startActivity(intent);
                        }
                    });
                }

            }
        }
        return baseResult;
    }


    public synchronized BLBaseResult deleteDeviceToFamilyV2(String did) {
        if (currentFamily == null)
            return null;

        BLBaseResult baseResult = BLLet.Family.removeDeviceFromFamily(did, currentFamily.getFamilyId(), currentFamily.getFamilyVersion());
        if (baseResult != null) {
            int code = baseResult.getStatus();
            Logutils.log_d("deleteDeviceToFamily ：" + JSON.toJSONString(baseResult));
            if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                BLFamilyAllInfo familyAllInfo = queryFamilyInfoV2();
                if (familyAllInfo != null) {
                    currentFamilyAllInfo = familyAllInfo;
                    currentFamily = currentFamilyAllInfo.getFamilyInfo();
                }
            }
        }
        return baseResult;
    }


    public void readdBLNetWork() {
        if (currentFamilyAllInfo != null) {
            Logutils.log_d("addFamilyDeviceToNetWork from readdBLNetWork");
            addFamilyDeviceToNetWork(currentFamilyAllInfo);
        }
    }


    public synchronized BLModuleControlResult modifyFamilModuleNameV1(String modelID, String name) {
        if (currentFamilyAllInfo == null || currentFamily == null)
            return null;
        BLModuleControlResult result = null;
        List<BLFamilyModuleInfo> blFamilyModuleInfoList = currentFamilyAllInfo.getModuleInfos();
        if (blFamilyModuleInfoList != null && blFamilyModuleInfoList.size() > 0) {
            for (BLFamilyModuleInfo moduleInfo : blFamilyModuleInfoList) {
                if (moduleInfo.getModuleId().equals(modelID)) {

                    BLFamilyModuleInfo familyModuleInfo = new BLFamilyModuleInfo();
                    familyModuleInfo.setModuleId(moduleInfo.getModuleId());
                    familyModuleInfo.setRoomId(moduleInfo.getRoomId());
                    familyModuleInfo.setName(name);
                    familyModuleInfo.setFamilyId(moduleInfo.getFamilyId());
                    familyModuleInfo.setExtend(moduleInfo.getExtend());
                    familyModuleInfo.setFlag(moduleInfo.getFollowDev());
                    familyModuleInfo.setFollowDev(moduleInfo.getFollowDev());
                    familyModuleInfo.setIconPath(moduleInfo.getIconPath());
                    familyModuleInfo.setModuleDevs(moduleInfo.getModuleDevs());
                    familyModuleInfo.setOrder(moduleInfo.getOrder());
                    familyModuleInfo.setScenceType(moduleInfo.getScenceType());
                    familyModuleInfo.setModuleType(moduleInfo.getModuleType());

                    result = BLLet.Family.modifyModuleFromFamily(familyModuleInfo, currentFamily.getFamilyId(), currentFamily.getFamilyVersion());
                    if (result != null) {
                        Logutils.log_w("modifyFamilModuleName:" + JSON.toJSONString(result));
                        int code = result.getStatus();
                        if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                            moduleInfo.setName(name);
                            currentFamily.setFamilyVersion(result.getVersion());
                        } else if (code == Constants.BLErrorCode.NOLOGIN_CODE1
                                || code == Constants.BLErrorCode.NOLOGIN_CODE6) {
                            if (mActivity != null) {
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent intent = new Intent();
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        intent.setClass(mActivity, LoadingActivity.class);
                                        mActivity.startActivity(intent);
                                    }
                                });
                            }

                        } else if (code == Constants.BLErrorCode.NOLOGIN_CODE2
                                || code == Constants.BLErrorCode.NOLOGIN_CODE3
                                || code == Constants.BLErrorCode.NOLOGIN_CODE4
                                || code == Constants.BLErrorCode.NOLOGIN_CODE5) {
                            if (mActivity != null) {
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Logutils.log_w("AKErrorLoginTokenIllegalError");
                                        Toast.makeText(mActivity, mActivity.getResources().getString(R.string.hasnotlogin), Toast.LENGTH_SHORT).show();
                                        BLAcountToAli.getInstance().cleanUserInfo();
                                        Intent intent = new Intent();
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        intent.setClass(mActivity, AccountMainActivity.class);
                                        mActivity.startActivity(intent);
                                    }
                                });
                            }
                        }
                    }
                    break;
                }
            }
        }
        return result;
    }

    public synchronized BLModuleControlResult modifyFamilModuleNameV2(String modelID, String name) {
        if (currentFamilyAllInfo == null || currentFamily == null)
            return null;
        BLModuleControlResult result = null;
        List<BLFamilyModuleInfo> blFamilyModuleInfoList = currentFamilyAllInfo.getModuleInfos();
        if (blFamilyModuleInfoList != null && blFamilyModuleInfoList.size() > 0) {
            for (BLFamilyModuleInfo moduleInfo : blFamilyModuleInfoList) {
                if (moduleInfo.getModuleId().equals(modelID)) {

                    BLFamilyModuleInfo familyModuleInfo = new BLFamilyModuleInfo();
                    familyModuleInfo.setModuleId(moduleInfo.getModuleId());
                    familyModuleInfo.setRoomId(moduleInfo.getRoomId());
                    familyModuleInfo.setName(name);
                    familyModuleInfo.setFamilyId(moduleInfo.getFamilyId());
                    familyModuleInfo.setExtend(moduleInfo.getExtend());
                    familyModuleInfo.setFlag(moduleInfo.getFollowDev());
                    familyModuleInfo.setFollowDev(moduleInfo.getFollowDev());
                    familyModuleInfo.setIconPath(moduleInfo.getIconPath());
                    familyModuleInfo.setModuleDevs(moduleInfo.getModuleDevs());
                    familyModuleInfo.setOrder(moduleInfo.getOrder());
                    familyModuleInfo.setScenceType(moduleInfo.getScenceType());
                    familyModuleInfo.setModuleType(moduleInfo.getModuleType());

                    result = BLLet.Family.modifyModuleFromFamily(familyModuleInfo, currentFamily.getFamilyId(), currentFamily.getFamilyVersion());
                    if (result != null) {
                        Logutils.log_w("modifyFamilModuleName:" + JSON.toJSONString(result));
                        int code = result.getStatus();
                        if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                            moduleInfo.setName(name);
                            currentFamily.setFamilyVersion(result.getVersion());
                        }
                    }
                    break;
                }
            }
        }
        return result;
    }

    public synchronized BLModuleControlResult modifyFamilModuleIconV1(String modelID, File iconFile) {
        if (currentFamilyAllInfo == null || currentFamily == null)
            return null;
        BLModuleControlResult result = null;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userid", BLAcountToAli.getInstance().getBlUserInfo().getBl_userid());
        String body = jsonObject.toJSONString();
        String result_icon = BLLet.Family.familyMutipartPost("/ec4/v1/system/addpic", null, body, iconFile);
        if (result_icon != null) {
            Logutils.log_d("modifyFamilModuleIcon ：" + JSON.toJSONString(result_icon));
            BLSubmitPicResult blSubmitPicResult = JSON.parseObject(result_icon, BLSubmitPicResult.class);
            if (blSubmitPicResult != null && blSubmitPicResult.getStatus() == Constants.BLErrorCode.SUCCESS_CODE) {
                List<BLFamilyModuleInfo> blFamilyModuleInfoList = currentFamilyAllInfo.getModuleInfos();
                if (blFamilyModuleInfoList != null && blFamilyModuleInfoList.size() > 0) {
                    for (BLFamilyModuleInfo moduleInfo : blFamilyModuleInfoList) {
                        if (moduleInfo.getModuleId().equals(modelID)) {

                            BLFamilyModuleInfo familyModuleInfo = new BLFamilyModuleInfo();
                            familyModuleInfo.setModuleId(moduleInfo.getModuleId());
                            familyModuleInfo.setRoomId(moduleInfo.getRoomId());
                            familyModuleInfo.setName(moduleInfo.getName());
                            familyModuleInfo.setFamilyId(moduleInfo.getFamilyId());
                            familyModuleInfo.setExtend(moduleInfo.getExtend());
                            familyModuleInfo.setFlag(moduleInfo.getFollowDev());
                            familyModuleInfo.setFollowDev(moduleInfo.getFollowDev());
                            familyModuleInfo.setIconPath(blSubmitPicResult.getPicpath());
                            familyModuleInfo.setModuleDevs(moduleInfo.getModuleDevs());
                            familyModuleInfo.setOrder(moduleInfo.getOrder());
                            familyModuleInfo.setScenceType(moduleInfo.getScenceType());
                            familyModuleInfo.setModuleType(moduleInfo.getModuleType());
                            result = BLLet.Family.modifyModuleFromFamily(familyModuleInfo, currentFamily.getFamilyId(), currentFamily.getFamilyVersion());
                            if (result != null) {
                                Logutils.log_d("modifyFamilModuleIcon modifyModuleFromFamily ：" + JSON.toJSONString(result));
                                int code = result.getStatus();
                                if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                                    moduleInfo.setIconPath(blSubmitPicResult.getPicpath());
                                    currentFamily.setFamilyVersion(result.getVersion());
                                } else if (code == Constants.BLErrorCode.NOLOGIN_CODE1
                                        || code == Constants.BLErrorCode.NOLOGIN_CODE6) {
                                    if (mActivity != null) {
                                        mActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Intent intent = new Intent();
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                intent.setClass(mActivity, LoadingActivity.class);
                                                mActivity.startActivity(intent);
                                            }
                                        });
                                    }

                                } else if (code == Constants.BLErrorCode.NOLOGIN_CODE2
                                        || code == Constants.BLErrorCode.NOLOGIN_CODE3
                                        || code == Constants.BLErrorCode.NOLOGIN_CODE4
                                        || code == Constants.BLErrorCode.NOLOGIN_CODE5) {
                                    if (mActivity != null) {
                                        mActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Logutils.log_w("AKErrorLoginTokenIllegalError");
                                                Toast.makeText(mActivity, mActivity.getResources().getString(R.string.hasnotlogin), Toast.LENGTH_SHORT).show();
                                                BLAcountToAli.getInstance().cleanUserInfo();
                                                Intent intent = new Intent();
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                intent.setClass(mActivity, AccountMainActivity.class);
                                                mActivity.startActivity(intent);
                                            }
                                        });
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }


    public synchronized BLModuleControlResult modifyFamilModuleIconV2(String modelID, File iconFile) {
        if (currentFamilyAllInfo == null || currentFamily == null)
            return null;
        BLModuleControlResult result = null;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userid", BLAcountToAli.getInstance().getBlUserInfo().getBl_userid());
        String body = jsonObject.toJSONString();
        String result_icon = BLLet.Family.familyMutipartPost("/ec4/v1/system/addpic", null, body, iconFile);
        if (result_icon != null) {
            Logutils.log_d("modifyFamilModuleIcon ：" + JSON.toJSONString(result_icon));
            BLSubmitPicResult blSubmitPicResult = JSON.parseObject(result_icon, BLSubmitPicResult.class);
            if (blSubmitPicResult != null && blSubmitPicResult.getStatus() == Constants.BLErrorCode.SUCCESS_CODE) {
                List<BLFamilyModuleInfo> blFamilyModuleInfoList = currentFamilyAllInfo.getModuleInfos();
                if (blFamilyModuleInfoList != null && blFamilyModuleInfoList.size() > 0) {
                    for (BLFamilyModuleInfo moduleInfo : blFamilyModuleInfoList) {
                        if (moduleInfo.getModuleId().equals(modelID)) {

                            BLFamilyModuleInfo familyModuleInfo = new BLFamilyModuleInfo();
                            familyModuleInfo.setModuleId(moduleInfo.getModuleId());
                            familyModuleInfo.setRoomId(moduleInfo.getRoomId());
                            familyModuleInfo.setName(moduleInfo.getName());
                            familyModuleInfo.setFamilyId(moduleInfo.getFamilyId());
                            familyModuleInfo.setExtend(moduleInfo.getExtend());
                            familyModuleInfo.setFlag(moduleInfo.getFollowDev());
                            familyModuleInfo.setFollowDev(moduleInfo.getFollowDev());
                            familyModuleInfo.setIconPath(blSubmitPicResult.getPicpath());
                            familyModuleInfo.setModuleDevs(moduleInfo.getModuleDevs());
                            familyModuleInfo.setOrder(moduleInfo.getOrder());
                            familyModuleInfo.setScenceType(moduleInfo.getScenceType());
                            familyModuleInfo.setModuleType(moduleInfo.getModuleType());
                            result = BLLet.Family.modifyModuleFromFamily(familyModuleInfo, currentFamily.getFamilyId(), currentFamily.getFamilyVersion());
                            if (result != null) {
                                Logutils.log_d("modifyFamilModuleIcon modifyModuleFromFamily ：" + JSON.toJSONString(result));
                                int code = result.getStatus();
                                if (code == Constants.BLErrorCode.SUCCESS_CODE) {
                                    moduleInfo.setIconPath(blSubmitPicResult.getPicpath());
                                    currentFamily.setFamilyVersion(result.getVersion());
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    public List<BLFamilyDeviceInfo> getFamilyDeviceModelV1() {
        int family_code = existDefaultFamily();
        if (family_code == FAMILY_NO) {
            createDefaultFamily();
        }
        if (currentFamilyAllInfo != null) {
            return currentFamilyAllInfo.getDeviceInfos();
        }
        return null;
    }

    public List<BLFamilyDeviceInfo> getFamilyDeviceModelV2() {
        if (currentFamilyAllInfo != null) {
            return currentFamilyAllInfo.getDeviceInfos();
        }
        return null;
    }

    public List<BLFamilyModuleInfo> getFamilyModuleV1() {
        int family_code = existDefaultFamily();
        if (family_code == FAMILY_NO) {
            createDefaultFamily();
        }
        if (currentFamilyAllInfo != null) {
            return currentFamilyAllInfo.getModuleInfos();
        }
        return null;
    }


    public List<BLFamilyModuleInfo> getFamilyModuleV2() {
        if (currentFamilyAllInfo != null) {
            return currentFamilyAllInfo.getModuleInfos();
        }
        return null;
    }

    public boolean refreshFamilyDataV1() {
        int family_code = existDefaultFamily();
        if (family_code == FAMILY_NO) {
            BLFamilyInfo familyInfo = createDefaultFamily();
            if (familyInfo != null) {
                return true;
            }
        } else if (family_code == FAMILY_EXIST) {
            BLFamilyAllInfo familyAllInfo = queryFamilyInfoV1();
            if (familyAllInfo != null) {
                currentFamilyAllInfo = familyAllInfo;
                currentFamily = currentFamilyAllInfo.getFamilyInfo();
                return true;
            }
        }
        return false;
    }

    public boolean refreshFamilyDataV2() {
        BLFamilyAllInfo familyAllInfo = queryFamilyInfoV2();
        if (familyAllInfo != null) {
            currentFamilyAllInfo = familyAllInfo;
            currentFamily = currentFamilyAllInfo.getFamilyInfo();
            return true;
        }
        return false;
    }

    private void addFamilyDeviceToNetWork(BLFamilyAllInfo blFamilyAllInfo) {
        //Logutils.log_d("remove  All  Device from bllet");
        //BLLet.Controller.removeAllDevice();
        List<BLFamilyDeviceInfo> blFamilyDeviceInfoList = blFamilyAllInfo.getDeviceInfos();
        List<BLDNADevice>  localDeviceList=DeviceManager.getInstance().getLoaclWifiDeviceList();
        if (blFamilyDeviceInfoList != null) {
            for (BLFamilyDeviceInfo familyDeviceInfo : blFamilyDeviceInfoList) {
                boolean islocal=false;
                for(BLDNADevice bldnaDevice:localDeviceList){
                    String sdid=familyDeviceInfo.getsDid();
                    String did=familyDeviceInfo.getDid();
                    if(!TextUtils.isEmpty(sdid)){
                        if(did.equals(bldnaDevice.getpDid())&&sdid.equals(bldnaDevice.getDid())){
                            islocal=true;
                            break;
                        }
                    }else{
                        if(did.equals(bldnaDevice.getDid())){
                            islocal=true;
                            break;
                        }
                    }
                }
                if(islocal){
                    continue;
                }
                BLDNADevice bldnaDevice = new BLDNADevice();
                bldnaDevice.setDid(familyDeviceInfo.getDid());
                bldnaDevice.setMac(familyDeviceInfo.getMac());
                bldnaDevice.setId(familyDeviceInfo.getTerminalId());
                bldnaDevice.setKey(familyDeviceInfo.getAeskey());
                bldnaDevice.setLock(familyDeviceInfo.isLock());
                bldnaDevice.setPid(familyDeviceInfo.getPid());
                bldnaDevice.setpDid(familyDeviceInfo.getDid());
                bldnaDevice.setName(familyDeviceInfo.getName());
                bldnaDevice.setPassword(familyDeviceInfo.getPassword());
                bldnaDevice.setType(familyDeviceInfo.getType());
                bldnaDevice.setExtend(familyDeviceInfo.getExtend());
                Logutils.log_d("add dev to bldna:" + JSON.toJSONString(bldnaDevice));
                BLLet.Controller.addDevice(bldnaDevice);
            }
        }
    }

}
