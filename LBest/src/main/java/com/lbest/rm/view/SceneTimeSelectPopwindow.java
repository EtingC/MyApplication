package com.lbest.rm.view;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.lbest.rm.R;
import com.lbest.rm.data.ParamData;
import com.lbest.rm.utils.Logutils;

/**
 * Created by dell on 2017/11/1.
 */

public class SceneTimeSelectPopwindow implements View.OnClickListener{

    private PopupWindow popupWindow;
    private Activity mActivity;
    private NumberPickerView np_hour;
    private NumberPickerView np_minute;
    private Button bt_sure;
    private ImageView bt_exit;
    private onClickListener clickListener;

    public void setClickListener(onClickListener clickListener) {
        this.clickListener = clickListener;
    }

    private String[] numberPick_hour;
    private String[] numberPick_minute;
    private String selectValue_hour;
    private String selectValue_minute;
    private int action;

    public void setAction(int action) {
        this.action = action;
    }

    public interface onClickListener{
        public void onClick(int action,String value1,String value2);
    }

    public SceneTimeSelectPopwindow(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public void showWindow(View parent) {

        WindowManager m = mActivity.getWindowManager();
        Display d = m.getDefaultDisplay();  //为获取屏幕宽、高
        int width=(int) (d.getWidth() * 0.9);
        int height=(int) (d.getHeight() * 0.45);

        final View contentView = LayoutInflater.from(mActivity).inflate(R.layout.scenetime_selectwindow_layout, null);
        popupWindow = new PopupWindow(contentView,width, ViewGroup.LayoutParams.WRAP_CONTENT,true);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                backgroundAlpha(mActivity,1.0f);
            }
        });
        popupWindow.setContentView(contentView);
        findView(contentView);
        initView();
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
            case R.id.bt_sure:
                clickListener.onClick(action,selectValue_hour,selectValue_minute);
                break;
            case R.id.bt_exit:
                break;
        }
        if(popupWindow!=null){
            popupWindow.dismiss();
        }
    }

    private void findView(View rootView){
        bt_sure = (Button) rootView.findViewById(R.id.bt_sure);
        bt_exit = (ImageView) rootView.findViewById(R.id.bt_exit);
        np_hour = (NumberPickerView) rootView.findViewById(R.id.np_hour);
        np_minute = (NumberPickerView) rootView.findViewById(R.id.np_minute);
    }
    private void  initView(){
            numberPick_hour = new String[24];
            for(int i=0;i<24;i++){
                numberPick_hour[i]=String.valueOf(i);
            }
            np_hour.setDisplayedValues(numberPick_hour);
            np_hour.setMinValue(0);
            np_hour.setMaxValue(numberPick_hour.length-1);
            selectValue_hour=numberPick_hour[0];
            np_hour.setValue(0);//设置第一次显示的位置

            numberPick_minute = new String[60];
            for(int i=0;i<60;i++){
                numberPick_minute[i]=String.valueOf(i);
            }
            np_minute.setDisplayedValues(numberPick_minute);
            np_minute.setMinValue(0);
            np_minute.setMaxValue(numberPick_minute.length-1);
            selectValue_minute=numberPick_minute[29];
            np_minute.setValue(29);//设置第一次显示的位置
    }

    private void setListener(){
        bt_sure.setOnClickListener(this);
        bt_exit.setOnClickListener(this);
        np_hour.setOnValueChangedListener(new NumberPickerView.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
                Logutils.log_d("onValueChange:"+oldVal+"    "+newVal);
                selectValue_hour=numberPick_hour[newVal];
            }
        });

        np_minute.setOnValueChangedListener(new NumberPickerView.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
                Logutils.log_d("onValueChange:"+oldVal+"    "+newVal);
                selectValue_minute=numberPick_minute[newVal];
            }
        });
    }
}
