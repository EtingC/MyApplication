package com.lbest.rm.view.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.lbest.rm.Constants;
import com.lbest.rm.DeviceConfigActivity;
import com.lbest.rm.HomeActivity;
import com.lbest.rm.R;
import com.lbest.rm.data.ProductInitAction;
import com.lbest.rm.data.productInfo;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.utils.NetworkUtils;

/**
 * Created by dell on 2017/10/27.
 */

public class ConfigDeviceErrorFragment extends BaseFragment{

    public static final int SHOWTYPE_CONFIGERR0R=1;
    public static final int SHOWTYPE_ACTIVEERR0R=2;
    public static final int SHOWTYPE_ACTIVESUCCESS=3;
    private DeviceConfigActivity mActivity;
    private TextView tv_texttitle;
    private TextView tv_erroradvice;
    private TextView tv_errorresult;
    private ImageView iv_erroricon;
    private int type=1;//

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.configdeviceerror_fragment_layout, container, false);
        mActivity=(DeviceConfigActivity)getActivity();
        findView(view);
        setListener();
        return view;
    }

    private void findView(View rootView) {
        tv_texttitle= (TextView) rootView.findViewById(R.id.tv_texttitle);
        tv_erroradvice= (TextView) rootView.findViewById(R.id.tv_erroradvice);
        tv_errorresult= (TextView) rootView.findViewById(R.id.tv_errorresult);
        iv_erroricon= (ImageView) rootView.findViewById(R.id.iv_erroricon);
    }


    private void setListener() {

    }

    private void refreshView() {
        if(type==SHOWTYPE_CONFIGERR0R){
            tv_texttitle.setVisibility(View.VISIBLE);
            tv_errorresult.setText(getResources().getString(R.string.str_configdeviceerror));
            tv_erroradvice.setText(getResources().getString(R.string.str_configdeviceerroradvice));
            iv_erroricon.setImageResource(R.drawable.configdevice_fail);
            mActivity.setNextButton(getResources().getString(R.string.str_configtryagain), getResources().getColor(R.color.colorAccent), true);
            mActivity.setNextButtonListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.finish();
                }
            });
        }else if(type==SHOWTYPE_ACTIVEERR0R){
            tv_texttitle.setVisibility(View.VISIBLE);
            tv_errorresult.setText(getResources().getString(R.string.str_activedeviceerror));
            tv_erroradvice.setText(getResources().getString(R.string.str_activedeviceerroradvice));
            iv_erroricon.setImageResource(R.drawable.configdevice_fail);

            mActivity.setNextButton(getResources().getString(R.string.str_configtryagain), getResources().getColor(R.color.colorAccent), true);
            mActivity.setNextButtonListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.finish();
                }
            });
        }else if(type==SHOWTYPE_ACTIVESUCCESS){
            tv_texttitle.setVisibility(View.INVISIBLE);
            tv_errorresult.setText(getResources().getString(R.string.str_configdevicesuccess));
            tv_erroradvice.setVisibility(View.INVISIBLE);
            iv_erroricon.setImageResource(R.drawable.configdevice_success);
            //tv_erroradvice.setText(getResources().getString(R.string.str_activedeviceerroradvice));

            mActivity.setNextButton(getResources().getString(R.string.str_configfinish), getResources().getColor(R.color.colorAccent), true);
            mActivity.setNextButtonListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.finish();
                    Intent intent=new Intent();
                    intent.setClass(mActivity, HomeActivity.class);
                    intent.putExtra(Constants.INTENT_FRAGMENTINDEX,HomeActivity.DEVICELIST_INDEX);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mActivity.startActivity(intent);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!NetworkUtils.isWifiConnect(getActivity().getApplicationContext())) {
            Toast.makeText(getActivity(),getResources().getString(R.string.connectwifi),Toast.LENGTH_LONG).show();
        }
        refreshView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            refreshView();
        }else{

        }
    }
}
