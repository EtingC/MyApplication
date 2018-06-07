package com.lbest.rm.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.lbest.rm.view.fragment.DeviceListFragment;
import com.lbest.rm.view.fragment.MeFragment;
import com.lbest.rm.view.fragment.SceneFragment;

import java.util.HashMap;
import java.util.List;

/**
 * Created by dell on 2017/11/22.
 */

public class PageViewAdapter extends FragmentPagerAdapter{

    private List<String> pageTitleList;

    public void setPageTitleList(List<String> pageTitleList) {
        this.pageTitleList = pageTitleList;
    }

    private HashMap<Integer,Fragment> fragmentHashMap=new HashMap<>();
    public PageViewAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return pageTitleList.get(position);
    }

    @Override
    public Fragment getItem(int position) {
        if(position==0){
            Fragment fragment=new DeviceListFragment();
            fragmentHashMap.put(position,fragment);
            return fragment;
        }else if(position==1){
            Fragment fragment=new SceneFragment();
            fragmentHashMap.put(position,fragment);
            return fragment;
        }else if(position==2){
            Fragment  fragment=new MeFragment();
            fragmentHashMap.put(position,fragment);
            return fragment;
        }
        return null;
    }

    @Override
    public int getCount() {
        return pageTitleList==null?0:pageTitleList.size();
    }

    public Fragment getItemObject(int position){
        return fragmentHashMap.get(position);
    }
}
