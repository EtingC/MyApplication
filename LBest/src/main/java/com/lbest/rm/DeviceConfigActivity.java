package com.lbest.rm;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.alink.business.devicecenter.api.add.AddDeviceBiz;
import com.aliyun.alink.business.devicecenter.api.add.DeviceInfo;
import com.aliyun.alink.business.devicecenter.api.add.IAddDeviceListener;
import com.aliyun.alink.business.devicecenter.base.DCErrorCode;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.data.AliDeviceStatusResult;
import com.lbest.rm.data.productInfo;
import com.lbest.rm.productDevice.AliDeviceController;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.fragment.ActiviteDeviceFragment;
import com.lbest.rm.view.fragment.ConfigDeviceErrorFragment;
import com.lbest.rm.view.fragment.ConfigLoadingFragment;
import com.lbest.rm.view.fragment.ConfigWifiFragment;
import com.lbest.rm.view.fragment.ProductInfoFragment;

import static com.aliyun.alink.business.devicecenter.api.add.DeviceInfo.JoinStep.EnrolleeProtocol_ACTIVEFAILURE;
import static com.aliyun.alink.business.devicecenter.api.add.DeviceInfo.JoinStep.EnrolleeProtocol_ACTVING;

public class DeviceConfigActivity extends FragmentActivity implements View.OnClickListener {

    private final String ConfigWifiFragment_TAG=ConfigWifiFragment.class.getSimpleName();
    private final String ProductInfoFragment_TAG=ProductInfoFragment.class.getSimpleName();
    private final String ConfigLoadingFragment_TAG=ConfigLoadingFragment.class.getSimpleName();
    private final String ActiviteDeviceFragment_TAG=ActiviteDeviceFragment.class.getSimpleName();
    private final String ConfigDeviceErrorFragment_TAG=ConfigDeviceErrorFragment.class.getSimpleName();

    private Toolbar toolbar;
    private TextView toolbar_title;


    private productInfo product;
    private TextView bt_config;

    private static FragmentManager fgManager;
    private  ConfigWifiFragment configWifiFragment;
    private ProductInfoFragment productInfoFragment;
    private ConfigLoadingFragment configLoadingFragment;
    private ActiviteDeviceFragment activiteDeviceFragment;
    private ConfigDeviceErrorFragment configDeviceErrorFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_config);
        product=getIntent().getParcelableExtra(Constants.INTENT_PRODUCTINFO);
        fgManager=getSupportFragmentManager();

        findview();
        initView();
        setListener();

        showProductInfoFragment();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(configLoadingFragment!=null){
            configLoadingFragment.stopConfig();
        }
    }

    private void initView(){
        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(R.string.Intelligent_clothes_dryer);
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));
    }

    private void findview(){
        bt_config=(TextView)findViewById(R.id.bt_config);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
    }


    private void setListener(){
        bt_config.setOnClickListener(this);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeviceConfigActivity.this.finish();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        //showProductInfoFragment();
    }

    public void showConfigWifiFragment(){
        hiddenProductInfoFragment();

        FragmentTransaction transaction = fgManager.beginTransaction();
        configWifiFragment= (ConfigWifiFragment) fgManager.findFragmentByTag(ConfigWifiFragment_TAG);
        if(configWifiFragment==null){
            configWifiFragment=new ConfigWifiFragment();
            transaction.add(R.id.fl_fragmentcontain, configWifiFragment,ConfigWifiFragment_TAG);
        }else{
            transaction.show(configWifiFragment);
        }
        transaction.commit();
    }

    public  void hiddenConfigWifiFragment(){
        configWifiFragment= (ConfigWifiFragment) fgManager.findFragmentByTag(ConfigWifiFragment_TAG);
        if(configWifiFragment!=null){
            FragmentTransaction transaction = fgManager.beginTransaction();
            transaction.remove(configWifiFragment);
            transaction.commit();
        }
    }



    public void showConfigDeviceErrorFragment(int type){
//        if(type== ConfigDeviceErrorFragment.SHOWTYPE_ACTIVEERR0R){
//
//        }else if(type== ConfigDeviceErrorFragment.SHOWTYPE_CONFIGERR0R){
//
//        }
        hiddenConfigLoadingFragment();
        hiddenActiveDeviceFragment();
        FragmentTransaction transaction = fgManager.beginTransaction();
        configDeviceErrorFragment= (ConfigDeviceErrorFragment) fgManager.findFragmentByTag(ConfigDeviceErrorFragment_TAG);
        if(configDeviceErrorFragment==null){
            configDeviceErrorFragment=new ConfigDeviceErrorFragment();
            configDeviceErrorFragment.setType(type);
            transaction.add(R.id.fl_fragmentcontain, configDeviceErrorFragment,ConfigDeviceErrorFragment_TAG);
        }else{
            configDeviceErrorFragment.setType(type);
            transaction.show(configDeviceErrorFragment);
        }
        transaction.commit();
    }

    public  void hiddenConfigDeviceErrorFragment(){
        configDeviceErrorFragment= (ConfigDeviceErrorFragment) fgManager.findFragmentByTag(ConfigDeviceErrorFragment_TAG);
        if(configDeviceErrorFragment!=null){
            FragmentTransaction transaction = fgManager.beginTransaction();
            transaction.remove(configDeviceErrorFragment);
            transaction.commit();
        }
    }



    public void showActiveDeviceFragment(){
        hiddenConfigLoadingFragment();

        FragmentTransaction transaction = fgManager.beginTransaction();
        activiteDeviceFragment= (ActiviteDeviceFragment) fgManager.findFragmentByTag(ActiviteDeviceFragment_TAG);
        if(activiteDeviceFragment==null){
            activiteDeviceFragment=new ActiviteDeviceFragment();
            transaction.add(R.id.fl_fragmentcontain, activiteDeviceFragment,ActiviteDeviceFragment_TAG);
        }else{
            transaction.show(activiteDeviceFragment);
        }
        transaction.commit();
    }

    public  void hiddenActiveDeviceFragment(){
        activiteDeviceFragment= (ActiviteDeviceFragment) fgManager.findFragmentByTag(ActiviteDeviceFragment_TAG);
        if(activiteDeviceFragment!=null){
            FragmentTransaction transaction = fgManager.beginTransaction();
            transaction.remove(activiteDeviceFragment);
            transaction.commit();
        }
    }


    public void showProductInfoFragment(){
        hiddenConfigDeviceErrorFragment();
        hiddenActiveDeviceFragment();
        hiddenConfigLoadingFragment();
        hiddenConfigWifiFragment();

        FragmentTransaction transaction = fgManager.beginTransaction();
        productInfoFragment= (ProductInfoFragment) fgManager.findFragmentByTag(ProductInfoFragment_TAG);
        if(productInfoFragment==null){
            productInfoFragment=new ProductInfoFragment();
            transaction.add(R.id.fl_fragmentcontain, productInfoFragment,ProductInfoFragment_TAG);
        }else{
            transaction.show(productInfoFragment);
        }
        transaction.commit();
    }

    public  void hiddenProductInfoFragment(){
        productInfoFragment= (ProductInfoFragment) fgManager.findFragmentByTag(ProductInfoFragment_TAG);
        if(productInfoFragment!=null){
            FragmentTransaction transaction = fgManager.beginTransaction();
            transaction.remove(productInfoFragment);
            transaction.commit();
        }
    }


    public void showConfigLoadingFragment(String ssid,String password){
        hiddenConfigWifiFragment();

        FragmentTransaction transaction = fgManager.beginTransaction();
        configLoadingFragment= (ConfigLoadingFragment) fgManager.findFragmentByTag(ConfigLoadingFragment_TAG);
        if(configLoadingFragment==null){
            configLoadingFragment=new ConfigLoadingFragment();
            configLoadingFragment.setSsid(ssid);
            configLoadingFragment.setPassword(password);
            transaction.add(R.id.fl_fragmentcontain, configLoadingFragment,ConfigLoadingFragment_TAG);
        }else{
            configLoadingFragment.setSsid(ssid);
            configLoadingFragment.setPassword(password);
            transaction.show(configLoadingFragment);
        }
        transaction.commit();
    }


    public  void hiddenConfigLoadingFragment(){
        configLoadingFragment= (ConfigLoadingFragment) fgManager.findFragmentByTag(ConfigLoadingFragment_TAG);
        if(configLoadingFragment!=null){
            FragmentTransaction transaction = fgManager.beginTransaction();
            transaction.remove(configLoadingFragment);
            transaction.commit();
        }
    }



    public void setNextButton(String text,int color,boolean enable){
        bt_config.setText(text);
        bt_config.setBackgroundColor(color);
        bt_config.setEnabled(enable);
    }


    public void setNextButtonListener(View.OnClickListener listener){
        bt_config.setOnClickListener(listener);
    }

    public productInfo getProduct() {
        return product;
    }


    @Override
    public void onClick(View v) {
        int id=v.getId();
        switch (id){
            case R.id.bt_config:

                break;
        }
    }
}
