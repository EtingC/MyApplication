package com.lbest.rm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.adapter.TitleMoreAdapter;
import com.lbest.rm.common.BLAlert;
import com.lbest.rm.common.BLBitmapUtils;
import com.lbest.rm.common.BLConstants;
import com.lbest.rm.common.BLFwVersionModule;
import com.lbest.rm.common.BLFwVersionParser;
import com.lbest.rm.common.BLStyleDialog;
import com.lbest.rm.common.OnSingleClickListener;
import com.lbest.rm.common.StorageUtils;
import com.lbest.rm.data.FwVersionInfo;
import com.lbest.rm.data.RefreshTokenResult;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.plugin.Timer;
import com.lbest.rm.plugin.data.NativeTitleInfo;
import com.lbest.rm.plugin.data.TitleBarInfo;
import com.lbest.rm.plugin.data.TitleBtnInfo;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.LoadingDialog;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.ConfigXmlParser;
import org.apache.cordova.CordovaInterfaceImpl;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewImpl;
import org.apache.cordova.engine.SystemWebChromeClient;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewClient;
import org.apache.cordova.engine.SystemWebViewEngine;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.constants.controller.BLDeviceState;
import cn.com.broadlink.sdk.result.account.BLBaseResult;

/**
 * 项目名称：BLEControlAppV4 <br>
 * 类名称：DNACordoveActivity <br>
 * 类描述： <br>
 * 创建人：YeJing <br>
 * 创建时间：2015-7-14 下午7:22:36 <br>
 * 修改人：YeJin <br>
 * 修改时间：2015-7-14 下午7:22:36 <br>
 * 修改备注：
 */
@SuppressLint("SetJavaScriptEnabled")
public class DNAH5Activity extends TitleActivity {
    private final String TAG = DNAH5Activity.class.getSimpleName();

    private CordovaWebView cordovaWebView;
    private SystemWebView mSystemWebView;
    private WebView webview;

    private LinearLayout mContentWebLayout;

    public FamilyDeviceModuleData mBlDeviceInfo;

    //    public final ArrayBlockingQueue<String> onPageFinishedUrl = new ArrayBlockingQueue<String>(5);
    //加载的URL地址
    public String mLoadUrl;

    private NativeTitleInfo mCordovaBtnHandler;

    private ValueCallback<Uri> mUploadMessage1;
    private ValueCallback<Uri[]> mUploadMessage2;
    private Uri mCapturedImageURI;

    private static final int FILECHOOSER_RESULTCODE = 4102;

    private String DevicePid;

    private BLFwVersionModule mBLFwVersionModule;

    public LoadingDialog dialog1;
    //public LoadingDialog dialog2;
    private boolean loadurltype = true;
    public Context context;
    private boolean isactivityrun = true;
    protected CordovaInterfaceImpl cordovaInterface = new CordovaInterfaceImpl(this) {

        @Override
        public Object onMessage(String id, Object data) {
//            if ("onPageFinished".equals(id)) {
//                onPageFinishedUrl.add((String) data);
//            }
            return super.onMessage(id, data);
        }
    };

    private BLAcountToAli blAcountToAli;

    public Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(isactivityrun) {
                switch (msg.what) {
                    case 1:
                        String h5Path;
                        h5Path = StorageUtils.getH5IndexPath(DevicePid);
                        mLoadUrl = "file:///" + h5Path;
                        if (dialog1.isshowing()) {
                            dialog1.close();
                        }
                        loadUrl();
                        break;
                    case 2:
                        if (dialog1.isshowing()) {
                            dialog1.close();
                        }
                        loadUrl();
                        break;
                    default:
                        break;
                }
            }
            super.handleMessage(msg);
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dna_h5_layout);
        setBackWhiteVisible();
        findView();
        context = DNAH5Activity.this;
        initData();

        if(loadurltype) {
            loadUrl();
        }

        initCamera();

        setListener();
        //checkDevFwVersion();

    }

    private void initData(){
        blAcountToAli=BLAcountToAli.getInstance();

        mBLFwVersionModule = new BLFwVersionParser();

        mBlDeviceInfo = (FamilyDeviceModuleData) getIntent().getSerializableExtra(Constants.INTENT_DEVICE);

        String url = getIntent().getStringExtra(BLConstants.INTENT_URL);
        String param = getIntent().getStringExtra(BLConstants.INTENT_PARAM);
        DevicePid = mBlDeviceInfo.getPid();
        Log.e(TAG, "H5 pid value:" + DevicePid);
        //如果传进来url 存在直接打开当前的url
        if(!TextUtils.isEmpty(url)){
            mLoadUrl = url;
        }else{
            String h5Path;
            h5Path = StorageUtils.getH5IndexPath(DevicePid);
            mLoadUrl = "file:///" + h5Path;
            if (!TextUtils.isEmpty(param)) {
                Log.e(TAG, "H5 extend value:" + param);
                mLoadUrl = mLoadUrl + param;
            }
            //UI包不存在的时候，下载UI包和脚本
            if(TextUtils.isEmpty(h5Path)){
                Log.e("mrmr", "h5Path isEmpty");
                Product.downloadBLUIAndScript(DevicePid);
                loadurltype = false;
                dialog1.show();
                DownloadThread2 thread2 = new DownloadThread2();
                thread2.start();
            }else{
                Log.e("mrmr", "h5Path isnotEmpty");
                File h5File=new File(h5Path);
                if(!h5File.exists()){
                    Log.e("mrmr", "h5File isnotexists");
                    Product.downloadBLUIAndScript(DevicePid);
                    loadurltype = false;
                    dialog1.show();
                    DownloadThread thread = new DownloadThread();
                    thread.start();
                }
            }

        }
    }

    private void loadUrl(){
        ConfigXmlParser parser = new ConfigXmlParser();
        parser.parse(context);
        mSystemWebView.setVisibility(View.VISIBLE);
        cordovaWebView.init(cordovaInterface, parser.getPluginEntries(), parser.getPreferences());
        cordovaWebView.loadUrl(mLoadUrl);

    }

    private void findView(){
        mContentWebLayout = (LinearLayout) findViewById(R.id.content_web_layout);

        mSystemWebView = (SystemWebView) findViewById(R.id.cordovaWebView);

        mSystemWebView.getSettings().setJavaScriptEnabled(true);
        WebSettings webSettings = mSystemWebView.getSettings();
        webSettings.setAppCacheEnabled(false);
        webSettings.setDomStorageEnabled(true);
        cordovaWebView = new CordovaWebViewImpl(new SystemWebViewEngine(mSystemWebView));
        mSystemWebView.setVisibility(View.GONE);
        dialog1 = new LoadingDialog(this,"UI下载中...");
    }

    private void setListener(){
        setRightButtonOnClickListener(R.drawable.btn_more, new OnSingleClickListener() {

            @Override
            public void doOnClick(View v) {
                HashMap<String, String> dataMap = new HashMap<String, String>();
                Intent intent = new Intent();
                intent.putExtra(BLConstants.INTENT_DEVICE, mBlDeviceInfo);
                intent.setClass(DNAH5Activity.this, DevicePropertyActivity.class);
                startActivityForResult(intent, 5);
            }
        });
    }

    private void initView(){
        if(mBlDeviceInfo != null && ((mCordovaBtnHandler == null || mCordovaBtnHandler.getTitleBar() == null
                || TextUtils.isEmpty(mCordovaBtnHandler.getTitleBar().getTitle())))){
            setTitle(mBlDeviceInfo.getModuleName());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSystemWebView.onPause();
        if (cordovaWebView != null) {
            cordovaWebView.handlePause(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSystemWebView.onResume();
        if (cordovaWebView != null) {
            cordovaWebView.handleResume(true);
        }
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cordovaWebView.clearHistory();

        if(cordovaWebView != null){
            cordovaWebView.handleDestroy();
        }

        mContentWebLayout.removeView(mSystemWebView);
        mSystemWebView.removeAllViews();
        mSystemWebView.destroy();
        mSystemWebView = null;
        cordovaWebView = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (cordovaWebView != null) {
            cordovaWebView.handleStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (cordovaWebView != null) {
            cordovaWebView.handleStop();
        }
    }

    @Override
    protected void back() {
        if(mCordovaBtnHandler != null  && mCordovaBtnHandler.getLeftButton() != null && mCordovaBtnHandler.getLeftButton().getHandler() != null){
            postJSHander(mCordovaBtnHandler.getLeftButton().getHandler());
        }else{
            appBack();
        }
    }

    private void appBack(){
        if(mSystemWebView.canGoBack()){
            mSystemWebView.goBack();
        } else{
            super.back();
        }
    }

    private CallbackContext mAuthCallbackContext;


    private void initCamera(){
        SystemWebViewEngine engine = (SystemWebViewEngine) cordovaWebView.getEngine();
        mSystemWebView.setWebViewClient(new SystemWebViewClient(engine) {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("tel:")) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } else {
                    return super.shouldOverrideUrlLoading(view, url);
                }
            }
        });

        mSystemWebView.setWebChromeClient(new SystemWebChromeClient(engine) {
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Toast.makeText(mSystemWebView.getContext(), message, Toast.LENGTH_LONG).show();
                result.confirm();
                return true;
            }

            private void openChooser(ValueCallback<?> uploadMsg, String... acceptType) {

                File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AndroidExampleFolder");
                if (!imageStorageDir.exists()) {
                    // Create AndroidExampleFolder at sdcard
                    imageStorageDir.mkdirs();
                }
                // Create camera captured image file path and name
                File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                mCapturedImageURI = Uri.fromFile(file);

                // Camera capture image intent
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);


                Intent chooser = new Intent(Intent.ACTION_GET_CONTENT);
                chooser.addCategory(Intent.CATEGORY_OPENABLE);
                if (acceptType.length > 0) {
                    String type = TextUtils.isEmpty(acceptType[0]) ? "*/*" : acceptType[0];
                    chooser.setType(type);
                }

                // Create file chooser intent
                Intent chooserIntent = Intent.createChooser(chooser, "图片选择");
                // Set camera intent to file chooser
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

                DNAH5Activity.this.startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage1 = uploadMsg;
                openChooser(uploadMsg);
            }

            public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUploadMessage1 = uploadMsg;
                openChooser(uploadMsg, acceptType);
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUploadMessage1 = uploadMsg;
                openChooser(uploadMsg, acceptType);
            }


            // file upload callback (Android 5.0 (API level 21) -- current) (public method)
            @SuppressWarnings("all")
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                mUploadMessage2 = filePathCallback;

                if (Build.VERSION.SDK_INT >= 21) {
                    String[] acceptTypes = fileChooserParams.getAcceptTypes();
                    openChooser(filePathCallback, acceptTypes);
                    return true;
                } else {
                    return false;
                }

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent date) {
        super.onActivityResult(requestCode, resultCode, date);
        if(resultCode == RESULT_OK && requestCode == 5){
            initView();
        }

        //回调授权信息
        if(resultCode == RESULT_OK && requestCode == 10){
            String ticket = date.getStringExtra(BLConstants.INTENT_VALUE);
            if(ticket != null && mAuthCallbackContext != null){
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("did", mBlDeviceInfo.getDid());
                    jsonObject.put("ticket", ticket);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mAuthCallbackContext.success(jsonObject.toString());
            }
        }else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (date != null) {
                    if (mUploadMessage1 != null) {
                        mUploadMessage1.onReceiveValue(date.getData());
                        mUploadMessage1 = null;
                    }
                    else if (mUploadMessage2 != null) {
                        Uri[] dataUris = null;

                        try {
                            if (date.getDataString() != null) {
                                dataUris = new Uri[] { Uri.parse(date.getDataString()) };
                            }
                            else {
                                if (Build.VERSION.SDK_INT >= 16) {
                                    if (date.getClipData() != null) {
                                        final int numSelectedFiles = date.getClipData().getItemCount();

                                        dataUris = new Uri[numSelectedFiles];

                                        for (int i = 0; i < numSelectedFiles; i++) {
                                            dataUris[i] = date.getClipData().getItemAt(i).getUri();
                                        }
                                    }
                                }
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        mUploadMessage2.onReceiveValue(dataUris);
                        mUploadMessage2 = null;
                    }
                }else if(mCapturedImageURI != null){
                    if (mUploadMessage1 != null) {
                        mUploadMessage1.onReceiveValue(mCapturedImageURI);
                    }else if(mUploadMessage2 != null){
                        mUploadMessage2.onReceiveValue(new Uri[]{mCapturedImageURI});
                    }
                }
            }
            else {
                if (mUploadMessage1 != null) {
                    mUploadMessage1.onReceiveValue(null);
                    mUploadMessage1 = null;
                }
                else if (mUploadMessage2 != null) {
                    mUploadMessage2.onReceiveValue(null);
                    mUploadMessage2 = null;
                }
            }
        }
    }

    public void pushHander(NativeTitleInfo handler){
        mCordovaBtnHandler = handler;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initTitleBar();
            }
        });
    }

    private void initTitleBar(){
        if(mCordovaBtnHandler != null){
            refreshTitleView(mCordovaBtnHandler.getTitleBar());

            refreshRightBtnView(mCordovaBtnHandler.getRightButtons());

            refreshLeftBtnView(mCordovaBtnHandler.getLeftButton());
        }
    }

    private void refreshLeftBtnView(TitleBtnInfo leftBtnInfo){
        if(leftBtnInfo != null){
            String icon = leftBtnInfo.getIcon();
            String text = leftBtnInfo.getText();
            String color = leftBtnInfo.getColor();
            final String hander = leftBtnInfo.getHandler();

            int textColor = parseColor(color);
            Drawable drawableIcon = parseJSDrableIcon(icon);
            setLeftButtonOnClickListener(text, textColor, drawableIcon, new OnSingleClickListener() {
                @Override
                public void doOnClick(View v) {
                    postJSHander(hander);
                }
            });
        }else{
            setBackWhiteVisible();
        }
    }

    private void refreshRightBtnView(final List<TitleBtnInfo> rightButtons){
        if(rightButtons == null || rightButtons.isEmpty()){
            setRightButtonGone();
            return;
        }

        //显示单个按钮
        if(rightButtons.size() == 1){
            TitleBtnInfo titleBtnInfo = rightButtons.get(0);
            String icon = titleBtnInfo.getIcon();
            String text = titleBtnInfo.getText();
            String color = titleBtnInfo.getColor();
            final String hander = titleBtnInfo.getHandler();

            int textColor = parseColor(color);
            Drawable drawableIcon = parseJSDrableIcon(icon);
            setRightButtonOnClickListener(text, textColor, drawableIcon, new OnSingleClickListener() {
                @Override
                public void doOnClick(View v) {
                    postJSHander(hander);
                }
            });
        }else{
            setMoreAdapter(new MoreAdapter(DNAH5Activity.this, rightButtons));
            setRightButtonOnClickListener(R.drawable.btn_more, new OnSingleClickListener() {

                @Override
                public void doOnClick(View v) {
                    showMoreSelectWindow();
                }
            });
        }
    }

    @Override
    public void onMoreItemClick(int position, Object object) {
        super.onMoreItemClick(position, object);
        TitleBtnInfo item = (TitleBtnInfo) object;
        postJSHander(item.getHandler());
    }

    private void refreshTitleView(TitleBarInfo titleBarInfo){
        String titleName = mBlDeviceInfo != null ? mBlDeviceInfo.getModuleName():"";
        if(titleBarInfo != null){

            if(titleBarInfo.isVisibility()){
                if(titleBarInfo.isPadding()){
                    setBodyNoPadding();
                }else{
                    setTitleBarVisble();
                }
            }else{
                setTitleBarGone();
            }

            //取出标题
            if(!TextUtils.isEmpty(titleBarInfo.getTitle())){
                titleName = titleBarInfo.getTitle();
            }

            //设置标题字体颜色
            int titleColor = parseColor(titleBarInfo.getColor());
            if(titleColor != 0){
                setTitleTextColor(titleColor);
            }

            //设置标题背景颜色
            try {
                String titleBgColor = titleBarInfo.getBackgroundColor();
                if(!TextUtils.isEmpty(titleBgColor)){
                    int titleBgColorValue = Color.parseColor(titleBgColor);
                    if(titleBgColorValue != 0){
                        setTitleBackgroundColor(titleBgColorValue);
                    }
                }
            }catch (Exception e){
                Log.e(TAG, "titleBgColor format err");
                Log.e(TAG, e.getMessage(), e);
            }
        }

        setTitle(titleName);
    }

    private int parseColor(String color){
        if(!TextUtils.isEmpty(color)){
            try {
                return Color.parseColor(color);
            }catch (Exception e){
                Log.e(TAG, "titleColor format err");
                Log.e(TAG, e.getMessage(), e);
            }
        }

        return 0;
    }

    private Drawable parseJSDrableIcon(String icon){
        if(!TextUtils.isEmpty(icon)){
            if(icon.equals("appBackIcon")){
                return getResources().getDrawable(R.drawable.btn_back_white);
            }else if(icon.equals("appPropertyIcon")){
                return getResources().getDrawable(R.drawable.icon_set_white);
            }else{
                String filePath = StorageUtils.getH5Folder(mBlDeviceInfo.getPid())
                        + (!icon.startsWith("/") ? "/" : "") + icon;
                Bitmap bitmap = BLBitmapUtils.getBitmapFromFile(new File(filePath));
                if(bitmap != null){
                    return new BitmapDrawable(getResources(), bitmap);
                }
            }
        }
        return null;
    }

    private void postJSHander(String hander){
        if(hander == null || hander.equals("appClose")){
            DNAH5Activity.this.finish();
        }else if(hander.equals("appBack")){
            appBack();
        }else if(hander.equals("appProperty")){
            Intent intent = new Intent();
            intent.putExtra(BLConstants.INTENT_DEVICE, mBlDeviceInfo);
            intent.setClass(DNAH5Activity.this, DevicePropertyActivity.class);
            startActivityForResult(intent, 5);
        }else{
            cordovaWebView.loadUrl(String.format("javascript:%s()", hander));
        }
    }

    private class MoreAdapter extends TitleMoreAdapter {
        private List<TitleBtnInfo> list;

        public MoreAdapter(Context context, List<TitleBtnInfo> list) {
            super(context);
            this.list = list;
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public void getView(int position, TitleViewHolder viewHolder) {
            viewHolder.moreIconView.setImageDrawable(parseJSDrableIcon(list.get(position).getIcon()));
            viewHolder.moreTextView.setText(list.get(position).getText());
        }
    }

    public class DownloadThread extends Thread{
        @Override
        public void run() {
            boolean type = true;
            while(type){
                String h5Path;
                h5Path = StorageUtils.getH5IndexPath(DevicePid);
                File h5File=new File(h5Path);
                if(h5File.exists()){
                    type = false;
                    Log.e("mrmr", "h5File isexists");
                }
                try {
                    DownloadThread.sleep(100);
                }catch (InterruptedException e){

                }
            }
            handler.sendEmptyMessage(2);
        }
    }

    public class DownloadThread2 extends Thread{
        @Override
        public void run() {
            boolean type = true;
            while(true) {
                try {
                    DownloadThread.sleep(600);
                } catch (InterruptedException e) {

                }
                String h5Path;
                h5Path = StorageUtils.getH5IndexPath(DevicePid);
                Log.e("mrmr", "h5Path = "+h5Path);
                if (!TextUtils.isEmpty(h5Path)) {
                    break;
                }
            }

            handler.sendEmptyMessageDelayed(1,1000);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            Log.e("mrmr", "KEYCODE_BACK");
            isactivityrun = false;
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

