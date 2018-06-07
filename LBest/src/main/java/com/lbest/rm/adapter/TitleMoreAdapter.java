package com.lbest.rm.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lbest.rm.R;

/**
 * Created by dell on 2017/11/2.
 */

public abstract class TitleMoreAdapter extends BaseAdapter implements TitleMoreAdapterInterfacer{

    private LayoutInflater mInflater;

    public TitleMoreAdapter(Context context){
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return getItemCount();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TitleViewHolder viewHolder;
        if(convertView == null){
            viewHolder = new TitleViewHolder();
            convertView = mInflater.inflate(R.layout.title_more_item_layout, null);
            viewHolder.moreIconView = (ImageView) convertView.findViewById(R.id.more_icon_view);
            viewHolder.moreTextView = (TextView) convertView.findViewById(R.id.more_textview);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (TitleViewHolder) convertView.getTag();
        }

        getView(position, viewHolder);

        return convertView;
    }

    public class TitleViewHolder{
        public ImageView moreIconView;
        public TextView moreTextView;
    }

}