package com.lbest.rm.view;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.lbest.rm.Constants;
import com.lbest.rm.R;
import com.lbest.rm.common.StorageUtils;

import java.io.File;

/**
 * Created by dell on 2017/11/1.
 */

public class DeleteDevicePopwindow implements View.OnClickListener{

    private PopupWindow popupWindow;
    private Activity mActivity;
    private TextView bt_sure;
    private TextView bt_cancle;

    public View.OnClickListener surcBtlicklistener;

    public void setSurcBtlicklistener(View.OnClickListener surcBtlicklistener) {
        this.surcBtlicklistener = surcBtlicklistener;
    }

    public DeleteDevicePopwindow(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public void showWindow(View parent) {

        WindowManager m = mActivity.getWindowManager();
        Display d = m.getDefaultDisplay();  //为获取屏幕宽、高
        int width=(int) (d.getWidth() * 0.75);
        int height=(int) (d.getHeight() * 0.25);

        final View contentView = LayoutInflater.from(mActivity).inflate(R.layout.deletedevicewindow_layout, null);
        popupWindow = new PopupWindow(contentView,width, height,true);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                backgroundAlpha(mActivity,1.0f);
            }
        });
        popupWindow.setContentView(contentView);
        findView(contentView);
        setListener();
        popupWindow.setFocusable(true);
        int h=getNavigationBarHeight();
        ColorDrawable dw = new ColorDrawable(0x00000000);
        popupWindow.setBackgroundDrawable(dw);
        backgroundAlpha(mActivity,0.5f);
        popupWindow.showAtLocation(parent, Gravity.CENTER,0,0);
    }

    private void backgroundAlpha(Activity context, float bgAlpha)
    {
        WindowManager.LayoutParams lp = context.getWindow().getAttributes();
        lp.alpha = bgAlpha;
        context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        context.getWindow().setAttributes(lp);
    }

    private int getNavigationBarHeight() {
        Resources resources = mActivity.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height","dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        Log.v("dbw", "Navi height:" + height);
        return height;
    }

    public void dismissWindow() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    public boolean isShowing(){
        if(popupWindow!=null){
            return popupWindow.isShowing();
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        int id=v.getId();
        Intent intent = null;
        switch (id){
            case R.id.tv_sure:
                if(surcBtlicklistener!=null){
                    surcBtlicklistener.onClick(v);
                }
                break;
            case R.id.tv_cancle:
                break;
        }
        if(popupWindow!=null){
            popupWindow.dismiss();
        }
    }

    private void findView(View rootView){
        bt_sure = (TextView) rootView.findViewById(R.id.tv_sure);
        bt_cancle = (TextView) rootView.findViewById(R.id.tv_cancle);
    }

    private void setListener(){
        bt_sure.setOnClickListener(this);
        bt_cancle.setOnClickListener(this);
    }
}
