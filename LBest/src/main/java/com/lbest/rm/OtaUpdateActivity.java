package com.lbest.rm;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.BLAlert;
import com.lbest.rm.common.BLConstants;
import com.lbest.rm.common.BLDialogOnClickListener;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.data.BaseDeviceInfo;
import com.lbest.rm.data.FwVersionInfo;
import com.lbest.rm.data.OTARequsetData;
import com.lbest.rm.data.OtaQueryResponseData;
import com.lbest.rm.data.OtaUpGradeProcessData;
import com.lbest.rm.data.RefreshTokenResult;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.productDevice.AliDeviceController;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.utils.http.HttpErrCode;
import com.lbest.rm.utils.http.HttpGetAccessor;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.constants.controller.BLDeviceState;
import cn.com.broadlink.sdk.result.BLBaseResult;
import cn.com.broadlink.sdk.result.controller.BLFirmwareVersionResult;

public class OtaUpdateActivity extends Activity {
    private final String GETOTAINFO_URL = "https://fwversions.ibroadlink.com/getfwversion";
    private Toolbar toolbar;
    private TextView toolbar_title;
    private TextView tv_currentversion;
    private TextView tv_latestversion;
    private TextView tv_upgradeinfo;
    private TextView bt_update;

    private FamilyDeviceModuleData baseDeviceInfo;
    private OtaQueryResponseData blotaData;
    private int currentVersion = -1;
    private int laseteVersion = -1;
    private String version_url;
    private boolean isCheck = false;

    private BLAcountToAli blAcountToAli;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otaupdate);

        initData();
        findview();
        initView();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isCheck) {
            new checkOTAVersionTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(baseDeviceInfo.getType()));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void initData() {
        blAcountToAli = BLAcountToAli.getInstance();
        baseDeviceInfo = (FamilyDeviceModuleData) getIntent().getSerializableExtra(Constants.INTENT_DEVICE);
    }

    private void findview() {

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        tv_upgradeinfo = (TextView) findViewById(R.id.tv_upgradeinfo);
        tv_currentversion = (TextView) findViewById(R.id.tv_currentversion);
        tv_latestversion = (TextView) findViewById(R.id.tv_latestversion);
        bt_update = (TextView) findViewById(R.id.bt_update);
    }

    private void initView() {
        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_deviceota));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));

        bt_update.setEnabled(false);
    }

    private void setListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OtaUpdateActivity.this.finish();
            }
        });

        bt_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String did = baseDeviceInfo.getDid();
                String url = version_url;
                new OTAUpgradeTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, did, url);
            }
        });
    }


    class checkOTAVersionTask extends AsyncTask<String, String, String> {
        private BLProgressDialog progressDialog;
        private RefreshTokenResult refreshTokenResult = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isCheck = true;
            progressDialog = BLProgressDialog.createDialog(OtaUpdateActivity.this);
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            tv_currentversion.setText(values[0]);
            try {
                currentVersion = Integer.parseInt(values[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        protected String doInBackground(String... params) {

            BLFirmwareVersionResult blFirmwareVersionResult = Product.getFirmwareVersionV2(OtaUpdateActivity.this, baseDeviceInfo.getDid());
            if (blFirmwareVersionResult != null) {
                if (blFirmwareVersionResult.getStatus() == Constants.BLErrorCode.SUCCESS_CODE) {
                    publishProgress(blFirmwareVersionResult.getVersion());
                    String deviceType = params[0];
                    OTARequsetData otaRequsetData = new OTARequsetData();
                    otaRequsetData.setDevicetype(deviceType);
                    HttpGetAccessor httpGetAccessor = new HttpGetAccessor();
                    String result = httpGetAccessor.execute(GETOTAINFO_URL, otaRequsetData);
                    Logutils.log_d("checkOTAVersionTask success");
                    return result;
                }
            }

            String old_refresh_token = blAcountToAli.getBlUserInfo().getRefresh_token();
            String old_access_token = blAcountToAli.getBlUserInfo().getAccess_token();
            Logutils.log_d("checkOTAVersionTask refreshAccessToken old_refresh_token:"+old_refresh_token+"   old_access_token:"+old_access_token);
            refreshTokenResult = blAcountToAli.refreshToken(old_refresh_token);
            if (refreshTokenResult.isSuccess()) {
                blFirmwareVersionResult = Product.getFirmwareVersionV1(OtaUpdateActivity.this, baseDeviceInfo.getDid());
                if (blFirmwareVersionResult != null) {
                    if (blFirmwareVersionResult.getStatus() == Constants.BLErrorCode.SUCCESS_CODE) {
                        publishProgress(blFirmwareVersionResult.getVersion());
                        String deviceType = params[0];
                        OTARequsetData otaRequsetData = new OTARequsetData();
                        otaRequsetData.setDevicetype(deviceType);
                        HttpGetAccessor httpGetAccessor = new HttpGetAccessor();
                        String result = httpGetAccessor.execute(GETOTAINFO_URL, otaRequsetData);
                        return result;
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            isCheck = false;
            if (OtaUpdateActivity.this.isFinishing()) {
                return;
            }
            progressDialog.dismiss();

            if (refreshTokenResult!=null&&!refreshTokenResult.isSuccess()) {
                cn.com.broadlink.sdk.result.account.BLBaseResult baseResult = refreshTokenResult.getResult();
                int code = refreshTokenResult.getCode();
                if (code == -3) {
                    if (baseResult != null) {
                        Toast.makeText(OtaUpdateActivity.this, "error:" + baseResult.getError(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(OtaUpdateActivity.this, OtaUpdateActivity.this.getResources().getString(R.string.err_network), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (code == -1) {
                        Toast.makeText(OtaUpdateActivity.this, OtaUpdateActivity.this.getResources().getString(R.string.str_loginfail), Toast.LENGTH_SHORT).show();
                    } else if (code == -2) {
                        Toast.makeText(OtaUpdateActivity.this, "token is null", Toast.LENGTH_SHORT).show();
                    }
//                    Intent intent = new Intent();
//                    intent.setClass(OtaUpdateActivity.this, AccountMainActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    OtaUpdateActivity.this.finish();
                }
                return;
            }

            if (!TextUtils.isEmpty(result)) {
                try {
                    JSONObject jsonObj = JSON.parseObject(result);
                    for (Map.Entry<String, Object> entry : jsonObj.entrySet()) {
                        Logutils.log_d(entry.getKey() + ":" + entry.getValue());
                        //if (entry.getKey().equals("44")) {
                        String versionInfo = String.valueOf(entry.getValue());
                        blotaData = JSON.parseObject(versionInfo, OtaQueryResponseData.class);
                        Logutils.log_d(JSON.toJSONString(blotaData));
                        if (blotaData != null) {
                            for (OtaQueryResponseData.versionInfo version : blotaData.getVersions()) {
                                try {
                                    int versionNum = Integer.parseInt(version.version);
                                    if (versionNum > laseteVersion) {
                                        laseteVersion = versionNum;
                                        version_url = version.url;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (laseteVersion != -1) {
                                tv_latestversion.setText(String.valueOf(laseteVersion));
                                if (laseteVersion > currentVersion) {
                                    bt_update.setEnabled(true);
                                }
                            }
                        }
                        break;
                        //}
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class OTAUpgradeTask extends AsyncTask<String, Void, BLBaseResult> {
        private BLProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = BLProgressDialog.createDialog(OtaUpdateActivity.this);
            progressDialog.show();
        }

        @Override
        protected BLBaseResult doInBackground(String... params) {
            String did = params[0];
            String url = params[1];
            return AliDeviceController.upgradeBLOta(did, url);
        }

        @Override
        protected void onPostExecute(BLBaseResult blBaseResult) {
            super.onPostExecute(blBaseResult);
            if (OtaUpdateActivity.this.isFinishing()) {
                return;
            }
            progressDialog.dismiss();
            if (blBaseResult != null) {
                Logutils.log_d("OTAUpgradeTask：" + JSON.toJSONString(blBaseResult));
                if (blBaseResult.succeed()) {
                    bt_update.setEnabled(false);
                    Toast.makeText(OtaUpdateActivity.this, getResources().getString(R.string.str_ota_upgrade), Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(OtaUpdateActivity.this, getResources().getString(R.string.str_ota_upgrade_fail) + ":" + blBaseResult.getStatus() + " " + blBaseResult.getMsg(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(OtaUpdateActivity.this, getResources().getString(R.string.str_ota_upgrade_fail), Toast.LENGTH_LONG).show();
            }
        }

    }
}
