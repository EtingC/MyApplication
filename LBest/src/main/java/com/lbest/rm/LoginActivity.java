package com.lbest.rm;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.aliyun.alink.business.login.IAlinkLoginCallback;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.account.broadlink.BLUserInfo;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.utils.Logutils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.result.account.BLGetUserInfoResult;
import cn.com.broadlink.sdk.result.account.BLLoginResult;

public class LoginActivity extends AppCompatActivity {

    private final String OAUTH_URL = "https://oauthbroadlinktest.ibroadlink.com/login.html";
    private WebView webview_login;
    private WebSettings webSettings;
    private final String client_id = "2a45b8562ef9c6f3e23643a4fe15281b";
    private String access_token;
    private String expires_in;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        findview();
        initView();

        try {
            String redirect_uri = URLEncoder.encode("https://797bea98e0a8cdf1291b200906497984oauth.ibroadlink.com", "UTF-8");
            //String url=OAUTH_URL+"/?response_type=token&client_id="+client_id+"&redirect_uri="+redirect_uri;
            String url = "https://797bea98e0a8cdf1291b200906497984oauth.ibroadlink.com/?response_type=token&client_id=2a45b8562ef9c6f3e23643a4fe15281b&redirect_uri=" + redirect_uri + "&mode=app";
            //String url="https://46236d6a58b0ed1bc6ed705b1975b194oauth.ibroadlink.com/?response_type=token&client_id=2a45b8562ef9c6f3e23643a4fe15281b&redirect_uri="+redirect_uri;
            webview_login.loadUrl(url);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private void findview() {
        webview_login = (WebView) findViewById(R.id.webview_login);
    }

    private void initView() {
        webSettings = webview_login.getSettings();
        //如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        webSettings.setJavaScriptEnabled(true);
        // 若加载的 html 里有JS 在执行动画等操作，会造成资源浪费（CPU、电量）
        // 在 onStop 和 onResume 里分别把 setJavaScriptEnabled() 给设置成 false 和 true 即可

        //设置自适应屏幕，两者合用
        webSettings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        //缩放操作
        webSettings.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        webSettings.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        webSettings.setDisplayZoomControls(false); //隐藏原生的缩放控件

        webview_login.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url.contains("access_token") && url.contains("expires_in")) {
                    Logutils.log_d("url:" + url);
                    int token_index = url.lastIndexOf("access_token");
                    int index = url.lastIndexOf("&");
                    access_token = url.substring(token_index + 13, index);
                    int expires_index = url.lastIndexOf("expires_in");
                    expires_in = url.substring(expires_index + 11, url.length());
                    Logutils.log_d("access_token:" + access_token + "  expires_in:" + expires_in);
                    if (access_token == null) {
                        Toast.makeText(LoginActivity.this, getResources().getString(R.string.gettoken_fail), Toast.LENGTH_LONG).show();
                    } else {
                        new loadUserInfoTask().execute(access_token);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onStop() {
        super.onStop();
        webSettings.setJavaScriptEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webview_login != null) {
            webview_login.removeAllViews();
            ((ViewGroup) webview_login.getParent()).removeView(webview_login);
            webview_login.setTag(null);
            webview_login.clearHistory();
            webview_login.destroy();
            webview_login = null;
        }
    }


    @Override
    public void onBackPressed() {
//        if(webview_login.canGoBack()){
//            webview_login.goBack();
//        }
        super.onBackPressed();
        LoginActivity.this.finish();
    }

    class loadUserInfoTask extends AsyncTask<String, Void, Boolean> {
        BLProgressDialog blProgressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            blProgressDialog = BLProgressDialog.createDialog(LoginActivity.this, null);
            blProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String token = params[0];
            BLLoginResult blLoginResult = BLLet.Account.oauthByIhc(token);
            if (blLoginResult != null && blLoginResult.succeed()) {
                ArrayList<String> useridList=new ArrayList<>();
                useridList.add(blLoginResult.getUserid());
                BLGetUserInfoResult userInfo=BLLet.Account.getUserInfo(useridList);
                String icon=null;
                if(userInfo!=null&& userInfo.getInfo()!=null&&userInfo.getInfo().size()>0){
                   icon=userInfo.getInfo().get(0).getIcon();
                }
               // BLAcountToAli.getInstance().saveUserInfo(access_token, expires_in, blLoginResult.getLoginsession(), blLoginResult.getNickname(), blLoginResult.getUserid(),icon);
                return true;
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            blProgressDialog.dismiss();
            if (aBoolean) {


                BLUserInfo blUserInfo=BLAcountToAli.getInstance().getBlUserInfo();
                BLLoginResult result = new BLLoginResult();
                result.setLoginsession(blUserInfo.getBl_loginsession());
                result.setUserid(blUserInfo.getBl_userid());;
                BLLoginResult loginres=BLLet.Account.localLogin(result);
                BLLet.DebugLog.on();
                if(loginres!=null){
                    Logutils.log_d(" loginactivity local login:"+ JSON.toJSONString(loginres));
                }else{
                    Logutils.log_d("loginactivity local login:null");
                }

                BLAcountToAli.getInstance().login(new IAlinkLoginCallback() {
                    @Override
                    public void onSuccess() {
                        Logutils.log_d("login onSuccess");

                        Intent intent=new Intent();
                        intent.setClass(LoginActivity.this,HomeActivity.class);
                        startActivity(intent);
                        LoginActivity.this.finish();
                    }

                    @Override
                    public void onFailure(int i, String s) {
                        Logutils.log_d("login onFailure");
                        Toast.makeText(LoginActivity.this, i + "   " + s, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(LoginActivity.this, getResources().getString(R.string.getuserinfo_fail), Toast.LENGTH_LONG).show();
            }
        }
    }
}
