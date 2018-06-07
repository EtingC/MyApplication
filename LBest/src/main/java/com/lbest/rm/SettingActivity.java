package com.lbest.rm;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.BLBitmapUtils;
import com.lbest.rm.common.BLFileUtils;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.common.StorageUtils;
import com.lbest.rm.data.RefreshTokenResult;
import com.lbest.rm.utils.ImageLoaderUtils;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.ChoosePicPopwindow;
import com.broadlink.lib.imageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.result.account.BLBaseResult;
import cn.com.broadlink.sdk.result.account.BLModifyUserIconResult;

public class SettingActivity extends AppCompatActivity {

    private RelativeLayout rl_modifynickename;
    private RelativeLayout rl_modifyicon;
    private RelativeLayout rl_modifypassword;
    private ImageView iv_faceicon;
    private TextView tv_nickname;
    private TextView bt_loginout;

    private Toolbar toolbar;
    private TextView toolbar_title;

    private ImageLoaderUtils mImageLoaderUtils;

    private ChoosePicPopwindow popupWindow;
    private BLProgressDialog mBLProgressDialog;

    private BLAcountToAli blAcountToAli;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        blAcountToAli=BLAcountToAli.getInstance();
        mImageLoaderUtils = ImageLoaderUtils.getInstence(getApplicationContext());

        findview();
        initView();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshview();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode ==  Constants.REQUESTCODE_FROMCAMERA && resultCode == RESULT_OK) {
            Intent intent = new Intent();
            intent.putExtra(Constants.INTENT_TAKEPHOTO_PATH, StorageUtils.CACHE_FILE_PATH+File.separator+ Constants.TAKEICONPHOTO_NAME);
            intent.setClass(SettingActivity.this, CropActivity.class);
            startActivityForResult(intent,Constants.REQUESTCODE_CROPIMAGE);
        }else  if (requestCode ==  Constants.REQUESTCODE_CROPIMAGE && resultCode == RESULT_OK) {
            String path=data.getStringExtra(Constants.INTENT_CROPPHOTOPATH);
            File icon_file= new File(path);
            Bitmap bitmap = BLBitmapUtils.getBitmapFromFile(icon_file);
            bitmap = BLBitmapUtils.changBitmapSize(bitmap, 160, 160);
            String iconPath = StorageUtils.CACHE_FILE_PATH + File.separator + Constants.TAKEICONPHOTO_NAME;
            BLFileUtils.saveBitmapToFile(bitmap, StorageUtils.CACHE_FILE_PATH , Constants.TAKEICONPHOTO_NAME);
            new modifyIconTask().execute(iconPath);
        }else  if (requestCode ==  Constants.REQUESTCODE_FROMGALLERY && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String iconPath=getPath(getApplicationContext(),uri);
            Intent intent = new Intent();
            intent.putExtra(Constants.INTENT_TAKEPHOTO_PATH, iconPath);
            intent.setClass(SettingActivity.this, CropActivity.class);
            startActivityForResult(intent,Constants.REQUESTCODE_CROPIMAGE);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void findview(){
        rl_modifynickename=(RelativeLayout)findViewById(R.id.rl_modifynickename);
        rl_modifyicon=(RelativeLayout)findViewById(R.id.rl_modifyicon);
        rl_modifypassword=(RelativeLayout)findViewById(R.id.rl_modifypassword);

        iv_faceicon=(ImageView)findViewById(R.id.iv_faceicon);
        tv_nickname=(TextView)findViewById(R.id.tv_nickname);

         bt_loginout=(TextView)findViewById(R.id.bt_loginout);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);

    }

    private void refreshview(){
        tv_nickname.setText(BLAcountToAli.getInstance().getBlUserInfo().getBl_nickname());

        mImageLoaderUtils.displayImage(BLAcountToAli.getInstance().getBlUserInfo().getBl_icon(), iv_faceicon, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if(loadedImage == null){
                    //((ImageView) view).setImageResource(R.drawable.default_module_icon);
                    //view.setTag(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_module_icon));
                }else{
                    Bitmap circle=BLBitmapUtils.toImageCircle(loadedImage);
                    iv_faceicon.setImageBitmap(circle);
                    view.setTag(loadedImage);
                }
            }
        });
    }

    private void setListener(){
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingActivity.this.finish();
            }
        });

        rl_modifynickename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.setClass(SettingActivity.this, ModifyNicknameActivity.class);
                startActivity(intent);
            }
        });

        rl_modifyicon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(popupWindow==null){
                    popupWindow=new ChoosePicPopwindow(SettingActivity.this);
                    popupWindow.showWindow(getWindow().getDecorView(),null);
                }else{
                    if(!popupWindow.isShowing()){
                        popupWindow.showWindow(getWindow().getDecorView(),null);
                    }
                }
            }
        });

        rl_modifypassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.setClass(SettingActivity.this, ModifyPasswordActivity.class);
                startActivity(intent);
            }
        });

        bt_loginout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BLAcountToAli.getInstance().cleanUserInfo();
                Intent intent=new Intent();
//                intent.setClass(SettingActivity.this,LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.setClass(SettingActivity.this,AccountMainActivity.class);
                startActivity(intent);
                SettingActivity.this.finish();
            }
        });
    }


    private void initView(){
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_setting));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));

        tv_nickname.setText(BLAcountToAli.getInstance().getBlUserInfo().getBl_nickname());

        mImageLoaderUtils.displayImage(BLAcountToAli.getInstance().getBlUserInfo().getBl_icon(), iv_faceicon, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if(loadedImage == null){
                    //((ImageView) view).setImageResource(R.drawable.default_module_icon);
                    //view.setTag(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_module_icon));
                }else{
                    Bitmap circle=BLBitmapUtils.toImageCircle(loadedImage);
                    iv_faceicon.setImageBitmap(circle);
                    view.setTag(loadedImage);
                }
            }
        });
    }


    class modifyIconTask extends AsyncTask<String,Void,BLModifyUserIconResult>{
        String filePath=null;
        private RefreshTokenResult refreshTokenResult=null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(mBLProgressDialog==null){
                mBLProgressDialog=BLProgressDialog.createDialog(SettingActivity.this);
            }
            if(!mBLProgressDialog.isShowing()){
                mBLProgressDialog.show();
            }
        }

        @Override
        protected BLModifyUserIconResult doInBackground(String... params) {
            filePath=params[0];

            BLModifyUserIconResult result=blAcountToAli.modifyUserIcon(new File(filePath));
            if(result==null||!result.succeed()){
                String old_refresh_token = blAcountToAli.getBlUserInfo().getRefresh_token();
                String old_access_token = blAcountToAli.getBlUserInfo().getAccess_token();
                Logutils.log_d("modifyIconTask refreshAccessToken old_refresh_token:"+old_refresh_token+"   old_access_token:"+old_access_token);
                refreshTokenResult =  blAcountToAli.refreshToken(old_refresh_token);
                if(refreshTokenResult.isSuccess()){
                    return blAcountToAli.modifyUserIcon(new File(filePath));
                }
                return null;
            }else{
                Logutils.log_d("modifyIconTask success");
                return result;
            }
        }

        @Override
        protected void onPostExecute(BLModifyUserIconResult result) {
            super.onPostExecute(result);
            if(SettingActivity.this==null||SettingActivity.this.isFinishing())
                return;

            mBLProgressDialog.dismiss();

            if(refreshTokenResult!=null&&!refreshTokenResult.isSuccess()){
                BLBaseResult baseResult=refreshTokenResult.getResult();
                int code=refreshTokenResult.getCode();
                if(code==-3){
                    if(baseResult!=null){
                        Toast.makeText(SettingActivity.this,"error:"+baseResult.getError(),Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(SettingActivity.this,SettingActivity.this.getResources().getString(R.string.err_network),Toast.LENGTH_SHORT).show();
                    }
                }else {
                    if(code==-1){
                        Toast.makeText(SettingActivity.this,SettingActivity.this.getResources().getString(R.string.str_loginfail),Toast.LENGTH_SHORT).show();
                    }else if(code==-2){
                        Toast.makeText(SettingActivity.this,"token is null",Toast.LENGTH_SHORT).show();
                    }
//                    Intent intent = new Intent();
//                    intent.setClass(SettingActivity.this, AccountMainActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    SettingActivity.this.finish();
                }
                return;
            }

            if(result!=null){
                if(result.succeed()){
                    String icon_path=result.getIcon();
                    BLAcountToAli.getInstance().saveUserIcon(icon_path);

                    mImageLoaderUtils.displayImage(result.getIcon(), iv_faceicon, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            if(loadedImage == null){
                                //((ImageView) view).setImageResource(R.drawable.default_module_icon);
                                //view.setTag(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_module_icon));
                            }else{
                                Bitmap circle=BLBitmapUtils.toImageCircle(loadedImage);
                                iv_faceicon.setImageBitmap(circle);
                                view.setTag(loadedImage);
                            }
                        }
                    });


                }else{
                    Toast.makeText(SettingActivity.this,result.getError()+"  "+result.getMsg(),Toast.LENGTH_LONG).show();
                }
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
