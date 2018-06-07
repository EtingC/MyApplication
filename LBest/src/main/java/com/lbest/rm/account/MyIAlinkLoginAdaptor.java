package com.lbest.rm.account;

import android.content.Context;
import android.content.Intent;

import com.aliyun.alink.business.login.IAlinkLoginAdaptor;
import com.aliyun.alink.business.login.IAlinkLoginCallback;
import com.aliyun.alink.sdk.net.anet.api.AError;
import com.lbest.rm.AccountMainActivity;
import com.lbest.rm.account.broadlink.BLUserInfo;
import com.lbest.rm.utils.Logutils;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.result.account.BLLoginResult;

/**
 * Created by dell on 2017/10/20.
 */

public class MyIAlinkLoginAdaptor implements IAlinkLoginAdaptor {
    private final String AccountType="lbest";
    @Override
    public void login(Context context, IAlinkLoginCallback iAlinkLoginCallback) {
        Logutils.log_d("login-------");
        if(BLAcountToAli.getInstance().getBlUserInfo().getAccess_token()!=null){

            if (iAlinkLoginCallback != null) {
                iAlinkLoginCallback.onSuccess();
            }
        }else{
            iAlinkLoginCallback.onFailure(AError.AKErrorLoginTokenIllegalError,"Access_token is null");
        }
    }

    @Override
    public void logout(Context context, IAlinkLoginCallback iAlinkLoginCallback) {
        Logutils.log_d("logout");
        BLAcountToAli.getInstance().cleanUserInfo();
    }

    @Override
    public String getSessionID() {
        String token=null;
        if(BLAcountToAli.getInstance().getBlUserInfo().getAccess_token()!=null){
            token = BLAcountToAli.getInstance().getBlUserInfo().getAccess_token();
        }
        Logutils.log_d("get access_token："+token);
        return token;
    }

    @Override
    public String getUserID() {
        String userid=null;
        userid=BLAcountToAli.getInstance().getBlUserInfo().getBl_userid();
        Logutils.log_d("getUserID："+userid);
        return userid;
    }

    @Override
    public String getNickName() {
        String nickname=null;
        nickname=BLAcountToAli.getInstance().getBlUserInfo().getBl_nickname();
        Logutils.log_d("getNickName："+nickname);
        return nickname;
    }

    @Override
    public String getAvatarUrl() {
        String icon_url=null;
        icon_url=BLAcountToAli.getInstance().getBlUserInfo().getBl_icon();
        Logutils.log_d("getAvatarUrl："+icon_url);
        return icon_url;
    }

    @Override
    public boolean isLogin() {
        boolean isLogin=false;
        if(BLAcountToAli.getInstance().getBlUserInfo().getAccess_token() != null){
            isLogin=true;
        }
        Logutils.log_d("isLogin  "+isLogin);
        return isLogin;
    }

    @Override
    public void refreshSession(Context context, int i, IAlinkLoginCallback iAlinkLoginCallback) {
        Logutils.log_d("refreshSession");
//        BLAcountToAli.getInstance().cleanUserInfo();
//        Intent intent=new Intent();
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        intent.setClass(context,AccountMainActivity.class);
//        context.startActivity(intent);
    }

    @Override
    public void setInitResultCallback(IAlinkLoginCallback iAlinkLoginCallback) {
        Logutils.log_d("setInitResultCallback");
    }

    @Override
    public String getAccountType() {
        Logutils.log_d("getAccountType: "+AccountType);
        return AccountType;
    }
}
