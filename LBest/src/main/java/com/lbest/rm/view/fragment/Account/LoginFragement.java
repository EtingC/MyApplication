package com.lbest.rm.view.fragment.Account;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.aliyun.alink.business.login.IAlinkLoginCallback;
import com.lbest.rm.AccountMainActivity;
import com.lbest.rm.GetBackPasswordActivity;
import com.lbest.rm.HomeActivity;
import com.lbest.rm.R;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.account.broadlink.BLHttpErrCode;
import com.lbest.rm.account.broadlink.BLUserInfo;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.utils.CommonUtils;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.InputTextView;
import com.lbest.rm.view.fragment.LoginErrorPopwindow;

import java.util.ArrayList;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.result.account.BLGetUserInfoResult;
import cn.com.broadlink.sdk.result.account.BLLoginResult;
import cn.com.broadlink.sdk.result.account.BLOauthResult;

/**
 * Created by YeJin on 2017/4/19.
 */

public class LoginFragement extends AccountBaseFragemt {
    private EditText mAccoutView;

    private EditText mPasswordView;

    private TextView mLoginBtn;
    private TextView mForgotBtn;
    private ImageButton mPasswordeEyeBtn;

    private AccountMainActivity mActivity;

    private final String client_id = "0657c5f338ff5a06707126ef69ad7647";
//    private final String redirect_uri = "https://1497ee24cbf2367decc3f8edd04d3cf4oauth.ibroadlink.com";

    private final String redirect_uri = "http://localhost";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (AccountMainActivity) getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_fragment_layout, container, false);

        findView(view);

        setListener();

        initView();

        String sdkversion=BLLet.getSDKVersion();

        return view;
    }

    private void findView(View view) {
        mAccoutView = (EditText) view.findViewById(R.id.account_view);
        mPasswordView = (EditText) view.findViewById(R.id.password_view);

        mLoginBtn = (TextView) view.findViewById(R.id.btn_login);
        mForgotBtn = (TextView) view.findViewById(R.id.btn_forget);

        mPasswordeEyeBtn = (ImageButton) view.findViewById(R.id.password_eye_view);
    }

    private void setListener() {

        mPasswordeEyeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int inputtype=mPasswordView.getInputType();

                if (inputtype==InputType.TYPE_CLASS_TEXT) {
                    mPasswordeEyeBtn.setImageResource(R.drawable.password_invisiable);
                    mPasswordView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);

                } else {
                    mPasswordeEyeBtn.setImageResource(R.drawable.password_visiable);
                    mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT);
                }
            }
        });

        mAccoutView.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String password = mPasswordView.getText().toString();
                if (s.length() > 0 && (CommonUtils.isEmail(s.toString()) || CommonUtils.isPhone(s.toString()))
                        && (password.length() > 6 && !CommonUtils.strContainCNChar(s.toString()))) {
                    mLoginBtn.setEnabled(true);
                } else {
                    //mLoginBtn.setEnabled(false);
                }
            }
        });

        mPasswordView.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String account = mAccoutView.getText().toString();
                if (s.length() >= 6 && !CommonUtils.strContainCNChar(s.toString()) && (CommonUtils.isEmail(account) || CommonUtils.isPhone(account))) {
                    mLoginBtn.setEnabled(true);
                } else {
                   // mLoginBtn.setEnabled(false);
                }

                //mPasswordeEyeBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
        });

        //登陆按钮事件
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String account = mAccoutView.getText().toString();
                String password = mPasswordView.getText().toString();
                new LoginTask(getActivity()).execute(account, password,client_id,redirect_uri);

//                Intent intent=new Intent();
//                intent.setClass(mActivity, LoginActivity.class);
//                startActivity(intent);
//                mActivity.finish();
            }
        });

        mForgotBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mActivity, GetBackPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initView() {

    }

    /**
     * 用户登录异步线程
     ***/
    private class LoginTask extends AsyncTask<String, Void, Boolean> {

        private BLProgressDialog mBLProgressDialog;

        private String userName;
        private String userPassword;

        private String client_id;
        private String redirect_uri;
        private Activity mActivity;

        public LoginTask(Activity mActivity) {
            this.mActivity = mActivity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mBLProgressDialog = BLProgressDialog.createDialog(mActivity, getResources().getString(R.string.str_account_signin));
            mBLProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            userName = params[0];
            userPassword = params[1];
            client_id = params[2];
            redirect_uri = params[3];
            BLOauthResult blOauthResult = BLLet.Account.queryIhcAccessToken(userName, userPassword, client_id, redirect_uri);
            if (blOauthResult != null && blOauthResult.getError() == BLHttpErrCode.SUCCESS) {
                String access_token = blOauthResult.getAccessToken();
                String refresh_token = blOauthResult.getRefreshToken();
                int expires_in = blOauthResult.getExpires_in();
                Logutils.log_d("refresh_token:" + refresh_token +"     access_token:" + access_token + "    expires_in:" + expires_in);
                if (!TextUtils.isEmpty(access_token)) {
                    BLLoginResult blLoginResult = BLLet.Account.oauthByIhc(access_token);
                    if (blLoginResult != null && blLoginResult.succeed()) {
                        ArrayList<String> useridList = new ArrayList<>();
                        useridList.add(blLoginResult.getUserid());
                        BLGetUserInfoResult userInfo = BLLet.Account.getUserInfo(useridList);
                        String icon = null;
                        String nickName=null;
                        if (userInfo != null && userInfo.getInfo() != null && userInfo.getInfo().size() > 0) {
                            icon = userInfo.getInfo().get(0).getIcon();
                            nickName=userInfo.getInfo().get(0).getNickname();
                        }
                        BLAcountToAli.getInstance().saveUserInfo(refresh_token,access_token, String.valueOf(expires_in), blLoginResult.getLoginsession(), nickName, blLoginResult.getUserid(), icon);
                        return true;
                    } else {
                        Logutils.log_w(getResources().getString(R.string.getuserinfo_fail));
                       // Toast.makeText(mActivity, getResources().getString(R.string.getuserinfo_fail), Toast.LENGTH_LONG).show();
                        return false;
                    }
                } else {
                    Logutils.log_w(getResources().getString(R.string.gettoken_fail));
                    //Toast.makeText(mActivity, getResources().getString(R.string.gettoken_fail), Toast.LENGTH_LONG).show();
                    return false;
                }

            } else if (blOauthResult != null && blOauthResult.getError() != BLHttpErrCode.SUCCESS) {
                Logutils.log_w(getResources().getString(R.string.gettoken_fail) + ":" + blOauthResult.getError() + "   " + blOauthResult.getMsg());
               // Toast.makeText(mActivity, getResources().getString(R.string.gettoken_fail) + ":" + blOauthResult.getError() + "   " + blOauthResult.getMsg(), Toast.LENGTH_LONG).show();
            } else {
                Logutils.log_w(getResources().getString(R.string.str_err_network));
                //Toast.makeText(mActivity, getResources().getString(R.string.str_err_network), Toast.LENGTH_LONG).show();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            mBLProgressDialog.dismiss();
            if (result) {

                BLUserInfo blUserInfo=BLAcountToAli.getInstance().getBlUserInfo();
                BLLoginResult result2 = new BLLoginResult();
                result2.setLoginsession(blUserInfo.getBl_loginsession());
                result2.setUserid(blUserInfo.getBl_userid());
                BLLoginResult loginres=BLLet.Account.localLogin(result2);
                BLLet.DebugLog.on();
                if(loginres!=null){
                    Logutils.log_d(" loginfragment local login:"+ JSON.toJSONString(loginres));
                }else{
                    Logutils.log_d("loginfragment local login:null");
                }


                BLAcountToAli.getInstance().login(new IAlinkLoginCallback() {
                    @Override
                    public void onSuccess() {
                        Logutils.log_d("login onSuccess");
                        Intent intent = new Intent();
                        intent.setClass(getActivity(), HomeActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }

                    @Override
                    public void onFailure(int i, String s) {
                        Logutils.log_d("login onFailure");
                        Toast.makeText(getActivity(), i + "   " + s, Toast.LENGTH_LONG).show();
                    }
                });
            }else{
                LoginErrorPopwindow loginErrorPopwindow=new LoginErrorPopwindow(mActivity);
                loginErrorPopwindow.showWindow(mActivity.getWindow().getDecorView());
                //Toast.makeText(mActivity, getResources().getString(R.string.str_loginfail), Toast.LENGTH_LONG).show();
            }
        }
    }
}
