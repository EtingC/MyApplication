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

import com.lbest.rm.AboutActivity;
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

public class MeFragment extends BaseFragment {

    private RelativeLayout rl_setting;
    private RelativeLayout rl_version;
    private RelativeLayout rl_about;
    private ImageView iv_icon;
    private TextView tv_id;
    private ImageLoaderUtils mImageLoaderUtils;

    private boolean hasCreate=false;
    public MeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.me_fragment, container, false);
        mImageLoaderUtils = ImageLoaderUtils.getInstence(getActivity().getApplicationContext());

        findview(mView);
        setListener();
        initView();

        hasCreate=true;
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        initView();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(hasCreate){
            if(isVisibleToUser){
                initView();
            }else{

            }
        }
    }

    private void findview(View rootView){
        rl_setting=(RelativeLayout)rootView.findViewById(R.id.rl_setting);
        rl_version=(RelativeLayout)rootView.findViewById(R.id.rl_version);
        rl_about=(RelativeLayout)rootView.findViewById(R.id.rl_about);
        iv_icon=(ImageView)rootView.findViewById(R.id.iv_icon);
        tv_id=(TextView) rootView.findViewById(R.id.tv_id);
    }

    private void setListener(){
        rl_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.setClass(getContext(), SettingActivity.class);
                startActivity(intent);
            }
        });

        rl_version.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.setClass(getContext(), UpdateActivity.class);
                startActivity(intent);
            }
        });

        rl_about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.setClass(getContext(), AboutActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initView(){

        tv_id.setText(BLAcountToAli.getInstance().getBlUserInfo().getBl_nickname());
        mImageLoaderUtils.displayImage(BLAcountToAli.getInstance().getBlUserInfo().getBl_icon(), iv_icon, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if(loadedImage == null){
                    ((ImageView) view).setImageResource(R.drawable.default_icon);
                    view.setTag(BitmapFactory.decodeResource(getResources(), R.drawable.default_icon));
                }else{
                    Bitmap circle=BLBitmapUtils.toImageCircle(loadedImage);
                    iv_icon.setImageBitmap(circle);
                    view.setTag(loadedImage);
                }
            }
        });

    }
}
