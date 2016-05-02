package com.mwang.irregulargridview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class SimpleAdapter extends BaseAdapter{

    private ArrayList<String> mDataSet;
    private ArrayList<Integer> widthNums;
    private ArrayList<Integer> heightNums;

    public SimpleAdapter(Context context, RecyclerView rec,
                         ArrayList<String> data, ArrayList<Integer> w, ArrayList<Integer> h){
        super(context, rec);
        mDataSet = data;
        widthNums = w;
        heightNums = h;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(mContext).inflate(R.layout.grid_item_text,parent,false);
        MyViewHolder holder = new MyViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final VH holder, int position){
        ((MyViewHolder)holder).tv.setText(mDataSet.get(position));
        setViewParams(holder.itemView, position);
        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount(){
        return mDataSet.size();
    }

    public static class MyViewHolder extends BaseAdapter.VH {
        public TextView tv;
        public ImageView checkImage;
        public MyViewHolder(View v){
            super(v);
            tv = (TextView)v.findViewById(R.id.tv_num);
            checkImage = (ImageView)v.findViewById(R.id.iv_check);
        }
    }

    @Override
    protected void updateSelectedItem(VH holder){
        ((MyViewHolder)holder).checkImage.setVisibility(View.VISIBLE);
        ((MyViewHolder)holder).tv.setActivated(true);
    }

    @Override
    protected void updateUnselectedItem(VH holder){
        ((MyViewHolder)holder).checkImage.setVisibility(View.GONE);
        ((MyViewHolder)holder).tv.setActivated(false);
    }

    private void setViewParams(View v, int position){
        IrregularLayoutManager.LayoutParams lp = (IrregularLayoutManager.LayoutParams)v.getLayoutParams();
        lp.widthNum = widthNums.get(position);
        lp.heightNum = heightNums.get(position);
    }

}
