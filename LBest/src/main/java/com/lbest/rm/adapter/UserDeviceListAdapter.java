package com.lbest.rm.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.lbest.rm.Constants;
import com.lbest.rm.R;

import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.data.AliDeviceStatusResult;
import com.lbest.rm.data.ParamsMap;
import com.lbest.rm.data.ShowParamData;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.utils.ImageLoaderUtils;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.SwipeListItemLayout;
import com.broadlink.lib.imageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;
import java.util.List;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.data.controller.BLStdData;
import cn.com.broadlink.sdk.param.controller.BLStdControlParam;
import cn.com.broadlink.sdk.result.controller.BLStdControlResult;

/**
 * Created by dell on 2017/10/25.
 */

public class UserDeviceListAdapter extends BaseAdapter {
    private Activity mActivity;
    private LayoutInflater mInflater;
    private List<FamilyDeviceModuleData> deviceInfoList = null;
    private ImageLoaderUtils mImageLoaderUtils;
    private boolean canSwipe = false;
    private onSwitchClickListener switchClickListener;
    private onActionClickListener actionClickListener;
    private onItemClickListener itemClickListener;

    public void setDeviceInfoList(List<FamilyDeviceModuleData> deviceInfoList) {
        this.deviceInfoList = deviceInfoList;
    }

    public void setSwitchClickListener(onSwitchClickListener switchClickListener) {
        this.switchClickListener = switchClickListener;
    }

    public void setActionClickListener(onActionClickListener actionClickListener) {
        this.actionClickListener = actionClickListener;
    }

    public void setItemClickListener(onItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public interface onSwitchClickListener {
        public void switchClick(int position, ViewHolder viewHolder);
    }

    public interface onActionClickListener {
        public void actionClick(int position, ViewHolder viewHolder);
    }

    public interface onItemClickListener {
        public void itemClick(int position, ViewHolder viewHolder);
    }

    public UserDeviceListAdapter(Activity mActivity, List<FamilyDeviceModuleData> datalist) {
        this.mActivity = mActivity;
        this.mImageLoaderUtils = ImageLoaderUtils.getInstence(mActivity);
        this.deviceInfoList = datalist;
        this.mInflater = LayoutInflater.from(mActivity);
    }


    @Override
    public int getCount() {
        return deviceInfoList == null ? 0 : deviceInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return deviceInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.userdevicelist_item_layout, null);
            viewHolder.iv_icon = (ImageView) convertView.findViewById(R.id.iv_icon);
            viewHolder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
            viewHolder.tv_status = (TextView) convertView.findViewById(R.id.tv_status);
            viewHolder.tv_action = (TextView) convertView.findViewById(R.id.tv_action);
            viewHolder.slil_item = (SwipeListItemLayout) convertView.findViewById(R.id.slil_item);
            viewHolder.rl_item = (RelativeLayout) convertView.findViewById(R.id.rl_item);

            viewHolder.divider1 =convertView.findViewById(R.id.divider1);
            viewHolder.divider2 =convertView.findViewById(R.id.divider2);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if(position==0){
            viewHolder.divider1.setVisibility(View.INVISIBLE);
        }else{
            viewHolder.divider1.setVisibility(View.VISIBLE);
        }
        if(position==(getCount()-1)){
            viewHolder.divider2.setVisibility(View.VISIBLE);
        }else{
            viewHolder.divider2.setVisibility(View.INVISIBLE);
        }
        final FamilyDeviceModuleData baseDeviceInfo = deviceInfoList.get(position);
        String iconPath=baseDeviceInfo.getModuleIcon();
        if(!TextUtils.isEmpty(iconPath)){
            Log.i("mrmr","iconPath = "+iconPath);
            if(iconPath.equals("file:///android_asset/lb1.png") || iconPath.contains("X23-X30")){
                Bitmap bitmap= null;

                iconPath = "file:///android_asset/lb1.png";
                try {
                    String filename=iconPath.substring(22,iconPath.length());
                    bitmap = BitmapFactory.decodeStream( mActivity.getAssets().open(filename));
                    viewHolder.iv_icon.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                mImageLoaderUtils.displayImage(iconPath, viewHolder.iv_icon, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        super.onLoadingComplete(imageUri, view, loadedImage);
                        if (loadedImage == null) {
                            //((ImageView) view).setImageResource(R.drawable.default_module_icon);
                            //view.setTag(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_module_icon));
                        } else {
                            view.setTag(loadedImage);
                        }
                    }
                });
            }
        }
        viewHolder.slil_item.setCanSwipe(canSwipe);
        String nickName = baseDeviceInfo.getModuleName();
        if (TextUtils.isEmpty(nickName)) {
            viewHolder.tv_name.setText(baseDeviceInfo.getName());
        } else {
            viewHolder.tv_name.setText(nickName);
        }
        viewHolder.tv_action.setText(mActivity.getResources().getString(R.string.str_deletedevice));


        final ShowParamData showParamData = Product.getShowParamsByPid(mActivity,baseDeviceInfo.getPid());
        if(showParamData!=null){
            //刷新设备状态
            updateItemStatus(viewHolder, baseDeviceInfo, showParamData);
        }

        viewHolder.rl_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.itemClick(position, viewHolder);
                }
            }
        });

        viewHolder.tv_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionClickListener != null) {
                    actionClickListener.actionClick(position, viewHolder);
                }
            }
        });

        return convertView;
    }

    public static class ViewHolder {
        public ImageView iv_icon;
        public TextView tv_name;
        public TextView tv_status;
        public TextView tv_action;
        public View divider1;
        public View divider2;
        public RelativeLayout rl_item;
        public SwipeListItemLayout slil_item;
    }


    public void updateItemStatus(final ViewHolder viewHolder, FamilyDeviceModuleData familyDeviceModuleData, final ShowParamData showParamData) {
        new updateDeviceStatus(viewHolder,familyDeviceModuleData,showParamData).execute();
    }


    class updateDeviceStatus extends AsyncTask<Void, Void, BLStdControlResult> {
        private ViewHolder viewHolder;
        private FamilyDeviceModuleData baseDeviceInfo;
        private ShowParamData showParamData;

        public updateDeviceStatus(ViewHolder viewHolder, FamilyDeviceModuleData baseDeviceInfo, ShowParamData showParamData) {
            this.viewHolder = viewHolder;
            this.baseDeviceInfo = baseDeviceInfo;
            this.showParamData = showParamData;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected BLStdControlResult doInBackground(Void... params) {
            BLStdControlParam blStdControlParam=new BLStdControlParam();
            blStdControlParam.setAct("get");
//            blStdControlParam.getParams().add("rack_drying");
//            blStdControlParam.getParams().add("rack_drytime");
//            ArrayList<BLStdData.Value> values=new ArrayList<>();
//            BLStdData.Value val=new BLStdData.Value();
//            val.setVal(1);
//            val.setIdx(0);
//            values.add(val);
//            blStdControlParam.getVals().add(values);
//
//            ArrayList<BLStdData.Value> values2=new ArrayList<>();
//            BLStdData.Value val2=new BLStdData.Value();
//            val2.setVal(60);
//            val2.setIdx(0);
//            values2.add(val2);
//            blStdControlParam.getVals().add(values2);
            return  BLLet.Controller.dnaControl(baseDeviceInfo.getDid(),baseDeviceInfo.getsDid(),blStdControlParam);
        }

        @Override
        protected void onPostExecute(BLStdControlResult blStdControlResult) {
            super.onPostExecute(blStdControlResult);
            if(blStdControlResult!=null){
                Logutils.log_d("update dev statue:"+ JSON.toJSONString(blStdControlResult));
                if(blStdControlResult.succeed()&&blStdControlResult.getData()!=null){
                    ArrayList<String> paramList= blStdControlResult.getData().getParams();
                    ArrayList<ArrayList<BLStdData.Value>> valueList=blStdControlResult.getData().getVals();

                    ArrayList<ShowParamData.ParamData> paramDataArrayList=showParamData.getParam_list();
                    int show_grade=0;
                    StringBuffer stringBuffer=new StringBuffer();
                    for(ShowParamData.ParamData showParamData:paramDataArrayList){
                        if(showParamData.show_grade>=show_grade){
                            if(paramList!=null){
                                int size=paramList.size();
                                if(size>0){
                                    for(int i=0;i<size;i++){
                                        String paraName=paramList.get(i);
                                        if(paraName.equals(showParamData.param)){
                                            Object value=valueList.get(i).get(0).getVal();
                                            if(showParamData.judge_way.equals("*")){
                                                stringBuffer.append(showParamData.name+" "+value+showParamData.util);
                                                stringBuffer.append(" ");
                                            }else if(showParamData.judge_way.equals(">")){
                                                int val= (int) value;
                                                if(val>Integer.parseInt(showParamData.show_condition_val)){
                                                    if(showParamData.show_grade>show_grade){
                                                        int  sb_length = stringBuffer.length();
                                                        stringBuffer.delete(0,sb_length);
                                                    }
                                                    stringBuffer.append(showParamData.name+" "+value+showParamData.util);
                                                    stringBuffer.append(" ");

                                                    show_grade=showParamData.show_grade;
                                                }
                                            }else if(showParamData.judge_way.equals("<")){
                                                int val= (int) value;
                                                if(val<Integer.valueOf(showParamData.show_condition_val)){
                                                    if(showParamData.show_grade>show_grade){
                                                        int  sb_length = stringBuffer.length();
                                                        stringBuffer.delete(0,sb_length);
                                                    }
                                                    stringBuffer.append(showParamData.name+" "+value+showParamData.util);
                                                    stringBuffer.append(" ");

                                                    show_grade=showParamData.show_grade;
                                                }
                                            }else if(showParamData.judge_way.equals("=")){
                                                int val= (int) value;
                                                if(val==Integer.valueOf(showParamData.show_condition_val)){
                                                    if(showParamData.show_grade>show_grade){
                                                        int  sb_length = stringBuffer.length();
                                                        stringBuffer.delete(0,sb_length);
                                                    }
                                                    stringBuffer.append(showParamData.name+" "+value+showParamData.util);
                                                    stringBuffer.append(" ");

                                                    show_grade=showParamData.show_grade;
                                                }
                                            }else if(showParamData.judge_way.equals(">=")){
                                                int val= (int) value;
                                                if(val>=Integer.valueOf(showParamData.show_condition_val)){
                                                    if(showParamData.show_grade>show_grade){
                                                        int  sb_length = stringBuffer.length();
                                                        stringBuffer.delete(0,sb_length);
                                                    }
                                                    stringBuffer.append(showParamData.name+" "+value+showParamData.util);
                                                    stringBuffer.append(" ");

                                                    show_grade=showParamData.show_grade;
                                                }
                                            }else if(showParamData.judge_way.equals("<=")){
                                                int val= (int) value;
                                                if(val<=Integer.valueOf(showParamData.show_condition_val)){
                                                    if(showParamData.show_grade>show_grade){
                                                        int  sb_length = stringBuffer.length();
                                                        stringBuffer.delete(0,sb_length);
                                                    }
                                                    stringBuffer.append(showParamData.name+" "+value+showParamData.util);
                                                    stringBuffer.append(" ");

                                                    show_grade=showParamData.show_grade;
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    String data=stringBuffer.toString().trim();
                    if(TextUtils.isEmpty(data)){
                        viewHolder.tv_status.setText(mActivity.getResources().getString(R.string.str_nowork));
                    }else{
                        viewHolder.tv_status.setText(data);
                    }
                }
            }
        }
    }
}