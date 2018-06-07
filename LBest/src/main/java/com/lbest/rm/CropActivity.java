package com.lbest.rm;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.lbest.rm.common.BLBitmapUtils;
import com.lbest.rm.common.BLFileUtils;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.common.StorageUtils;
import com.lbest.rm.view.CropImage.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Created by dell on 2017/11/1.
 */

public class CropActivity extends Activity{
    private CropImageView mCropImageView;
    private static final int WRITE_PERMISSION = 0x01;
    private int mAspectRatioX=160;
    private int mAspectRatioY=160;
    private static final int ROTATE_NINETY_DEGREES = 90;

    private Button mCancelButton;
    private Button mRotateButton;
    private Button mConfimButton;

    private String mImagePath;

    private int P_HEIGHT;
    private int P_WIDTH;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop_layout);

        mImagePath = getIntent().getStringExtra(Constants.INTENT_TAKEPHOTO_PATH);

        mCropImageView = (CropImageView) findViewById(R.id.cropImageView);
        mCropImageView.setFixedAspectRatio(true);
        mCropImageView.setAspectRatio(mAspectRatioX, mAspectRatioY);



        // 获得屏幕高度（像素）
       P_HEIGHT = getResources().getDisplayMetrics().heightPixels;
        // 获得屏幕宽度（像素）
       P_WIDTH  = getResources().getDisplayMetrics().widthPixels;
        DisplayMetrics dm = new DisplayMetrics();
        dm = getResources().getDisplayMetrics();
        int densityDPI = dm.densityDpi;
        //判断是不是手机被横屏之后，设备宽高获取错误。
        if(P_HEIGHT < P_WIDTH){
            int temp = P_HEIGHT;
            P_HEIGHT =P_WIDTH;
           P_WIDTH = temp;
        }



        if(mImagePath != null){
            mCropImageView.setImageBitmap(BLBitmapUtils.getBitmapFromFile(
                    new File(mImagePath),P_WIDTH*2 /3,P_HEIGHT*2 /3));
        }

        findView();

        setListener();

        requestWritePermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == WRITE_PERMISSION){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You must allow permission write external storage to your mobile device.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private void findView(){
        mCancelButton = (Button) findViewById(R.id.btn_cancel);
        mRotateButton = (Button) findViewById(R.id.btn_rotate);
        mConfimButton = (Button) findViewById(R.id.btn_save);
    }

    private void setListener(){
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mRotateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mCropImageView.rotateImage(ROTATE_NINETY_DEGREES);
            }
        });

        mConfimButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new editPicTask().execute();
            }
        });
    }


    private void requestWritePermission(){
        if (ActivityCompat.checkSelfPermission(CropActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CropActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }
    }

    class editPicTask extends AsyncTask<Void,Void,String>{
        private BLProgressDialog progressDialog;
        private Bitmap bitmap;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog=BLProgressDialog.createDialog(CropActivity.this);
            bitmap=mCropImageView.getCroppedImage();
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(String path) {
            super.onPostExecute(path);
            progressDialog.dismiss();
            if(TextUtils.isEmpty(path)){
                finish();
            }else{
                Intent intent = new Intent();
                intent.putExtra(Constants.INTENT_CROPPHOTOPATH, path);
                setResult(RESULT_OK, intent);
                finish();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                String path = StorageUtils.CACHE_FILE_PATH + File.separator
                        + System.currentTimeMillis() + ".png";

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);// (0 - 100)压缩文件
                File file = new File(path);
                byte[] bytes1 = stream.toByteArray();
                // 将图像的图片保存在本地
                BLFileUtils.saveBytesToFile(bytes1, file);
                return path;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
