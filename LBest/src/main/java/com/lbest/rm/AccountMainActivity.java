package com.lbest.rm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lbest.rm.adapter.RecyclingPagerAdapter;
import com.lbest.rm.view.AutoScrollView.AutoScrollViewPager;
import com.lbest.rm.view.FlowIndicator;
import com.lbest.rm.view.fragment.Account.AccountBaseFragemt;
import com.lbest.rm.view.fragment.Account.LoginFragement;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dell on 2017/11/6.
 */

public class AccountMainActivity extends FragmentActivity{
    private FrameLayout mBannerLayout;

    private LinearLayout mSignUpLayout;

    private TextView mSignUpBtn;


    public LinkedList<AccountBaseFragemt> mFragmentBackStack = new LinkedList<>();
    //底部栏定义
    public enum BottomBar{
        Back, Login, SignUp
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_main_layout);

        findView();

        setListener();

        initView();
    }

    private void findView(){
        mBannerLayout = (FrameLayout) findViewById(R.id.banner_layout);

        mSignUpLayout = (LinearLayout) findViewById(R.id.signup_layout);

        mSignUpBtn = (TextView) findViewById(R.id.btn_signup);
    }


    private void initView(){
        LoginFragement singUpTypeFragment = new LoginFragement();
        addFragment(singUpTypeFragment, false, BottomBar.SignUp);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mBannerLayout.getLayoutParams();
        int P_WIDTH=getResources().getDisplayMetrics().widthPixels;
        params.height = (int) ((401f * P_WIDTH) / 750f);
        mBannerLayout.setLayoutParams(params);

        ArrayList<Integer> imageIdList = new ArrayList<>();
        imageIdList.add(R.drawable.login_banner1);
    }

    public AccountBaseFragemt mCurrtFragemnt;

    //fragment添加/切换
    public void addFragment(AccountBaseFragemt fragment, boolean addBackStack, BottomBar bottomType){
        fragment.setBottomBar(bottomType);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if(addBackStack){
            //将上一个Fragment放入返回栈中，并且隐藏显示
            if(mCurrtFragemnt != null){
                mFragmentBackStack.addLast(mCurrtFragemnt);
                fragmentTransaction.hide(mCurrtFragemnt);
            }

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.add(R.id.content_layout, fragment).commit();
        }else{
            fragmentTransaction.replace(R.id.content_layout, fragment).commit();
        }

        mCurrtFragemnt = fragment;
        showBottomBar(bottomType);
    }

    private void setListener(){

        mSignUpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.setClass(AccountMainActivity.this,RegisterActivity.class);
                startActivity(intent);
            }
        });

    }

    public void showBottomBar(BottomBar bottomType){
        mSignUpLayout.setVisibility(bottomType.equals(BottomBar.SignUp) ? View.VISIBLE : View.GONE);
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class ImagePagerAdapter extends RecyclingPagerAdapter {

        private Context context;
        private List<Integer> imageIdList;

        private int           size;
        private boolean       isInfiniteLoop;

        public ImagePagerAdapter(Context context, List<Integer> imageIdList) {
            this.context = context;
            this.imageIdList = imageIdList;
            this.size = imageIdList.size();
            isInfiniteLoop = false;
        }

        @Override
        public int getCount() {
            return isInfiniteLoop ? Integer.MAX_VALUE : imageIdList.size();
        }

        /**
         * get really position
         *
         * @param position
         * @return
         */
        private int getPosition(int position) {
            return isInfiniteLoop ? position % size : position;
        }

        @Override
        public View getView(int position, View view, ViewGroup container) {
            ViewHolder holder;
            if (view == null) {
                holder = new ViewHolder();
                view = holder.imageView = new ImageView(context);
                view.setTag(holder);
            } else {
                holder = (ViewHolder)view.getTag();
            }

            holder.imageView.setImageResource(imageIdList.get(getPosition(position)));
            return view;
        }

        private class ViewHolder {

            ImageView imageView;
        }

        /**
         * @return the isInfiniteLoop
         */
        public boolean isInfiniteLoop() {
            return isInfiniteLoop;
        }

        /**
         * @param isInfiniteLoop the isInfiniteLoop to set
         */
        public ImagePagerAdapter setInfiniteLoop(boolean isInfiniteLoop) {
            this.isInfiniteLoop = isInfiniteLoop;
            return this;
        }
    }

}
