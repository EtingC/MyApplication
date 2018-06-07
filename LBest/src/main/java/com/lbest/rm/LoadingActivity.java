package com.lbest.rm;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.aliyun.alink.business.login.IAlinkLoginCallback;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.account.broadlink.BLUserInfo;
import com.lbest.rm.data.RefreshTokenResult;
import com.lbest.rm.utils.Logutils;

import java.util.ArrayList;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.result.account.BLBaseResult;
import cn.com.broadlink.sdk.result.account.BLGetUserInfoResult;
import cn.com.broadlink.sdk.result.account.BLLoginResult;
import cn.com.broadlink.sdk.result.account.BLOauthResult;

public class LoadingActivity extends Activity {

    private BLAcountToAli blAcountToAli;
    private BLUserInfo blUserInfo;
    private Handler mHandler;
    private boolean isClose = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        mHandler = new Handler();
        blAcountToAli = BLAcountToAli.getInstance();
        blUserInfo = blAcountToAli.getBlUserInfo();

    }


    @Override
    protected void onResume() {
        super.onResume();
        isClose = false;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isClose) {
                    return;
                }

                final Intent intent = new Intent();
                String old_refresh_token = blAcountToAli.getBlUserInfo().getRefresh_token();
                String old_access_token = blAcountToAli.getBlUserInfo().getAccess_token();
                Logutils.log_d("loading refreshAccessToken old_refresh_token:"+old_refresh_token+"   old_access_token:"+old_access_token);

                if (!TextUtils.isEmpty(old_refresh_token)) {
                    BLLoginResult loginresult = new BLLoginResult();
                    loginresult.setLoginsession(blUserInfo.getBl_loginsession());
                    loginresult.setUserid(blUserInfo.getBl_userid());
                    BLLet.Account.localLogin(loginresult);

                    new refreshTokenTask().execute(old_refresh_token);
                } else {
                    intent.setClass(LoadingActivity.this, AccountMainActivity.class);
                    startActivity(intent);
                    LoadingActivity.this.finish();
                }
            }

        }, 2000);
    }


    @Override
    protected void onPause() {
        super.onPause();
        isClose = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private class refreshTokenTask extends AsyncTask<String, Void, Integer> {
        BLBaseResult baseResult=null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected Integer doInBackground(String... strings) {
            int result=0;
            String oldtoken=strings[0];
            RefreshTokenResult refreshTokenResult = blAcountToAli.refreshToken(oldtoken);
            baseResult=refreshTokenResult.getResult();
            result=refreshTokenResult.getCode();
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

           final Intent intent = new Intent();
            if(result==0||result==-3){
                if(result==-3){
                    if(baseResult!=null){
                        Toast.makeText(LoadingActivity.this,"error:"+baseResult.getError(),Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(LoadingActivity.this,LoadingActivity.this.getResources().getString(R.string.err_network),Toast.LENGTH_SHORT).show();
                    }
                }

                BLAcountToAli.getInstance().login(new IAlinkLoginCallback() {
                    @Override
                    public void onSuccess() {
                        Logutils.log_d("login onSuccess");
                        intent.setClass(LoadingActivity.this, HomeActivity.class);
                        startActivity(intent);
                        LoadingActivity.this.finish();
                    }

                    @Override
                    public void onFailure(int i, String s) {
                        Logutils.log_d("login onFailure");
                        Toast.makeText(LoadingActivity.this, i + "   " + s, Toast.LENGTH_LONG).show();
                        //intent.setClass(LoadingActivity.this, LoginActivity.class);
                        intent.setClass(LoadingActivity.this, AccountMainActivity.class);
                        startActivity(intent);
                        LoadingActivity.this.finish();
                    }
                });
            }else{
                if(result==-1){
                    Toast.makeText(LoadingActivity.this,LoadingActivity.this.getResources().getString(R.string.str_loginfail),Toast.LENGTH_SHORT).show();
                }else if(result==-2){
                    Toast.makeText(LoadingActivity.this,"token is null",Toast.LENGTH_SHORT).show();
                }
//                intent.setClass(LoadingActivity.this, AccountMainActivity.class);
//                startActivity(intent);
//                LoadingActivity.this.finish();
            }
        }
    }
}
