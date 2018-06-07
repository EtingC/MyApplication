package com.lbest.rm.view.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.DeviceConfigActivity;
import com.lbest.rm.R;
import com.lbest.rm.utils.NetworkUtils;

import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by dell on 2017/10/27.
 */

public class ConfigWifiFragment extends BaseFragment{

    private DeviceConfigActivity mActivity;
    private EditText mSSIDView, mPWDView;
    private TextView mSwitchView, mUnPwdView;
    private ImageView mPWDSeeView;
    private LinearLayout mPWDLayout;

    private WifiBroadcastReceiver mWifiBroadcastReceiver;
    private volatile boolean isRegister;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.configwifi_fragment_layout, container, false);
        mActivity=(DeviceConfigActivity)getActivity();

        findView(view);

        mActivity.setNextButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ssid=mSSIDView.getText().toString();
                String password=mPWDView.getText().toString();
                mActivity.showConfigLoadingFragment(ssid,password);
            }
        });


        initView();
        setListener();
        return view;
    }

    private void findView(View rootView) {

        mSSIDView = (EditText) rootView.findViewById(R.id.edit_ssid_str);
        mPWDView = (EditText) rootView.findViewById(R.id.edit_pwd_str);
        mSwitchView = (TextView) rootView.findViewById(R.id.edit_ssid_btn);
        mPWDSeeView = (ImageView) rootView.findViewById(R.id.edit_pwd_btn);

        mPWDLayout = (LinearLayout) rootView.findViewById(R.id.edit_pwd_layout);

        mUnPwdView = (TextView) rootView.findViewById(R.id.edit_unpwd_layout);
    }


    private void setListener() {
        mSSIDView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                } else {
                    checkSSID(s.toString());
                }
            }
        });

        mPWDView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    mActivity.setNextButton(getResources().getString(R.string.str_next), Color.parseColor("#BBBBBB"),false);
                } else {
                    mActivity.setNextButton(getResources().getString(R.string.str_next),getResources().getColor(R.color.colorAccent),true);
                }
            }
        });

        mSwitchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
            }
        });

        mPWDSeeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPWDView.getInputType() != InputType.TYPE_CLASS_TEXT) {
                    mPWDView.setInputType(InputType.TYPE_CLASS_TEXT);
                    mPWDSeeView.setImageResource(R.drawable.password_visiable);
                } else {
                    mPWDView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);
                    mPWDSeeView.setImageResource(R.drawable.password_invisiable);
                }
            }
        });
    }

    private void checkSSID(String ssid) {
        if (ssid != null && ssid.contains("未知")) {
            mSSIDView.setClickable(true);
            mSSIDView.setFocusable(true);
            mSSIDView.setFocusableInTouchMode(true);
            Toast.makeText(getActivity(), R.string.str_devices_configure_wifi_ware, Toast.LENGTH_SHORT).show();
        } else {
            mSSIDView.setClickable(false);
            mSSIDView.setFocusable(false);
            mSSIDView.setFocusableInTouchMode(false);
        }
    }


    private void initView() {
        mPWDView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPWDSeeView.setImageResource(R.drawable.password_visiable);
        if (NetworkUtils.isWifiConnect(getActivity().getApplicationContext())) {
            initWiFiSSIDView();
        }
    }


    //显示当前手机所连接的SSID
    public void initWiFiSSIDView() {
        String ssid=NetworkUtils.getWifiSSID(getActivity().getApplicationContext());
        if (!TextUtils.isEmpty(ssid)) {
            mSSIDView.setText(ssid);
            switchPWDLayout(ssid);
        }
    }

    private void switchPWDLayout(String currentWifiSSID) {
        if (NetworkUtils.checkWifiHasPassword(getActivity().getApplicationContext(), currentWifiSSID)) {
            mUnPwdView.setVisibility(GONE);
            mPWDLayout.setVisibility(VISIBLE);
        } else {
            mPWDLayout.setVisibility(GONE);
            mUnPwdView.setVisibility(VISIBLE);
        }
    }


    public void registerBroadcastReceiver() {
        if (!isRegister) {
            isRegister = true;
            if (mWifiBroadcastReceiver == null) {
                mWifiBroadcastReceiver = new WifiBroadcastReceiver();
                IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
                getActivity().getApplicationContext().registerReceiver(mWifiBroadcastReceiver, intentFilter);
            }
        }
    }

    public void unregisterBroadcastReceiver() {
        if (isRegister) {
            if (mWifiBroadcastReceiver != null) {
                getActivity().getApplicationContext().unregisterReceiver(mWifiBroadcastReceiver);
                mWifiBroadcastReceiver = null;
            }
            isRegister = false;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!NetworkUtils.isWifiConnect(getActivity().getApplicationContext())) {
            Toast.makeText(getActivity(),getResources().getString(R.string.connectwifi),Toast.LENGTH_LONG).show();
        }
        registerBroadcastReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterBroadcastReceiver();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            registerBroadcastReceiver();

            mActivity.setNextButtonListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String ssid=mSSIDView.getText().toString();
                    String password=mPWDView.getText().toString();
                    mActivity.showConfigLoadingFragment(ssid,password);
                }
            });

        }else{
            unregisterBroadcastReceiver();
        }
    }



    class WifiBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (NetworkUtils.isWifiConnect(getActivity().getApplicationContext())) {
                initWiFiSSIDView();
            }
        }
    }

}
