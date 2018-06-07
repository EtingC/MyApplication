package com.lbest.rm;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.common.StorageUtils;
import com.lbest.rm.data.UpdateInfo;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.utils.downloadfile.ApkAccessor;
import com.lbest.rm.utils.downloadfile.DownloadAccessor;
import com.lbest.rm.utils.downloadfile.DownloadParameter;
import com.lbest.rm.utils.http.HttpAccessor;
import com.lbest.rm.utils.http.HttpGetAccessor;
import com.lbest.rm.view.InputTextView;

import java.io.File;

import cn.com.broadlink.sdk.result.account.BLBaseResult;

public class UpdateActivity extends Activity {
    public static final String VERSION_URL = "http://download.ibroadlink.com/soft/LBest/version.html";
    public static final int NOTIFICATION_ID_UPDATE = 1;
    private Toolbar toolbar;
    private TextView toolbar_title;


    private TextView tv_currentversion;
    private TextView tv_latestversion;
    private TextView bt_update;

    private UpdateInfo mUpdateInfo;
    private PackageInfo mPackageInfo;

    private UpdateTask mUpdateTask;
    private boolean isCheckUpdate=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        findview();
        initView();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isCheckUpdate){
            new CheckUpdateTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void findview() {

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);

        tv_currentversion = (TextView) findViewById(R.id.tv_currentversion);
        tv_latestversion = (TextView) findViewById(R.id.tv_latestversion);
        bt_update = (TextView) findViewById(R.id.bt_update);
    }

    private void initView() {
        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_version));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));

        PackageManager manager = UpdateActivity.this.getApplicationContext().getPackageManager();
        try {
            mPackageInfo = manager.getPackageInfo(UpdateActivity.this.getPackageName(), 0);
            String version = mPackageInfo.versionName;
            tv_currentversion.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateActivity.this.finish();
            }
        });

        bt_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              //检查版本号并更新
                if (mPackageInfo != null && mUpdateInfo != null &&
                        mPackageInfo.versionCode < mUpdateInfo.getVersion()) {
                    Toast.makeText(UpdateActivity.this, UpdateActivity.this.getText(R.string.start_update), Toast.LENGTH_SHORT).show();
                    updateApk(mUpdateInfo.getUrl());
                } else {
                    if (mPackageInfo == null) {
                        Toast.makeText(UpdateActivity.this, UpdateActivity.this.getText(R.string.systemInfo_error), Toast.LENGTH_SHORT).show();
                    } else if (mUpdateInfo == null) {
                        Toast.makeText(UpdateActivity.this, UpdateActivity.this.getText(R.string.updateInfo_error), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(UpdateActivity.this, UpdateActivity.this.getText(R.string.up_to_date), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    public void updateApk(String url) {
        if (mUpdateTask == null) {
            mUpdateTask = new UpdateTask();
            mUpdateTask.execute(url);
        }
    }

    public void stopUpdate() {
        if (mUpdateTask != null) {
            mUpdateTask.stop();
            mUpdateTask = null;
        }
    }

    /**
     * 应用更新检查
     */
    private class CheckUpdateTask extends AsyncTask<Void, Void, UpdateInfo> {

        @Override
        protected UpdateInfo doInBackground(Void... params) {
            isCheckUpdate=true;
            HttpGetAccessor httpGetAccessor = new HttpGetAccessor();
            return httpGetAccessor.execute(VERSION_URL, null, UpdateInfo.class);
        }

        @Override
        protected void onPostExecute(UpdateInfo updateInfo) {
            super.onPostExecute(updateInfo);
            isCheckUpdate=false;
            mUpdateInfo = updateInfo;
            if (mUpdateInfo != null) {
                tv_latestversion.setText(mUpdateInfo.getVersionName());
            } else {

            }
        }
    }


    /**
     * App更新下载
     */
    private class UpdateTask extends AsyncTask<String, Integer, File> {

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        private Notification mNotification;

        private ApkAccessor mApkAccessor;

        public Notification getNotification() {
            return mNotification;
        }

        @Override
        protected void onPreExecute() {
            mNotification = new Notification(R.mipmap.ic_launcher,
                    getString(R.string.start_update),
                    System.currentTimeMillis());
            mNotification.flags = Notification.FLAG_NO_CLEAR;

            mNotification.contentView = new RemoteViews(getPackageName(),
                    R.layout.notification_layout);

            mNotification.contentView.setTextViewText(R.id.notify_text,
                    getString(R.string.update_content, 0));
            mNotification.contentView.setProgressBar(R.id.notify_pb, 100, 0,
                    false);

            Intent notificationIntent = new Intent(UpdateActivity.this,
                    UpdateActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            mNotification.contentIntent = PendingIntent.getActivity(
                    UpdateActivity.this, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            mNotificationManager.notify(NOTIFICATION_ID_UPDATE,
                    mNotification);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mNotification.contentView.setTextViewText(R.id.notify_text,
                    getString(R.string.update_content, values[0]));
            mNotification.contentView.setProgressBar(R.id.notify_pb, 100,
                    values[0], false);
            mNotificationManager.notify(NOTIFICATION_ID_UPDATE,
                    mNotification);
        }

        @Override
        protected void onPostExecute(File result) {
            mNotificationManager.cancel(NOTIFICATION_ID_UPDATE);
            mUpdateTask = null;
            if (result != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(result),
                        "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
//                finish();
                System.exit(0);
            }
        }

        @Override
        protected File doInBackground(String... params) {

            DownloadParameter downloadParam = new DownloadParameter();

            downloadParam.setTempFilePath(StorageUtils.getTMPAppPath());
            downloadParam.setSaveFilePath(StorageUtils.getAppPath());

            mApkAccessor = new ApkAccessor(UpdateActivity.this);
            mApkAccessor.setOnProgressListener(new DownloadAccessor.OnProgressListener() {

                @Override
                public void onProgress(long progress, long total) {
                    publishProgress((int) (progress * 100 / total));
                }
            }, 2000);
            Boolean result = mApkAccessor.execute(params[0], downloadParam);

            publishProgress(100);

            return (result == null || result == false) ? null : new File(
                    StorageUtils.getAppPath());
        }

        public void stop() {
            if (mApkAccessor != null) {
                mApkAccessor.stop();
            }
            mNotificationManager.cancel(NOTIFICATION_ID_UPDATE);
        }
    }
}
