package com.lbest.rm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.aliyun.alink.business.login.IAlinkLoginCallback;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.adapter.ProductListAdapter;
import com.lbest.rm.adapter.UserDeviceListAdapter;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.data.productInfo;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.InputTextView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import java.util.ArrayList;
import java.util.List;

public class ProductListActivity extends Activity {
    private PullToRefreshListView ptrlv_productlist;
    private List<productInfo> productList=new ArrayList<>();
    private ProductListAdapter adapter;

    private Toolbar toolbar;
    private TextView toolbar_title;

    private InputTextView it_model;
    private TextView tv_cancle;
    private LinearLayout ll_searchcontainer;
    private RelativeLayout rl_search;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productlist_layout);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        findView();
        initView();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }


    private void findView() {
        ptrlv_productlist=(PullToRefreshListView)findViewById(R.id.lv_productlist);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);

        tv_cancle = (TextView) findViewById(R.id.tv_cancle);
        it_model = (InputTextView) findViewById(R.id.it_model);
        ll_searchcontainer= (LinearLayout) findViewById(R.id.ll_searchcontainer);
        rl_search= (RelativeLayout) findViewById(R.id.rl_search);
    }


    private void initView() {
        adapter=new ProductListAdapter(ProductListActivity.this,productList);
        ptrlv_productlist.setAdapter(adapter);

        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_adddevice));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));

        it_model.setTextColor(getResources().getColor(R.color.tabgray));
        it_model.setTextSize(13);
        it_model.clearFocus();
    }


    private void setListener(){
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProductListActivity.this.finish();
            }
        });

        adapter.setItemClickListener(new ProductListAdapter.onItemClickListener() {
            @Override
            public void itemClick(int position, UserDeviceListAdapter.ViewHolder viewHolder) {
                productInfo product= (productInfo) adapter.getItem(position);
                Intent intent=new Intent();
                intent.putExtra(Constants.INTENT_PRODUCTINFO,product);
                intent.setClass(ProductListActivity.this,DeviceConfigActivity.class);
                startActivity(intent);

            }
        });

        // Set a listener to be invoked when the list should be refreshed.
        ptrlv_productlist.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                String label = DateUtils.formatDateTime(getApplicationContext(), System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);

                // Update the LastUpdatedLabel
                refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);

                initData();
            }
        });

        // Add an end-of-list listener
        ptrlv_productlist.setOnLastItemVisibleListener(new PullToRefreshBase.OnLastItemVisibleListener() {

            @Override
            public void onLastItemVisible() {
                //Toast.makeText(ProductListActivity.this, "End of List!", Toast.LENGTH_SHORT).show();
            }
        });

        it_model.setTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Logutils.log_d("onTouch");

                RelativeLayout.LayoutParams lp= (RelativeLayout.LayoutParams) rl_search.getLayoutParams();
                int margin=getResources().getDimensionPixelSize(R.dimen.productlist_search_bt_width)+20;
                lp.setMargins(0,0,margin,0);
                rl_search.setLayoutParams(lp);


                ViewGroup.LayoutParams lp1=ll_searchcontainer.getLayoutParams();
                lp1.width=ViewGroup.LayoutParams.MATCH_PARENT;
                ll_searchcontainer.setLayoutParams(lp1);

                ViewGroup.LayoutParams lp2=it_model.getLayoutParams();
                lp2.width=ViewGroup.LayoutParams.MATCH_PARENT;
                it_model.setLayoutParams(lp2);
                tv_cancle.setVisibility(View.VISIBLE);

                it_model.setTextColor(getResources().getColor(R.color.tabgray_selected));
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    it_model.getEditText().requestFocus();
                    imm.showSoftInput(it_model.getEditText(), 0);
                }
                return false;
            }
        });
        tv_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                RelativeLayout.LayoutParams lp= (RelativeLayout.LayoutParams) rl_search.getLayoutParams();
                lp.setMargins(0,0,0,0);
                rl_search.setLayoutParams(lp);

                ViewGroup.LayoutParams lp2=it_model.getLayoutParams();
                lp2.width=ViewGroup.LayoutParams.WRAP_CONTENT;
                it_model.setLayoutParams(lp2);

                ViewGroup.LayoutParams lp1=ll_searchcontainer.getLayoutParams();
                lp1.width=ViewGroup.LayoutParams.WRAP_CONTENT;
                ll_searchcontainer.setLayoutParams(lp1);
                tv_cancle.setVisibility(View.GONE);
                it_model.setTextColor(getResources().getColor(R.color.tabgray));
                it_model.resetView();
                adapter.setProductList(productList);
                adapter.notifyDataSetChanged();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                }
            }
        });

        it_model.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Logutils.log_d("beforeTextChanged:"+s+"  "+start+"   "+count+"   "+after);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Logutils.log_d("onTextChanged:"+s+"  "+start+"   "+before+"   "+count);
            }

            @Override
            public void afterTextChanged(Editable s) {
                Logutils.log_d("afterTextChanged:"+s);
                if(productList.size()>0){
                    List<productInfo> productListTMP=new ArrayList<productInfo>();
                    for(productInfo product:productList){
                        String productName=product.getDeviceName().toLowerCase();
                        //String productModel=product.getModel().toLowerCase();
                        String s_lowcase=s.toString().toLowerCase();
                        if(productName.contains(s_lowcase)){
                            productListTMP.add(product);
                        }
                    }
                    adapter.setProductList(productListTMP);
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    public void initData(){
        Product.getProductList(new Product.ProductResultCallBack(ProductListActivity.this) {
            @Override
            public void callBack(int code, String msg, Object data) {
                Logutils.log_d("ProductListActivity initData getProductList:"+msg+"  "+code);
                super.callBack(code, msg, data);
                productList.clear();
                if (data != null) {
                    try {
                        JSONArray jsonArray = JSON.parseArray(data.toString());
                        if (jsonArray != null && jsonArray.size() > 0) {
                            for (Object obj : jsonArray) {
                                productInfo product = JSON.parseObject(obj.toString(), productInfo.class);

                                String model=product.getModel();
                                try{
                                    String[] array = model.split("_");
                                    if (array != null) {
                                        int lenght = array.length;
                                        if (lenght > 1) {
                                            int deviceType = Integer.parseInt(array[lenght - 2]);
                                            if(deviceType==Constants.AliProductTypeDefine.COMMON||deviceType==Constants.AliProductTypeDefine.GETWAY){
                                                productList.add(product);
                                            }
                                        }
                                    }
                                }catch (Exception e){
                                    Logutils.log_w("",e);
                                    //productList.add(product);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logutils.log_w("parse product result exception:", e);
                    }
                }

                productInfo oldproductInfo=new productInfo();
                oldproductInfo.setBrandName("LBestGB");
                oldproductInfo.setDeviceName("智能晾衣机-X23 X30 X50 K3 K5 M08 X60 Z80");
                oldproductInfo.setModel(Constants.LBESTOLDMODEL);
                oldproductInfo.setIcon("file:///android_asset/lb1.png");
                productList.add(oldproductInfo);

//                productInfo addproductInfo=new productInfo();
//                addproductInfo.setBrandName("LBestGB");
//                addproductInfo.setDeviceName("智能晾衣机-X23 X30 X50 K3 K5 M08 X60 Z80");
//                addproductInfo.setModel(Constants.LBESTOLDMODEL);
//                addproductInfo.setIcon("file:///android_asset/lb1.png");
//                productList.add(addproductInfo);

                adapter.setProductList(productList);
                adapter.notifyDataSetChanged();
                ptrlv_productlist.onRefreshComplete();
            }
        });
    }
}
