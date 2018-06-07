package com.lbest.rm.view.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lbest.rm.R;

/**
 * Created by dell on 2017/10/26.
 */

public class GetBackPasswordResultFragment extends BaseFragment {
    private boolean hasCreate=false;
    public GetBackPasswordResultFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.getbackpasswordresult_fragment, container, false);

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
