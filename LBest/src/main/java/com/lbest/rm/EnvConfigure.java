package com.lbest.rm;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.aliyun.alink.bone.CdnEnv;
import com.aliyun.alink.business.alink.ALinkEnv;

/**
 * Created by huanyu.zhy on 17/6/6.
 *
 * 当前环境配置
 */
public class EnvConfigure {
    public  static ALinkEnv aLinkEnv;
    public  static CdnEnv cdnEnv;

    static public void init(Context context){
        try {
            SharedPreferences sp = context.getSharedPreferences("envConfig",0);
            if (sp!=null) {
                String alinkEnvConf = sp.getString("aLinkEnv",(String) null);
                if (!TextUtils.isEmpty(alinkEnvConf))
                    aLinkEnv = ALinkEnv.valueOf(alinkEnvConf);

                String cdnEnvConf = sp.getString("cdnEnv",(String) null);
                if (!TextUtils.isEmpty(cdnEnvConf))
                    cdnEnv = CdnEnv.valueOf(cdnEnvConf);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }


    static public void saveAlinkEnv(Context context,ALinkEnv env) {
        aLinkEnv = env;
        SharedPreferences sharedPreferences = context.getSharedPreferences("envConfig", 0);
        Editor editor = sharedPreferences.edit();
        editor.putString("aLinkEnv", env.name()).commit();
    }

    static public void saveCDNEnv(Context context,CdnEnv env) {
        cdnEnv = env;
        SharedPreferences sharedPreferences = context.getSharedPreferences("envConfig", 0);
        Editor editor = sharedPreferences.edit();
        editor.putString("cdnEnv", env.name()).commit();
    }
}
