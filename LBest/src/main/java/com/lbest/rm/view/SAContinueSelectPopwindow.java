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
import com.lbest.rm.productDevice.SceneFactory;
import com.lbest.rm.utils.Logutils;

/**
 * Created by dell on 2017/11/1.
 */

public class SAContinueSelectPopwindow implements View.OnClickListener{

    private PopupWindow popupWindow;
    private Activity mActivity;
    private NumberPickerView numberPickerView;
    private Button bt_sure;
    private ImageView bt_exit;
    private onClickListener clickListener;

    private ParamData paramData;

    public void setClickListener(onClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setParamData(ParamData paramData) {
        this.paramData = paramData;
    }
    private String[] numberPick_datas;
    private String selectValue;

    public interface onClickListener{
        public void onClick(String value);
    }

    public SAContinueSelectPopwindow(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public void showWindow(View parent) {

        WindowManager m = mActivity.getWindowManager();
        Display d = m.getDefaultDisplay();  //为获取屏幕宽、高
        int width=(int) (d.getWidth() * 0.9);
        int height=(int) (d.getHeight() * 0.45);

        final View contentView = LayoutInflater.from(mActivity).inflate(R.layout.sa_continueselectwindow_layout, null);
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
                clickListener.onClick(selectValue);
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
        numberPickerView = (NumberPickerView) rootView.findViewById(R.id.numberpicker);
    }
    private void  initView(){
        if(paramData!=null){
            numberPickerView.setHintText(paramData.getUnit());
            numberPickerView.setHintTextColor(mActivity.getResources().getColor(R.color.tabgray_selected));
            int length=(paramData.getValue_range2().max-paramData.getValue_range2().min)/paramData.getValue_range2().step+1;
            numberPick_datas = new String[length];
            int start=paramData.getValue_range2().min;
            for(int i=0;i<length;i++){
                if(i>0){
                    start=start+paramData.getValue_range2().step;
                }
                numberPick_datas[i]=String.valueOf(start);
            }
            numberPickerView.setDisplayedValues(numberPick_datas);
            numberPickerView.setMinValue(0);
            numberPickerView.setMaxValue(length-1);
            selectValue=numberPick_datas[0];
            numberPickerView.setValue(0);//设置第一次显示的位置
        }
    }

    private void setListener(){
        bt_sure.setOnClickListener(this);
        bt_exit.setOnClickListener(this);
        numberPickerView.setOnValueChangedListener(new NumberPickerView.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
                Logutils.log_d("onValueChange:"+oldVal+"    "+newVal);
                selectValue=numberPick_datas[newVal];
            }
        });
    }
}
