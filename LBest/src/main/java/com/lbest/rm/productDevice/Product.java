package com.lbest.rm.productDevice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.webkit.DownloadListener;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.aliyun.alink.business.alink.ALinkBusinessEx;
import com.aliyun.alink.business.devicecenter.api.add.AddDeviceBiz;
import com.aliyun.alink.business.devicecenter.api.add.DeviceInfo;
import com.aliyun.alink.business.devicecenter.api.add.IAddDeviceListener;
import com.aliyun.alink.business.devicecenter.base.DCErrorCode;
import com.aliyun.alink.sdk.net.anet.api.AError;
import com.aliyun.alink.sdk.net.anet.api.transitorynet.TransitoryRequest;
import com.aliyun.alink.sdk.net.anet.api.transitorynet.TransitoryResponse;
import com.lbest.rm.AccountMainActivity;
import com.lbest.rm.BaseCallback;
import com.lbest.rm.Constants;
import com.lbest.rm.LoadingActivity;
import com.lbest.rm.MyApplication;
import com.lbest.rm.R;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.BLDevProfileInfo;
import com.lbest.rm.common.BLProfileTools;
import com.lbest.rm.common.StorageUtils;
import com.lbest.rm.data.ParamsMap;
import com.lbest.rm.data.ShowParamData;
import com.lbest.rm.data.ShowParamsCfg;
import com.lbest.rm.data.productInfo;
import com.lbest.rm.utils.Logutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.param.controller.BLDeviceConfigParam;
import cn.com.broadlink.sdk.param.family.BLFamilyModuleInfo;
import cn.com.broadlink.sdk.result.controller.BLDeviceConfigResult;
import cn.com.broadlink.sdk.result.controller.BLDownloadScriptResult;
import cn.com.broadlink.sdk.result.controller.BLDownloadUIResult;
import cn.com.broadlink.sdk.result.controller.BLFirmwareVersionResult;
import cn.com.broadlink.sdk.result.controller.BLProfileStringResult;

/**
 * Created by dell on 2017/7/27.
 */

public class Product {
    private static String TAG = Product.class.getSimpleName();

    public static void setTAG(String TAG) {
        Product.TAG = TAG;
    }

    private static List<String> downloadScriptList = new ArrayList<>();
    private static List<String> downloadUIList = new ArrayList<>();
    private static ConcurrentHashMap<String, ParamsMap> paramsMapConcurrentHashMap = new ConcurrentHashMap<>();


    private static ShowParamsCfg showParamsCfg;

    /**
     * 产品列表回调接口
     * onSuccess 成功回调  data:产品列表
     * onFailed  失败回调  errorCode:错误码
     */
    public abstract static class ProductResultCallBack extends BaseCallback {
        public ProductResultCallBack(Context mContext) {
            super(mContext);
        }
    }

    public static TransitoryRequest getRequest(String method) {
        return getRequest(method, null);
    }

    public static TransitoryRequest getRequest(String method, final Map<String, String> param) {
        TransitoryRequest transitoryRequest = new TransitoryRequest();
        transitoryRequest.setMethod(method);
        if (param != null && param.size() > 0) {
            Iterator<String> iterator = param.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = param.get(key);
                transitoryRequest.putParam(key, value);
            }
        }
        return transitoryRequest;
    }

    /**
     * 获取后台已发布的产品列表
     */
    public static void getProductList(final ProductResultCallBack productResultCallBack) {
        TransitoryRequest transitoryRequest = getRequest(AliDeviceController.METHOD_PRODUCTLIST);
        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                //Log.i("BindTaobaoActivity", "onSuccess");
                if (productResultCallBack != null) {
                    List<productInfo> productList = new ArrayList<productInfo>();
                    int code = transitoryResponse.getError().getSubCode();
                    String msg = transitoryResponse.getError().getSubMsg();
                    productResultCallBack.callBack(code, msg, transitoryResponse.data);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                //Log.i("BindTaobaoActivity", "onFailed");
                if (productResultCallBack != null && aError != null) {
                    productResultCallBack.callBack(aError.getCode(), aError.getMsg(), null);
                }
            }
        });
    }


    /**
     * 获取已发布的产品信息
     */
    public static void getProductDetail(final String model, final ProductResultCallBack productListener) {
        HashMap<String, String> params = new HashMap<>();
        params.put("model", model);
        TransitoryRequest transitoryRequest = getRequest(AliDeviceController.METHOD_PRODUCTDETAIL, params);
        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                if (productListener != null) {
                    productInfo product = null;
                    int code = transitoryResponse.getError().getCode();
                    String msg = transitoryResponse.getError().getMsg();
                    if (transitoryResponse.data != null) {
                        try {
                            product = JSON.parseObject(transitoryResponse.data.toString().toString(), productInfo.class);
                        } catch (Exception e) {
                            Log.w(TAG, "parse product result exception:", e);
                        }
                    }
                    productListener.callBack(code, msg, product);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                if (productListener != null && aError != null) {
                    productListener.callBack(aError.getSubCode(), aError.getSubMsg(), null);
                }
            }
        });
    }

    /**
     * 产品配网接口
     */
    public static void configProductByBroadCast(final Context mContext, final String model, final IAddDeviceListener listener) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.model = model; // 产品型号
        AddDeviceBiz.getInstance().setDevice(deviceInfo);
        AddDeviceBiz.getInstance().startAddDevice(mContext, listener);
    }

    public static BLDeviceConfigResult configProductByBroadCastBL(final Activity mActivity, final String pid, final String ssid, final String password) {
        BLDeviceConfigParam easyConfigParam = new BLDeviceConfigParam();
        easyConfigParam.setSsid(ssid);
        easyConfigParam.setPassword(password);
        easyConfigParam.setGatewayaddr(getGateWay(mActivity));
        /***
         * 判断是第几代配网方式,
         * bit0: 置1 表示支持一代配网模式(SP1)
         * bit1: 置1 表示支持二代配网模式
         * bit2: 置1 表示支持三代配网模式
         */
        BLDevProfileInfo devProfileInfo = BLProfileTools.queryProfileInfoByPid(pid);
        if (devProfileInfo != null) {
            easyConfigParam.setVersion(((devProfileInfo.getWificonfigtype() >> 2) & 0x1) == 1 ? 3 : 2);
        }

        Logutils.log_w("configProductByBroadCastBL:" + JSON.toJSONString(easyConfigParam));
        BLDeviceConfigResult result = BLLet.Controller.deviceConfig(easyConfigParam);
        if (result != null) {
            Logutils.log_d("configProductByBroadCastBL:" + JSON.toJSONString(result));
            int code = result.getStatus();
            if (code == Constants.BLErrorCode.SUCCESS_CODE) {

            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE1
                    || code == Constants.BLErrorCode.NOLOGIN_CODE6) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent();
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.setClass(mActivity, LoadingActivity.class);
                        mActivity.startActivity(intent);
                    }
                });

            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE2
                    || code == Constants.BLErrorCode.NOLOGIN_CODE3
                    || code == Constants.BLErrorCode.NOLOGIN_CODE4
                    || code == Constants.BLErrorCode.NOLOGIN_CODE5) {
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
        return result;
    }

    public static ParamsMap getParamsMapByModel(String model) {
        String pid = getBLPid(model);
        return getParamsMapByPid(pid);
    }

    public static ParamsMap getParamsMapByPid(String pid) {
        ParamsMap paramsMap = null;
        if (TextUtils.isEmpty(pid)) {
            return paramsMap;
        }
        paramsMap = paramsMapConcurrentHashMap.get(pid);
        if (paramsMap == null) {

            BLProfileStringResult blProfileStringResult = BLLet.Controller.queryProfileByPid(pid);
            if (blProfileStringResult != null) {
                Logutils.log_d("getParamsMapByPid:" + JSON.toJSONString(blProfileStringResult));
            }
            if (blProfileStringResult != null && blProfileStringResult.getStatus() == Constants.BLErrorCode.SUCCESS_CODE) {
                String profileStr = blProfileStringResult.getProfile();
                Logutils.log_d(profileStr);
            }
            if (paramsMap != null) {
                if (paramsMapConcurrentHashMap.size() < 50) {
                    paramsMapConcurrentHashMap.put(pid, paramsMap);
                }
            }
        }
        return paramsMap;
    }


    public static ShowParamData getShowParamsByPid(Context mContext, String pid) {
        ShowParamData showParamData = null;
        if (TextUtils.isEmpty(pid)) {

        } else {
            if (showParamsCfg == null) {
                InputStreamReader inputReader = null;
                try {
                    InputStream is = mContext.getAssets().open("showstatue.txt");
                    inputReader = new InputStreamReader(is);
                    BufferedReader bufReader = new BufferedReader(inputReader);
                    String line = "";
                    String Result = "";
                    while ((line = bufReader.readLine()) != null) {
                        Result += line;
                    }

                    if (!Result.equals("")) {
                        showParamsCfg = JSON.parseObject(Result, ShowParamsCfg.class);
                    }
                    Logutils.log_i("初始化配置文件成功");
                } catch (Exception e) {
                    e.printStackTrace();
                    Logutils.log_e("初始化配置文件失败", e);
                }
            }

            if (showParamsCfg != null) {
                ArrayList<ShowParamData> showParamDataArrayList = showParamsCfg.getProduct_list();
                if (showParamDataArrayList != null) {
                    for (ShowParamData spd : showParamDataArrayList) {
                        if (spd.getPid().equals(pid)) {
                            showParamData = spd;
                            break;
                        }
                    }
                }
            }
        }
        return showParamData;

    }


    public static BLFirmwareVersionResult getFirmwareVersionV1(final Activity mActivity, String did) {
        BLFirmwareVersionResult result = BLLet.Controller.queryFirmwareVersion(did);
        if (result != null) {
            Logutils.log_d("getFirmwareVersion:" + JSON.toJSONString(result));
            int code = result.getStatus();
            if (code == Constants.BLErrorCode.SUCCESS_CODE) {

            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE1
                    || code == Constants.BLErrorCode.NOLOGIN_CODE6) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent();
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.setClass(mActivity, LoadingActivity.class);
                        mActivity.startActivity(intent);
                    }
                });

            } else if (code == Constants.BLErrorCode.NOLOGIN_CODE2
                    || code == Constants.BLErrorCode.NOLOGIN_CODE3
                    || code == Constants.BLErrorCode.NOLOGIN_CODE4
                    || code == Constants.BLErrorCode.NOLOGIN_CODE5) {
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
        return result;
    }


    public static BLFirmwareVersionResult getFirmwareVersionV2(final Activity mActivity, String did) {
        BLFirmwareVersionResult result = BLLet.Controller.queryFirmwareVersion(did);
        return result;
    }

    public static String getBLPid(String model) {
        String result = null;
        if (model != null) {
            try {
                String[] array = model.split("_");
                if (array != null) {
                    int lenght = array.length;
                    if (lenght > 1) {
                        int deviceType = Integer.parseInt(array[lenght - 1]);
                        result = getPidbyDevice(deviceType);
                    }
                }
            } catch (Exception e) {
                Logutils.log_w("", e);
            }
        }
        return result;
    }

    private static String getPidbyDevice(int devtype) {
        String s = to16pid(devtype);
        StringBuffer sb = new StringBuffer();
        sb.append("000000000000000000000000");
        sb.append(s);
        sb.append("0000");
        return sb.toString();
    }

    private static String to16pid(int b) {
        String s = Integer.toHexString(b);
        int lenth = s.length();
        if (lenth == 1) {
            s = "0" + s;
        }
        if (lenth > 4) {
            s = s.substring(lenth - 4, lenth);
        }
        StringBuffer sb = new StringBuffer();
        sb.append(s.charAt(2));
        sb.append(s.charAt(3));
        sb.append(s.charAt(0));
        sb.append(s.charAt(1));
        return sb.toString();
    }


    public static void stopConfigProductByBroadCast() {
        AddDeviceBiz.getInstance().stopAddDevice();
        BLLet.Controller.deviceConfigCancel();
    }

    public static void downloadBLUIAndScript(String pid) {
        downloadBLUI(pid);
        downloadBLScript(pid);
    }

    public static void downloadBLUI(String pid) {
        synchronized (Product.class) {
            if (!downloadUIList.contains(pid)) {
                downloadUIList.add(pid);
                new DownLoadBLUITask().executeOnExecutor(MyApplication.FULL_TASK_EXECUTOR, pid);
            }
        }
    }

    public static void downloadBLScript(String pid) {
        synchronized (Product.class) {
            if (!downloadScriptList.contains(pid)) {
                downloadScriptList.add(pid);
                new DownLoadBLScriptTask().executeOnExecutor(MyApplication.FULL_TASK_EXECUTOR, pid);
            }
        }
    }


    static class DownLoadBLScriptTask extends AsyncTask<String, Void, BLDownloadScriptResult> {
        String pid;

        @Override
        protected BLDownloadScriptResult doInBackground(String... params) {
            pid = params[0];
            BLDownloadScriptResult result = BLLet.Controller.downloadScript(pid);
            return result;
        }

        @Override
        protected void onPostExecute(BLDownloadScriptResult blDownloadScriptResult) {
            super.onPostExecute(blDownloadScriptResult);
            downloadScriptList.remove(pid);
            if (blDownloadScriptResult != null) {
                Logutils.log_d("DownLoadBLScriptTask:" + JSON.toJSONString(blDownloadScriptResult));
                if (blDownloadScriptResult.getStatus() == Constants.BLErrorCode.SUCCESS_CODE) {
                    Logutils.log_d("DownLoadBLScriptTask Success:" + pid);
                } else {
                    Logutils.log_d("DownLoadBLScriptTask Fail:" + blDownloadScriptResult.getMsg() + " " + blDownloadScriptResult.getStatus());
                }
            } else {
                Logutils.log_d("DownLoadBLScriptTask Fail:" + pid);
            }
        }
    }


    static class DownLoadBLUITask extends AsyncTask<String, Void, BLDownloadUIResult> {
        String pid;

        @Override
        protected BLDownloadUIResult doInBackground(String... params) {
            pid = params[0];
            BLDownloadUIResult result = BLLet.Controller.downloadUI(pid);
            return result;
        }

        @Override
        protected void onPostExecute(BLDownloadUIResult blDownloadUIResult) {
            super.onPostExecute(blDownloadUIResult);
            downloadUIList.remove(pid);
            if (blDownloadUIResult != null) {
                Logutils.log_d("DownLoadBLUITask:" + JSON.toJSONString(blDownloadUIResult));
                if (blDownloadUIResult.getStatus() == Constants.BLErrorCode.SUCCESS_CODE) {
                    Logutils.log_d("DownLoadBLUITask Success:" + pid);
                } else {
                    Logutils.log_d("DownLoadBLUITask Fail:" + blDownloadUIResult.getMsg() + " " + blDownloadUIResult.getStatus());
                }
            } else {
                Logutils.log_d("DownLoadBLUITask Fail:" + pid);
            }
        }
    }


    // 获取网关
    private static String getGateWay(Context mContext) {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        // dhcpInfo获取的是最后一次成功的相关信息，包括网关、ip等
        return Formatter.formatIpAddress(dhcpInfo.gateway);
    }
}
