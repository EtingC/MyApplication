package com.lbest.rm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.lbest.rm.view.fragment.BaseFragment;
import com.lbest.rm.view.fragment.SignUpByEmailFragmnet;
import com.lbest.rm.view.fragment.SignUpByPhoneFragmnet;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends FragmentActivity {

    private ViewPager mViewPager;


    private List<BaseFragment> mTableFragmentList = new ArrayList<>();

    private MyPagerAdapter mMyPagerAdapter;

    private SignUpByPhoneFragmnet mSignUpByPhonelFragmnet;
    private Toolbar toolbar;
    private TextView toolbar_title;
    private View divider;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mSignUpByPhonelFragmnet = new SignUpByPhoneFragmnet();

        mTableFragmentList.clear();
        mTableFragmentList.add(mSignUpByPhonelFragmnet);

        mMyPagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), mTableFragmentList);
        findView();
        initView();
        setListener();
    }

    private void findView(){
        mViewPager = (ViewPager) findViewById(R.id.sign_boby_layout);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        divider=findViewById(R.id.divider);
    }

    private void initView(){

        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_mainaccount_register));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));
        toolbar.setBackgroundColor(Color.parseColor("#ffffff"));
        mViewPager.setAdapter(mMyPagerAdapter);//给ViewPager设置适配器

        divider.setVisibility(View.INVISIBLE);
    }

    private void setListener(){
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterActivity.this.finish();
            }
        });
    }

    //ViewPager适配器
    class MyPagerAdapter extends FragmentStatePagerAdapter {

        private List<BaseFragment> mTableList = new ArrayList<>();

        public MyPagerAdapter(FragmentManager fm, List<BaseFragment> roomlist) {
            super(fm);
            this.mTableList = roomlist;
        }

        @Override
        public BaseFragment getItem(int position) {
            return mTableList.get(position);
        }

        @Override
        public int getCount() {
            return mTableList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "";
        }
    }

}
