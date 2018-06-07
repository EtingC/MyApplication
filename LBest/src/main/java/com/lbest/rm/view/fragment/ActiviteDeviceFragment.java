package com.lbest.rm.view.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.lbest.rm.DeviceConfigActivity;
import com.lbest.rm.R;
import com.lbest.rm.data.ProductInitAction;
import com.lbest.rm.data.productInfo;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.utils.NetworkUtils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by dell on 2017/10/27.
 */

public class ActiviteDeviceFragment extends BaseFragment{

    private DeviceConfigActivity mActivity;
    private TextView tv_howactive;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activitedevice_fragment_layout, container, false);
        mActivity=(DeviceConfigActivity)getActivity();

        mActivity.setNextButtonListener(null);
        mActivity.setNextButton(getResources().getString(R.string.str_activingdevice), getResources().getColor(R.color.colorAccent), false);

        findView(view);
        setListener();

        mActivity.setNextButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        return view;
    }

    private void findView(View rootView) {
        tv_howactive= (TextView) rootView.findViewById(R.id.tv_howactive);
    }


    private void setListener() {

    }

    private void initView() {
        Product.getProductDetail(mActivity.getProduct().getModel(), new Product.ProductResultCallBack(getActivity()) {
            @Override
            public void callBack(int code, String msg, Object data) {
                Logutils.log_d("ActiviteDeviceFragment initView getProductDetail:"+msg+"  "+code);
                super.callBack(code, msg, data);
                if(data!=null){
                    productInfo product= (productInfo) data;
                    ProductInitAction productInitAction= JSON.parseObject(product.getInitAction(),ProductInitAction.class);
                    tv_howactive.setText(productInitAction.getAction());
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!NetworkUtils.isWifiConnect(getActivity().getApplicationContext())) {
            Toast.makeText(getActivity(),getResources().getString(R.string.connectwifi),Toast.LENGTH_LONG).show();
        }
        initView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            initView();
        }else{

        }
    }
}
