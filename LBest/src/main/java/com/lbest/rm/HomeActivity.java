package com.lbest.rm;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.adapter.PageViewAdapter;
import com.lbest.rm.data.QrParseData;
import com.lbest.rm.data.db.DatabaseHelper;
import com.lbest.rm.productDevice.DeviceManager;
import com.lbest.rm.productDevice.FamilyManager;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.productDevice.AliDeviceController;
import com.lbest.rm.view.fragment.DeviceListFragment;

import java.util.ArrayList;
import java.util.List;

import cn.com.broadlink.sdk.BLLet;

public class HomeActivity extends AppCompatActivity{
    private static final int REQ_CODE_PERMISSION = 0x1111;

    private Toolbar toolbar;
    private TextView toolbar_title;


    private ViewPager viewPager;
    private TabLayout tabLayout;
    private TabLayout.Tab tab_device;
    private TabLayout.Tab tab_scene;
    private TabLayout.Tab tab_me;

    private PageViewAdapter adapter;
    private DeviceManager deviceManager;
    private DatabaseHelper databaseHelper;

    public static final int DEVICELIST_INDEX=0;
    public static final int SCENE_INDEX=1;
    public static final int ME_INDEX=2;

    private List<String> tabtitle_list=new ArrayList();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_use_devlist);
        initData();
        findView();
        initView();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseHelper.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent!=null){
            int fragmet_index=intent.getIntExtra(Constants.INTENT_FRAGMENTINDEX,-1);
            if(fragmet_index==DEVICELIST_INDEX){
                viewPager.setCurrentItem(DEVICELIST_INDEX,true);
            }else if(fragmet_index==SCENE_INDEX){

            }else if(fragmet_index==ME_INDEX){

            }
        }
    }

    private void initData(){
        databaseHelper=DatabaseHelper.getHelper(getApplicationContext());
        deviceManager=DeviceManager.getInstance();
        FamilyManager.getInstance().init(HomeActivity.this);
    }

    private void findView() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);

        tabLayout= (TabLayout) findViewById(R.id.tabLayout);
        viewPager= (ViewPager) findViewById(R.id.viewPager);

    }

    private void setListener() {
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position=tab.getPosition();
                if (position == 0) {
                    ((TextView)tab.getCustomView().findViewById(R.id.tv_tabtitle)).setTextColor(getResources().getColor(R.color.color_tabtxt_select));
                    ((ImageView)tab.getCustomView().findViewById(R.id.iv_tabicon)).setImageResource(R.drawable.device);
                    toolbar_title.setText(getResources().getString(R.string.str_devicelist));
                    //viewPager.setCurrentItem(0);
                } else if (position == 1) {
                    ((TextView)tab.getCustomView().findViewById(R.id.tv_tabtitle)).setTextColor(getResources().getColor(R.color.color_tabtxt_select));
                    ((ImageView)tab.getCustomView().findViewById(R.id.iv_tabicon)).setImageResource(R.drawable.scene);
                    toolbar_title.setText(getResources().getString(R.string.str_fragment_scene));
                    //viewPager.setCurrentItem(1);
                } else if (position == 2) {
                    ((TextView)tab.getCustomView().findViewById(R.id.tv_tabtitle)).setTextColor(getResources().getColor(R.color.color_tabtxt_select));
                    ((ImageView)tab.getCustomView().findViewById(R.id.iv_tabicon)).setImageResource(R.drawable.me);
                    toolbar_title.setText(getResources().getString(R.string.str_fragment_me));
                    //viewPager.setCurrentItem(2);
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab == tabLayout.getTabAt(0)) {
                    ((TextView)tab.getCustomView().findViewById(R.id.tv_tabtitle)).setTextColor(getResources().getColor(R.color.color_tabtxt_unselect));
                    ((ImageView)tab.getCustomView().findViewById(R.id.iv_tabicon)).setImageResource(R.drawable.device_gray);
                } else if (tab == tabLayout.getTabAt(1)) {
                    ((TextView)tab.getCustomView().findViewById(R.id.tv_tabtitle)).setTextColor(getResources().getColor(R.color.color_tabtxt_unselect));
                    ((ImageView)tab.getCustomView().findViewById(R.id.iv_tabicon)).setImageResource(R.drawable.scene_gray);
                } else if (tab == tabLayout.getTabAt(2)) {
                    ((TextView)tab.getCustomView().findViewById(R.id.tv_tabtitle)).setTextColor(getResources().getColor(R.color.color_tabtxt_unselect));
                    ((ImageView)tab.getCustomView().findViewById(R.id.iv_tabicon)).setImageResource(R.drawable.me_gray);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                int position=tab.getPosition();
            }


        });

    }


    private void initView() {
        toolbar.setTitle("");
        toolbar.setBackgroundColor(Color.parseColor("#EFEFF1"));
        setSupportActionBar(toolbar);

        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_devicelist));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));

        adapter=new PageViewAdapter(getSupportFragmentManager());
        tabtitle_list.add(getResources().getString(R.string.str_fragment_device));
        tabtitle_list.add(getResources().getString(R.string.str_fragment_scene));
        tabtitle_list.add(getResources().getString(R.string.str_fragment_me));
        adapter.setPageTitleList(tabtitle_list);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);


        LayoutInflater layoutInflater=getLayoutInflater();
        View tabdeviceView=layoutInflater.inflate(R.layout.tabitem_layout,null,false);
        ImageView iv_home= (ImageView) tabdeviceView.findViewById(R.id.iv_tabicon);
        iv_home.setImageResource(R.drawable.device);
        TextView tv_home= (TextView) tabdeviceView.findViewById(R.id.tv_tabtitle);
        tv_home.setText(tabtitle_list.get(0));
        tv_home.setTextColor(getResources().getColor(R.color.color_tabtxt_select));
        tab_device = tabLayout.getTabAt(0);
        tab_device.setCustomView(tabdeviceView);

        View tabsceneView=layoutInflater.inflate(R.layout.tabitem_layout,null,false);
        ImageView iv_scene= (ImageView) tabsceneView.findViewById(R.id.iv_tabicon);
        iv_scene.setImageResource(R.drawable.scene_gray);
        TextView tv_scene= (TextView) tabsceneView.findViewById(R.id.tv_tabtitle);
        tv_scene.setText(tabtitle_list.get(1));
        tv_scene.setTextColor(getResources().getColor(R.color.color_tabtxt_unselect));
        tab_scene = tabLayout.getTabAt(1);
        tab_scene.setCustomView(tabsceneView);

        View tabmeView=layoutInflater.inflate(R.layout.tabitem_layout,null,false);
        ImageView iv_me= (ImageView) tabmeView.findViewById(R.id.iv_tabicon);
        iv_me.setImageResource(R.drawable.me_gray);
        TextView tv_me= (TextView) tabmeView.findViewById(R.id.tv_tabtitle);
        tv_me.setText(tabtitle_list.get(2));
        tv_me.setTextColor(getResources().getColor(R.color.color_tabtxt_unselect));
        tab_me = tabLayout.getTabAt(2);
        tab_me.setCustomView(tabmeView);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_CODE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // User agree the permission
                } else {
                    // User disagree the permission
                    Toast.makeText(this, "You must agree the camera permission request before you use the code scan function", Toast.LENGTH_LONG).show();
                }
            }
            break;
            default:
        }
    }




    public void setToolbarMenu(){

    }

    public void setToolbarMenuListener(){

    }
}
