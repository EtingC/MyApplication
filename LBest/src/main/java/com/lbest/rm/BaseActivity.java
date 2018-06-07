package com.lbest.rm;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.lbest.rm.common.SystemBarTintManager;

import java.lang.reflect.Field;

/**
 * Created by dell on 2017/11/29.
 */

public class BaseActivity extends FragmentActivity {
    protected SystemBarTintManager mSystemBarTintManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSystemBar();
    }

    private void initSystemBar(){
        mSystemBarTintManager = new SystemBarTintManager(this);
        mSystemBarTintManager.setStatusBarTintEnabled(true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            if (mSystemBarTintManager.isStatusBarAvailable()) {
                mSystemBarTintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimaryDark));
                mSystemBarTintManager.setStatusBarAlpha(1.0f);
            }
        }
    }

    public boolean inRangeOfView(View view, MotionEvent ev) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        if (ev.getX() < x || ev.getX() > (x + view.getWidth()) || ev.getY() < y || ev.getY() > (y + view.getHeight())) {
            return false;
        }
        return true;
    }

    protected void back() {
        setResult(RESULT_OK);
        finish();
    }

    public void activityFinish() {
        this.finish();
    }

    /**关闭手机键盘**/
    public void closeInputMethod() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (BaseActivity.this.getCurrentFocus() != null) {
            if (BaseActivity.this.getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(BaseActivity.this.getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }
}
