package com.lbest.rm.view.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.alibaba.fastjson.JSON;
import com.lbest.rm.Constants;
import com.lbest.rm.DeviceConfigActivity;
import com.lbest.rm.R;
import com.lbest.rm.data.ProductInitAction;
import com.lbest.rm.data.productInfo;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.utils.ImageLoaderUtils;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.utils.NetworkUtils;
import com.broadlink.lib.imageloader.core.listener.SimpleImageLoadingListener;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by dell on 2017/10/27.
 */

public class ProductInfoFragment extends BaseFragment{

    private DeviceConfigActivity mActivity;
    private TextView tv_des;
    private ImageView iv_icon;
    private ImageLoaderUtils mImageLoaderUtils;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.productinfo_fragment_layout, container, false);
        mActivity=(DeviceConfigActivity)getActivity();
        mActivity.setNextButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showConfigWifiFragment();
            }
        });
        mActivity.setNextButton(getResources().getString(R.string.str_next),getResources().getColor(R.color.colorAccent),true);

        mImageLoaderUtils = ImageLoaderUtils.getInstence(mActivity);
        findView(view);
        initView();
        return view;
    }

    private void findView(View rootView) {
        tv_des = (TextView) rootView.findViewById(R.id.tv_des);
        iv_icon = (ImageView) rootView.findViewById(R.id.iv_icon);
    }

    private void initView() {
        if(mActivity.getProduct()!=null){
            String iconPath="file:///android_asset/lb2.png";
            Bitmap bitmap= null;
            String filename=iconPath.substring(22,iconPath.length());
            try {
                bitmap = BitmapFactory.decodeStream(mActivity.getAssets().open(filename));
                iv_icon.setImageBitmap(bitmap);
            }catch (Exception e) {
                  e.printStackTrace();
            }



//            if(iconPath.startsWith("file:")){
//                Bitmap bitmap= null;
//                try {
//                    String filename=iconPath.substring(22,iconPath.length());
//                    bitmap = BitmapFactory.decodeStream( mActivity.getAssets().open(filename));
//                    iv_icon.setImageBitmap(bitmap);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }else{
//                mImageLoaderUtils.displayImage(mActivity.getProduct().getIcon(), iv_icon, new SimpleImageLoadingListener() {
//                    @Override
//                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                        super.onLoadingComplete(imageUri, view, loadedImage);
//                        if(loadedImage == null){
//                            //((ImageView) view).setImageResource(R.drawable.default_module_icon);
//                            //view.setTag(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_module_icon));
//                        }else{
//                            view.setTag(loadedImage);
//                        }
//                    }
//                });
//            }

            if(Constants.LBESTOLDMODEL.equals(mActivity.getProduct().getModel())){
                tv_des.setText(getResources().getString(R.string.str_lbestoldmodel_state));
            }else{
                Product.getProductDetail(mActivity.getProduct().getModel(), new Product.ProductResultCallBack(getActivity()) {
                    @Override
                    public void callBack(int code, String msg, Object data) {
                        Logutils.log_d("ProductInfoFragment initView getProductDetail:"+msg+"  "+code);
                        super.callBack(code, msg, data);
                        if(data!=null){
                            productInfo product= (productInfo) data;
                            ProductInitAction productInitAction=JSON.parseObject(product.getInitAction(),ProductInitAction.class);
                            String des=productInitAction.getInitAction();
                            String[] strArry=des.split("\\\\n");
                            int index=0;
                            if(strArry!=null&&strArry.length>1){
                                StringBuffer sb=new StringBuffer();
                                for(String str:strArry){
                                    sb.append(str);
                                    if(index<(strArry.length-1))
                                        sb.append("\n");
                                    index++;
                                }
                                tv_des.setText(sb.toString());
                            }else{
                                tv_des.setText(des);
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            mActivity.setNextButton(getResources().getString(R.string.str_next),getResources().getColor(R.color.colorAccent),true);
            mActivity.setNextButtonListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.showConfigWifiFragment();
                }
            });
        }
    }

}
