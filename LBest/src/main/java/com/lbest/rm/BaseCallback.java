package com.lbest.rm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.aliyun.alink.sdk.net.anet.api.AError;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.utils.Logutils;

/**
 * Created by dell on 2017/10/26.
 */

public class BaseCallback {

    private Context mContext;

    public BaseCallback(Context mContext) {
        this.mContext = mContext;
    }

    public void callBack(int code, String msg, Object data){
        Logutils.log_d("BaseCallback: "+code+"  "+msg);
        if(code== AError.AKErrorSuccess||code== Constants.AliErrorCode.SUCCESS_CODE){
           // Logutils.log_w("AKErrorLoginTokenIllegalError");
//            BLAcountToAli.getInstance().cleanUserInfo();
//            Intent intent=new Intent();
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            intent.setClass(mContext,AccountMainActivity.class);
//            mContext.startActivity(intent);
//            return;
        }else{
            Logutils.log_w("AKErrorLoginTokenIllegalError");
            Toast.makeText(mContext,msg+":"+code,Toast.LENGTH_SHORT).show();
        }
    }
}
