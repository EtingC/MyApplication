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

import com.lbest.rm.Constants;
import com.lbest.rm.R;
import com.lbest.rm.common.StorageUtils;
import com.lbest.rm.productDevice.SceneFactory;

import java.io.File;

/**
 * Created by dell on 2017/11/1.
 */

public class ChooseSceneModelPopwindow implements View.OnClickListener{

    private PopupWindow popupWindow;
    private Activity mActivity;
    private Button bt_open;
    private Button bt_close;
    private Button bt_open_close;
    private Button bt_cancle;
    private onItemClickListener clickListener;

    public void setClickListener(onItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface onItemClickListener{
        public void onClick(String value);
    }

    public ChooseSceneModelPopwindow(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public void showWindow(View parent) {

        WindowManager m = mActivity.getWindowManager();
        Display d = m.getDefaultDisplay();  //为获取屏幕宽、高
        int width=(int) (d.getWidth() * 0.9);
        int height=(int) (d.getHeight() * 0.45);

        final View contentView = LayoutInflater.from(mActivity).inflate(R.layout.choosescenemodelwindow_layout, null);
        popupWindow = new PopupWindow(contentView,width, ViewGroup.LayoutParams.WRAP_CONTENT,true);
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
        popupWindow.showAtLocation(parent, Gravity.CENTER_HORIZONTAL| Gravity.BOTTOM, 0, h);
    }

    public void showWindow(View parent, String data) {

        WindowManager m = mActivity.getWindowManager();
        Display d = m.getDefaultDisplay();  //为获取屏幕宽、高
        int width=(int) (d.getWidth() * 0.9);
        int height=(int) (d.getHeight() * 0.45);

        final View contentView = LayoutInflater.from(mActivity).inflate(R.layout.choosescenemodelwindow_layout, null);
        popupWindow = new PopupWindow(contentView,width, ViewGroup.LayoutParams.WRAP_CONTENT,true);
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
        popupWindow.showAtLocation(parent, Gravity.CENTER_HORIZONTAL| Gravity.BOTTOM, 0, h);
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
            case R.id.bt_open:
                if(clickListener!=null){
                    clickListener.onClick(SceneFactory.SCENE_APPOINT_ON);
                }
                break;
            case R.id.bt_close:
                if(clickListener!=null){
                    clickListener.onClick(SceneFactory.SCENE_APPOINT_OFF);
                }
                break;
            case R.id.bt_open_close:
                if(clickListener!=null){
                    clickListener.onClick(SceneFactory.SCENE_APPOINT_ON_OFF);
                }
                break;
            case R.id.bt_cancle:
                break;
        }
        if(popupWindow!=null){
            popupWindow.dismiss();
        }
    }

    private void findView(View rootView){

        bt_open = (Button) rootView.findViewById(R.id.bt_open);
        bt_close = (Button) rootView.findViewById(R.id.bt_close);
        bt_open_close = (Button) rootView.findViewById(R.id.bt_open_close);
        bt_cancle = (Button) rootView.findViewById(R.id.bt_cancle);
    }

    private void setListener(){
        bt_open.setOnClickListener(this);
        bt_close.setOnClickListener(this);
        bt_open_close.setOnClickListener(this);
        bt_cancle.setOnClickListener(this);
    }
}
