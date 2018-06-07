package com.lbest.rm.view;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.lbest.rm.BuildConfig;
import com.lbest.rm.Constants;
import com.lbest.rm.R;
import com.lbest.rm.common.StorageUtils;

import java.io.File;

/**
 * Created by dell on 2017/11/1.
 */

public class ChoosePicPopwindow implements View.OnClickListener {

    private PopupWindow popupWindow;
    private Activity mActivity;
    private TextView bt_froomgallary;
    private TextView bt_takephoto;
    private TextView bt_cancle;

    public ChoosePicPopwindow(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public void showWindow(View parent, String data) {

        WindowManager m = mActivity.getWindowManager();
        Display d = m.getDefaultDisplay();  //为获取屏幕宽、高
        int width = (int) (d.getWidth() * 0.9);
        int height = (int) (d.getHeight() * 0.45);

        final View contentView = LayoutInflater.from(mActivity).inflate(R.layout.choosepicwindow_layout, null);
        popupWindow = new PopupWindow(contentView, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                backgroundAlpha(mActivity, 1.0f);
            }
        });
        popupWindow.setContentView(contentView);
        findView(contentView);
        setListener();
        popupWindow.setFocusable(true);
        int h = 0;
        if (isNavigationBarShow()) {
            h = getNavigationBarHeight();
        }
        h = h + 20;
        ColorDrawable dw = new ColorDrawable(0x00000000);
        popupWindow.setBackgroundDrawable(dw);
        backgroundAlpha(mActivity, 0.5f);
        popupWindow.showAtLocation(parent, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, h);
    }

    private void backgroundAlpha(Activity context, float bgAlpha) {
        WindowManager.LayoutParams lp = context.getWindow().getAttributes();
        lp.alpha = bgAlpha;
        context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        context.getWindow().setAttributes(lp);
    }

    public boolean isNavigationBarShow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Display display = mActivity.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            Point realSize = new Point();
            display.getSize(size);
            display.getRealSize(realSize);
            return realSize.y != size.y;
        } else {
            boolean menu = ViewConfiguration.get(mActivity).hasPermanentMenuKey();
            boolean back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
            if (menu || back) {
                return false;
            } else {
                return true;
            }
        }
    }

    private int getNavigationBarHeight() {
        Resources resources = mActivity.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        Log.v("dbw", "Navi height:" + height);
        return height;
    }

    public void dismissWindow() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    public boolean isShowing() {
        if (popupWindow != null) {
            return popupWindow.isShowing();
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Intent intent = null;
        switch (id) {
            case R.id.bt_froomgallary:
                intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                mActivity.startActivityForResult(intent, Constants.REQUESTCODE_FROMGALLERY);
                break;
            case R.id.bt_takephoto:
                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(StorageUtils.CACHE_FILE_PATH, Constants.TAKEICONPHOTO_NAME)));
                mActivity.startActivityForResult(intent, Constants.REQUESTCODE_FROMCAMERA);
                break;
            case R.id.family_bt_cancle:
                break;
        }
        if (popupWindow != null) {
            popupWindow.dismiss();
        }
    }

    private void findView(View rootView) {
        bt_froomgallary = (TextView) rootView.findViewById(R.id.bt_froomgallary);
        bt_takephoto = (TextView) rootView.findViewById(R.id.bt_takephoto);
        bt_cancle = (TextView) rootView.findViewById(R.id.family_bt_cancle);
    }

    private void setListener() {
        bt_froomgallary.setOnClickListener(this);
        bt_takephoto.setOnClickListener(this);
        bt_cancle.setOnClickListener(this);
    }
}
