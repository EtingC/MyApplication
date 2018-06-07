package com.lbest.rm.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lbest.rm.R;
import com.lbest.rm.data.productInfo;
import com.lbest.rm.utils.ImageLoaderUtils;
import com.broadlink.lib.imageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dell on 2017/10/25.
 */

public class ProductListAdapter  extends BaseAdapter {
    private Context mContext;
    private LayoutInflater mInflater;
    private List<productInfo> productList=null;
    private ImageLoaderUtils mImageLoaderUtils;

    private onItemClickListener itemClickListener;

    public void setItemClickListener(onItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public interface onItemClickListener{
        public void itemClick(int position,UserDeviceListAdapter.ViewHolder viewHolder);
    }
    public void setProductList(List<productInfo> productList) {
        this.productList = productList;
    }

    public ProductListAdapter(Context context, List<productInfo>datalist) {
        this.mContext = context;
        this.mImageLoaderUtils = ImageLoaderUtils.getInstence(context);
        this.productList = datalist;
        this.mInflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return productList==null?0:productList.size();
    }

    @Override
    public Object getItem(int position) {
        return productList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if(convertView==null){
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.productitem_layout, null);
            viewHolder.rl_productitem= (RelativeLayout) convertView.findViewById(R.id.rl_productitem);
            viewHolder.iv_icon = (ImageView) convertView.findViewById(R.id.iv_icon);
            viewHolder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
            viewHolder.tv_des = (TextView) convertView.findViewById(R.id.tv_des);
            viewHolder.divider1 =convertView.findViewById(R.id.divider1);
            viewHolder.divider2 =convertView.findViewById(R.id.divider2);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if(position==(getCount()-1)){
            viewHolder.divider2.setVisibility(View.VISIBLE);
        }else{
            viewHolder.divider2.setVisibility(View.INVISIBLE);
        }


        productInfo datainfo=productList.get(position);
        String iconPath=datainfo.getIcon();
        if(iconPath.startsWith("file:")){
            Bitmap bitmap= null;
            try {
                String filename=iconPath.substring(22,iconPath.length());
                bitmap = BitmapFactory.decodeStream( mContext.getAssets().open(filename));
                viewHolder.iv_icon.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            mImageLoaderUtils.displayImage(datainfo.getIcon(), viewHolder.iv_icon, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    super.onLoadingComplete(imageUri, view, loadedImage);
                    if(loadedImage == null){
                        //((ImageView) view).setImageResource(R.drawable.default_module_icon);
                        //view.setTag(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_module_icon));
                    }else{
                        view.setTag(loadedImage);
                    }
                }
            });
        }
        viewHolder.rl_productitem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(itemClickListener!=null){
                    itemClickListener.itemClick(position,viewHolder);
                }
            }
        });
        String deviceName=datainfo.getDeviceName();
        String[] arrStr=null;
        if(deviceName!=null){
            arrStr=deviceName.split("-");
        }
        if(arrStr!=null&&arrStr.length>1){
            viewHolder.tv_name.setText(arrStr[0]);
            viewHolder.tv_des.setText(arrStr[1]);
        }else{
            viewHolder.tv_name.setText(datainfo.getDeviceName());
            viewHolder.tv_des.setText(datainfo.getModel());
        }
        return convertView;
    }

    static class ViewHolder extends UserDeviceListAdapter.ViewHolder {
        public ImageView iv_icon;
        public TextView tv_name;
        public TextView tv_des;
        public View divider1;
        public View divider2;
        public RelativeLayout rl_productitem;
    }
}