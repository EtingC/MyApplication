package com.lbest.rm.view.fragment;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.lbest.rm.AccountMainActivity;
import com.lbest.rm.Constants;
import com.lbest.rm.DNAH5Activity;
import com.lbest.rm.HomeActivity;
import com.lbest.rm.LoadingActivity;
import com.lbest.rm.MyApplication;
import com.lbest.rm.ProductListActivity;
import com.lbest.rm.R;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.adapter.UserDeviceListAdapter;
import com.lbest.rm.data.RefreshTokenResult;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.productDevice.DeviceManager;
import com.lbest.rm.utils.ImageLoaderUtils;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.lbest.rm.utils.Logutils;

import java.util.ArrayList;
import java.util.List;

import cn.com.broadlink.sdk.result.account.BLBaseResult;

/**
 * Created by dell on 2017/10/26.
 */

public class DeviceListFragment extends BaseFragment {

    private PullToRefreshListView ptrlv_devicelist;
    private LinearLayout ll_nulldevice;
    private List<FamilyDeviceModuleData> deviceInfoList = new ArrayList<>();
    private UserDeviceListAdapter adapter;
    private BLAcountToAli blAcountToAli;
    private ImageLoaderUtils mImageLoaderUtils;
    private HomeActivity mActivity;
    private boolean hasCreate=false;
    private boolean needrefresh=false;
    private Handler mHandler;
    public DeviceListFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mHandler=new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logutils.log_d("device list onCreateView");
        View mView = inflater.inflate(R.layout.devicelist_fragment, container, false);
        mImageLoaderUtils = ImageLoaderUtils.getInstence(getActivity().getApplicationContext());
        mActivity= (HomeActivity) getActivity();
        blAcountToAli=BLAcountToAli.getInstance();
        findview(mView);
        initView();
        setListener();
        hasCreate=true;


        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                needrefresh=true;
                ptrlv_devicelist.setRefreshing();
            }
        },500);

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Logutils.log_d("device list onResume");
        reloadDeviceList();
    }

    @Override
    public void onPause() {
        super.onPause();
        ptrlv_devicelist.onRefreshComplete();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.userdevice_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_adddevice:
                Intent intent1 = new Intent();
                intent1.setClass(mActivity, ProductListActivity.class);
                startActivity(intent1);
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(hasCreate){
            Logutils.log_d("device list setUserVisibleHint "+isVisibleToUser);
            if(isVisibleToUser){
//                needrefresh=false;
//                ptrlv_devicelist.setRefreshing();
                reloadDeviceList();
            }else{
                ptrlv_devicelist.onRefreshComplete();
            }
        }
    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if(requestCode==10001){
//            reloadDeviceList();
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    private void findview(View rootView){
        ptrlv_devicelist = (PullToRefreshListView) rootView.findViewById(R.id.lv_devicelist);
        ll_nulldevice= (LinearLayout) rootView.findViewById(R.id.ll_nulldevice);
    }

    private void setListener(){
        // Set a listener to be invoked when the list should be refreshed.
        ptrlv_devicelist.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                String label = DateUtils.formatDateTime(mActivity.getApplicationContext(), System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);

                // Update the LastUpdatedLabel
                refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
                if(needrefresh){
                    refreshDeviceList();
                }else{
                    reloadDeviceList();
                }
                needrefresh=true;
            }
        });

        // Add an end-of-list listener
        ptrlv_devicelist.setOnLastItemVisibleListener(new PullToRefreshBase.OnLastItemVisibleListener() {

            @Override
            public void onLastItemVisible() {
                //Toast.makeText(HomeActivity.this, "End of List!", Toast.LENGTH_SHORT).show();
            }
        });


        adapter.setActionClickListener(new UserDeviceListAdapter.onActionClickListener() {
            @Override
            public void actionClick(int position, UserDeviceListAdapter.ViewHolder viewHolder) {

            }
        });


        adapter.setItemClickListener(new UserDeviceListAdapter.onItemClickListener() {
            @Override
            public void itemClick(int position, UserDeviceListAdapter.ViewHolder viewHolder) {
                FamilyDeviceModuleData deviceInfo = (FamilyDeviceModuleData) adapter.getItem(position);
                Intent intent = new Intent();
                intent.setClass(mActivity, DNAH5Activity.class);
                intent.putExtra(Constants.INTENT_DEVICE, deviceInfo);
                startActivityForResult(intent,10001);
            }
        });
    }

    private void initView(){
        adapter = new UserDeviceListAdapter(mActivity, deviceInfoList);
        ptrlv_devicelist.setAdapter(adapter);
    }

    //刷新服务器设备列表
    private void refreshDeviceList() {
        Logutils.log_d("device list refreshDeviceList");
        new refreshDeviceListTask().executeOnExecutor(MyApplication.FULL_TASK_EXECUTOR);
    }
    //刷新本地设备列表
    private void reloadDeviceList() {
        Logutils.log_d("device list reloadDeviceList");
        new loadDeviceListTask().executeOnExecutor(MyApplication.FULL_TASK_EXECUTOR);
    }

    class refreshDeviceListTask extends AsyncTask<Void,Void,List<FamilyDeviceModuleData>>{
        private RefreshTokenResult refreshTokenResult=null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<FamilyDeviceModuleData> doInBackground(Void... params) {
            List<FamilyDeviceModuleData> result=blAcountToAli.refreshUserDeviceListV2();
            if(result==null){
                String old_refresh_token = blAcountToAli.getBlUserInfo().getRefresh_token();
                String old_access_token = blAcountToAli.getBlUserInfo().getAccess_token();
                Logutils.log_d("refreshDeviceListTask refreshAccessToken old_refresh_token:"+old_refresh_token+"   old_access_token:"+old_access_token);
                refreshTokenResult =  blAcountToAli.refreshToken(old_refresh_token);
                if(refreshTokenResult.isSuccess()){
                    return blAcountToAli.refreshUserDeviceListV1();
                }
                return null;
            }else{
                Logutils.log_d("refreshDeviceListTask success");
                return result;
            }
        }

        @Override
        protected void onPostExecute(final List<FamilyDeviceModuleData> familyDeviceModuleDatas) {
            super.onPostExecute(familyDeviceModuleDatas);
            if(mActivity==null||mActivity.isFinishing())
                return;


            if(refreshTokenResult!=null&&!refreshTokenResult.isSuccess()){
                BLBaseResult baseResult=refreshTokenResult.getResult();
                int code=refreshTokenResult.getCode();
                if(code==-3){
                    if(baseResult!=null){
                        Toast.makeText(mActivity,"error:"+baseResult.getError(),Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(mActivity,mActivity.getResources().getString(R.string.err_network),Toast.LENGTH_SHORT).show();
                    }
                }else {
                    if(code==-1){
                        Toast.makeText(mActivity,mActivity.getResources().getString(R.string.str_loginfail),Toast.LENGTH_SHORT).show();
                    }else if(code==-2){
                        Toast.makeText(mActivity,"token is null",Toast.LENGTH_SHORT).show();
                    }
//                    Intent intent = new Intent();
//                    intent.setClass(mActivity, AccountMainActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    mActivity.finish();
                }
                ptrlv_devicelist.onRefreshComplete();
                return;
            }

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    deviceInfoList.clear();
                    if(familyDeviceModuleDatas != null){
                        deviceInfoList.addAll(familyDeviceModuleDatas);
                    }
                    adapter.setDeviceInfoList(deviceInfoList);
                    adapter.notifyDataSetChanged();
                    DeviceManager.getInstance().refreshDeviceList(deviceInfoList);

                    ptrlv_devicelist.onRefreshComplete();

                    if(adapter.getCount()<=0){
                        ll_nulldevice.setVisibility(View.VISIBLE);
                    }else{
                        ll_nulldevice.setVisibility(View.GONE);
                    }
                }
            }, 5000);
        }
    }


    class loadDeviceListTask extends AsyncTask<Void,Void,List<FamilyDeviceModuleData>>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<FamilyDeviceModuleData> doInBackground(Void... params) {
            return BLAcountToAli.getInstance().reloadUserDeviceList();
        }

        @Override
        protected void onPostExecute(final List<FamilyDeviceModuleData> familyDeviceModuleDatas) {
            super.onPostExecute(familyDeviceModuleDatas);
            deviceInfoList.clear();
            if(familyDeviceModuleDatas != null){
                deviceInfoList.addAll(familyDeviceModuleDatas);
            }
            adapter.setDeviceInfoList(deviceInfoList);
            adapter.notifyDataSetChanged();
            DeviceManager.getInstance().refreshDeviceList(deviceInfoList);

            ptrlv_devicelist.onRefreshComplete();
            if(adapter.getCount()<=0){
                ll_nulldevice.setVisibility(View.VISIBLE);
            }else{
                ll_nulldevice.setVisibility(View.GONE);
            }
        }
    }
}
