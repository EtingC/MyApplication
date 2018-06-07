package com.lbest.rm;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.BLBitmapUtils;
import com.lbest.rm.common.BLConstants;
import com.lbest.rm.common.BLFileUtils;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.common.StorageUtils;
import com.lbest.rm.data.RefreshTokenResult;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.productDevice.AliDeviceController;
import com.lbest.rm.utils.ImageLoaderUtils;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.ChoosePicPopwindow;
import com.lbest.rm.view.DeleteDevicePopwindow;
import com.broadlink.lib.imageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;
import java.io.IOException;

import cn.com.broadlink.sdk.param.family.BLFamilyAllInfo;
import cn.com.broadlink.sdk.result.BLBaseResult;
import cn.com.broadlink.sdk.result.family.BLModuleControlResult;

public class DevicePropertyActivity extends AppCompatActivity implements View.OnClickListener {

    private Toolbar toolbar;
    private TextView toolbar_title;

    private RelativeLayout rl_modifydevicename;
    private RelativeLayout rl_modifyicon;
    private RelativeLayout rl_deviceota;
    private ImageView iv_deviceicon;
    private TextView tv_devicename;
    private TextView bt_deletedevice;

    private ImageLoaderUtils mImageLoaderUtils;
    private FamilyDeviceModuleData baseDeviceInfo;
    private ChoosePicPopwindow popupWindow;
    private BLProgressDialog mBLProgressDialog;
    private BLAcountToAli blAcountToAli;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_property);
        mImageLoaderUtils = ImageLoaderUtils.getInstence(getApplicationContext());

        initData();
        findview();
        initview();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == Constants.REQUESTCODE_MODIFYDEVICE) {
            baseDeviceInfo = (FamilyDeviceModuleData) data.getSerializableExtra(BLConstants.INTENT_DEVICE);
            refreshView();
        } else if (requestCode == Constants.REQUESTCODE_FROMCAMERA && resultCode == RESULT_OK) {
            Intent intent = new Intent();
            intent.putExtra(Constants.INTENT_TAKEPHOTO_PATH, StorageUtils.CACHE_FILE_PATH + File.separator + Constants.TAKEICONPHOTO_NAME);
            intent.setClass(DevicePropertyActivity.this, CropActivity.class);
            startActivityForResult(intent, Constants.REQUESTCODE_CROPIMAGE);
        } else if (requestCode == Constants.REQUESTCODE_CROPIMAGE && resultCode == RESULT_OK) {
            String path = data.getStringExtra(Constants.INTENT_CROPPHOTOPATH);
            File icon_file = new File(path);
            Bitmap bitmap = BLBitmapUtils.getBitmapFromFile(icon_file);
            bitmap = BLBitmapUtils.changBitmapSize(bitmap, 160, 160);
            String iconPath = StorageUtils.CACHE_FILE_PATH + File.separator + Constants.TAKEICONPHOTO_NAME;
            BLFileUtils.saveBitmapToFile(bitmap, StorageUtils.CACHE_FILE_PATH, Constants.TAKEICONPHOTO_NAME);
            new modifyIconTask().execute(iconPath);
        } else if (requestCode == Constants.REQUESTCODE_FROMGALLERY && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String iconPath = getPath(getApplicationContext(), uri);
            Intent intent = new Intent();
            intent.putExtra(Constants.INTENT_TAKEPHOTO_PATH, iconPath);
            intent.setClass(DevicePropertyActivity.this, CropActivity.class);
            startActivityForResult(intent, Constants.REQUESTCODE_CROPIMAGE);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.rl_modifydevicename:
                Intent intent1 = new Intent();
                intent1.putExtra(Constants.INTENT_DEVICE, baseDeviceInfo);
                intent1.setClass(DevicePropertyActivity.this, ModifyDeviceNameActivity.class);
                startActivityForResult(intent1, Constants.REQUESTCODE_MODIFYDEVICE);
                break;
            case R.id.rl_modifyicon:
                if (popupWindow == null) {
                    popupWindow = new ChoosePicPopwindow(DevicePropertyActivity.this);
                    popupWindow.showWindow(getWindow().getDecorView(), null);
                } else {
                    if (!popupWindow.isShowing()) {
                        popupWindow.showWindow(getWindow().getDecorView(), null);
                    }
                }
                break;
            case R.id.rl_deviceota:
                Intent intent3 = new Intent();
                intent3.putExtra(Constants.INTENT_DEVICE, baseDeviceInfo);
                intent3.setClass(DevicePropertyActivity.this, OtaUpdateActivity.class);
                startActivity(intent3);
                break;
            case R.id.bt_deletedevice:
                DeleteDevicePopwindow deleteDevicePopwindow = new DeleteDevicePopwindow(DevicePropertyActivity.this);
                deleteDevicePopwindow.showWindow(getWindow().getDecorView());
                deleteDevicePopwindow.setSurcBtlicklistener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new deletDeviceTask().execute(baseDeviceInfo.getDid());
                    }
                });
                break;
            default:
                break;
        }
    }

    private void initData() {
        blAcountToAli=BLAcountToAli.getInstance();
        baseDeviceInfo = (FamilyDeviceModuleData) getIntent().getSerializableExtra(BLConstants.INTENT_DEVICE);
    }

    private void findview() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);

        rl_modifydevicename = (RelativeLayout) findViewById(R.id.rl_modifydevicename);
        rl_modifyicon = (RelativeLayout) findViewById(R.id.rl_modifyicon);
        rl_deviceota = (RelativeLayout) findViewById(R.id.rl_deviceota);

        iv_deviceicon = (ImageView) findViewById(R.id.iv_deviceicon);
        tv_devicename = (TextView) findViewById(R.id.tv_devicename);
        bt_deletedevice = (TextView) findViewById(R.id.bt_deletedevice);
    }

    private void initview() {
        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar.setTitle("");
        toolbar_title.setText(getResources().getString(R.string.str_property));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));
        setSupportActionBar(toolbar);
        if (baseDeviceInfo != null) {
            tv_devicename.setText(baseDeviceInfo.getModuleName());
            String iconPath = baseDeviceInfo.getModuleIcon();
            if (iconPath.equals("file:///android_asset/lb1.png") || iconPath.contains("X23-X30")) {
                iconPath = "file:///android_asset/lb1.png";
                Bitmap bitmap = null;
                try {
                    String filename = iconPath.substring(22, iconPath.length());
                    bitmap = BitmapFactory.decodeStream(getAssets().open(filename));
                    iv_deviceicon.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                mImageLoaderUtils.displayImage(baseDeviceInfo.getModuleIcon(), iv_deviceicon, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        if (loadedImage == null) {
                            //((ImageView) view).setImageResource(R.drawable.default_module_icon);
                            //view.setTag(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_module_icon));
                        } else {
                            iv_deviceicon.setImageBitmap(loadedImage);
                            view.setTag(loadedImage);
                        }
                    }
                });
            }
        }
    }

    private void refreshView() {
        tv_devicename.setText(baseDeviceInfo.getModuleName());

        String iconPath = baseDeviceInfo.getModuleIcon();
        if (iconPath.equals("file:///android_asset/lb1.png") || iconPath.contains("X23-X30")) {
            iconPath = "file:///android_asset/lb1.png";
            Bitmap bitmap = null;
            try {
                String filename = iconPath.substring(22, iconPath.length());
                bitmap = BitmapFactory.decodeStream(getAssets().open(filename));
                iv_deviceicon.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mImageLoaderUtils.displayImage(baseDeviceInfo.getModuleIcon(), iv_deviceicon, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    if (loadedImage == null) {
                        //((ImageView) view).setImageResource(R.drawable.default_module_icon);
                        //view.setTag(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_module_icon));
                    } else {
                        iv_deviceicon.setImageBitmap(loadedImage);
                        view.setTag(loadedImage);
                    }
                }
            });
        }
    }

    private void setListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DevicePropertyActivity.this.finish();
            }
        });

        rl_modifydevicename.setOnClickListener(DevicePropertyActivity.this);
        rl_modifyicon.setOnClickListener(DevicePropertyActivity.this);
        rl_deviceota.setOnClickListener(DevicePropertyActivity.this);
        bt_deletedevice.setOnClickListener(DevicePropertyActivity.this);
    }


    class modifyIconTask extends AsyncTask<String, Void, BLModuleControlResult> {
        private final int SERVER_DEFAULT_PIC_SIZE = 512;
        String filePath = null;
        private RefreshTokenResult refreshTokenResult=null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mBLProgressDialog == null) {
                mBLProgressDialog = BLProgressDialog.createDialog(DevicePropertyActivity.this);
            }
            if (!mBLProgressDialog.isShowing()) {
                mBLProgressDialog.show();
            }
        }

        @Override
        protected BLModuleControlResult doInBackground(String... params) {
            filePath = params[0];
            File iconFile = new File(filePath);
            BLModuleControlResult result=AliDeviceController.moditfyDeviceIconV2(baseDeviceInfo.getModuleid(), iconFile);
            if(result==null||!result.succeed()){
                String old_refresh_token = blAcountToAli.getBlUserInfo().getRefresh_token();
                String old_access_token = blAcountToAli.getBlUserInfo().getAccess_token();
                Logutils.log_d("ModifyDeviceNameTask refreshAccessToken old_refresh_token:"+old_refresh_token+"   old_access_token:"+old_access_token);
                refreshTokenResult =  blAcountToAli.refreshToken(old_refresh_token);
                if(refreshTokenResult.isSuccess()){
                    return AliDeviceController.moditfyDeviceIconV1(baseDeviceInfo.getModuleid(), iconFile);
                }
                return null;
            }else{
                Logutils.log_d("ModifyDeviceNameTask success");
                return result;
            }
        }

        @Override
        protected void onPostExecute(BLModuleControlResult result) {
            super.onPostExecute(result);
            if(DevicePropertyActivity.this.isFinishing())
                return;

            mBLProgressDialog.dismiss();

            if(refreshTokenResult!=null&&!refreshTokenResult.isSuccess()){
                cn.com.broadlink.sdk.result.account.BLBaseResult baseResult=refreshTokenResult.getResult();
                int code=refreshTokenResult.getCode();
                if(code==-3){
                    if(baseResult!=null){
                        Toast.makeText(DevicePropertyActivity.this,"error:"+baseResult.getError(),Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(DevicePropertyActivity.this,DevicePropertyActivity.this.getResources().getString(R.string.err_network),Toast.LENGTH_SHORT).show();
                    }
                }else {
                    if(code==-1){
                        Toast.makeText(DevicePropertyActivity.this,DevicePropertyActivity.this.getResources().getString(R.string.str_loginfail),Toast.LENGTH_SHORT).show();
                    }else if(code==-2){
                        Toast.makeText(DevicePropertyActivity.this,"token is null",Toast.LENGTH_SHORT).show();
                    }
//                    Intent intent = new Intent();
//                    intent.setClass(DevicePropertyActivity.this, AccountMainActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    DevicePropertyActivity.this.finish();
                }
                return;
            }

            if (result != null) {
                if (result.succeed()) {
                    String moduleId = result.getModuleId();
                    baseDeviceInfo = AliDeviceController.getBLDeviceDetail(moduleId);
                    if (baseDeviceInfo != null) {

                        String iconPath = baseDeviceInfo.getModuleIcon();
                        if(!TextUtils.isEmpty(iconPath)){
                            if (iconPath.equals("file:///android_asset/lb1.png") || iconPath.contains("X23-X30")) {
                                iconPath = "file:///android_asset/lb1.png";
                                Bitmap bitmap = null;
                                try {
                                    String filename = iconPath.substring(22, iconPath.length());
                                    bitmap = BitmapFactory.decodeStream(getAssets().open(filename));
                                    iv_deviceicon.setImageBitmap(bitmap);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                mImageLoaderUtils.displayImage(baseDeviceInfo.getModuleIcon(), iv_deviceicon, new SimpleImageLoadingListener() {
                                    @Override
                                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                        if (loadedImage == null) {
                                            //((ImageView) view).setImageResource(R.drawable.default_module_icon);
                                            //view.setTag(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_module_icon));
                                        } else {
                                            Bitmap circle = BLBitmapUtils.toImageCircle(loadedImage);
                                            iv_deviceicon.setImageBitmap(circle);
                                            view.setTag(loadedImage);
                                        }
                                    }
                                });
                            }
                        }
                    }
                } else {
                    Toast.makeText(DevicePropertyActivity.this, result.getStatus() + "  " + result.getMsg(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(DevicePropertyActivity.this, getResources().getString(R.string.str_modifydeviceicon_fail), Toast.LENGTH_LONG).show();
            }
        }
    }


    class deletDeviceTask extends AsyncTask<String, Void, BLBaseResult> {
        private final int SERVER_DEFAULT_PIC_SIZE = 512;
        String did = null;
        private RefreshTokenResult refreshTokenResult=null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mBLProgressDialog == null) {
                mBLProgressDialog = BLProgressDialog.createDialog(DevicePropertyActivity.this);
            }
            if (!mBLProgressDialog.isShowing()) {
                mBLProgressDialog.show();
            }
        }

        @Override
        protected BLBaseResult doInBackground(String... params) {
            did = params[0];
            BLBaseResult result= BLAcountToAli.getInstance().delDeviveV2(did);
            if(result==null||!result.succeed()){
                String old_refresh_token = blAcountToAli.getBlUserInfo().getRefresh_token();
                String old_access_token = blAcountToAli.getBlUserInfo().getAccess_token();
                Logutils.log_d("deletDeviceTask refreshAccessToken old_refresh_token:"+old_refresh_token+"   old_access_token:"+old_access_token);
                refreshTokenResult =  blAcountToAli.refreshToken(old_refresh_token);
                if(refreshTokenResult.isSuccess()){
                    BLBaseResult baseresult = BLAcountToAli.getInstance().delDeviveV1(did);
                    if(baseresult == null || baseresult.getStatus() == -2014){
                        BLFamilyAllInfo familyAllInfo = BLAcountToAli.getInstance().queryfamilyInfoV2();
                        if(familyAllInfo == null){
                            return null;
                        }
                    }
                    return  BLAcountToAli.getInstance().delDeviveV1(did);
                }
                return null;
            }else{
                Logutils.log_d("deletDeviceTask success");
                return result;
            }
        }

        @Override
        protected void onPostExecute(BLBaseResult result) {
            super.onPostExecute(result);
            if(DevicePropertyActivity.this.isFinishing())
                return;

            mBLProgressDialog.dismiss();

            if(refreshTokenResult!=null&&!refreshTokenResult.isSuccess()){
                cn.com.broadlink.sdk.result.account.BLBaseResult baseResult=refreshTokenResult.getResult();
                int code=refreshTokenResult.getCode();
                if(code==-3){
                    if(baseResult!=null){
                        Toast.makeText(DevicePropertyActivity.this,"error:"+baseResult.getError(),Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(DevicePropertyActivity.this,DevicePropertyActivity.this.getResources().getString(R.string.err_network),Toast.LENGTH_SHORT).show();
                    }
                }else {
                    if(code==-1){
                        Toast.makeText(DevicePropertyActivity.this,DevicePropertyActivity.this.getResources().getString(R.string.str_loginfail),Toast.LENGTH_SHORT).show();
                    }else if(code==-2){
                        Toast.makeText(DevicePropertyActivity.this,"token is null",Toast.LENGTH_SHORT).show();
                    }
//                    Intent intent = new Intent();
//                    intent.setClass(DevicePropertyActivity.this, AccountMainActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    DevicePropertyActivity.this.finish();
                }
                return;
            }

            if (result != null) {
                if (result.succeed()) {
                    Intent intent = new Intent();
                    intent.setClass(DevicePropertyActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(Constants.INTENT_FRAGMENTINDEX, HomeActivity.DEVICELIST_INDEX);
                    startActivity(intent);
                    DevicePropertyActivity.this.finish();
                } else {
                    if(result.getStatus() == -2007){
                        Toast.makeText(DevicePropertyActivity.this, "设备已删除", Toast.LENGTH_LONG).show();
                    }else
                    Toast.makeText(DevicePropertyActivity.this, result.getStatus() + "  " + result.getMsg(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(DevicePropertyActivity.this, getResources().getString(R.string.str_deletedevice_fail), Toast.LENGTH_LONG).show();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
}
