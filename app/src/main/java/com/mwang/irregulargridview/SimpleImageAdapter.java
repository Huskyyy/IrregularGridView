package com.mwang.irregulargridview;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class SimpleImageAdapter extends BaseAdapter{

    private int sizePerSpan;
    private ArrayList<String> mImageDataPath;

    public SimpleImageAdapter(Context context, RecyclerView rec, ArrayList<String> arr){
        super(context, rec);
        mImageDataPath = arr;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(mContext).inflate(R.layout.grid_item_image, parent, false);
        MyViewHolder holder = new MyViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final VH holder, int position){

        IrregularLayoutManager.LayoutParams lp = setViewParams(mImageDataPath.get(position),
                holder.itemView);

        if(sizePerSpan == 0){
            sizePerSpan = ((IrregularLayoutManager)recyclerView.getLayoutManager()).getSizePerSpan() + 1;
        }

        Glide.with(mContext)
                .load("file://" + mImageDataPath.get(position))
                .override(lp.widthNum * sizePerSpan, lp.heightNum * sizePerSpan)
                .centerCrop()
                .placeholder(R.drawable.bg_empty_photo)
                .into(((MyViewHolder)holder).photo);

        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount(){
        return mImageDataPath.size();
    }

    public static class MyViewHolder extends BaseAdapter.VH {
        public ImageView photo;
        public ImageView checkImage;
        public MyViewHolder(View v){
            super(v);
            photo = (ImageView)v.findViewById(R.id.iv_photo);
            checkImage = (ImageView)v.findViewById(R.id.iv_check);
        }
    }

    @Override
    protected void updateSelectedItem(VH holder){
        ((MyViewHolder)holder).checkImage.setVisibility(View.VISIBLE);
        ((MyViewHolder)holder).photo.setColorFilter(Color.argb(120, 00, 00, 00));
    }

    @Override
    protected void updateUnselectedItem(VH holder){
        ((MyViewHolder)holder).checkImage.setVisibility(View.GONE);
        ((MyViewHolder)holder).photo.clearColorFilter();
    }

    private IrregularLayoutManager.LayoutParams setViewParams(String path, View view){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        int widthNum = 1, heightNum = 1;
        if(imageWidth >= 1200 && imageHeight >= 1200){
            widthNum = 2;
            heightNum = 2;
        }else if(imageWidth >= imageHeight * 1.5){
            widthNum = 2;
        }else if(imageHeight >= imageWidth * 1.3){
            heightNum = 2;
        }
        IrregularLayoutManager.LayoutParams lp = (IrregularLayoutManager.LayoutParams)view.getLayoutParams();
        lp.widthNum = widthNum;
        lp.heightNum = heightNum;
        return lp;
    }

}
