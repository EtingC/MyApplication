package com.lbest.rm.view.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lbest.rm.R;
import com.lbest.rm.SettingActivity;
import com.lbest.rm.UpdateActivity;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.BLBitmapUtils;
import com.lbest.rm.utils.ImageLoaderUtils;
import com.broadlink.lib.imageloader.core.listener.SimpleImageLoadingListener;

/**
 * Created by dell on 2017/10/26.
 */

public class SceneFragment extends BaseFragment {
    private boolean hasCreate=false;
    public SceneFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.scene_fragment, container, false);

        findview(mView);
        setListener();
        initView();

        hasCreate=true;
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(hasCreate){
            if(isVisibleToUser){

            }else{

            }
        }
    }

    private void findview(View rootView){

    }

    private void setListener(){

    }

    private void initView(){

    }
}
