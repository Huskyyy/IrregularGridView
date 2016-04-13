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
import java.util.Collections;
import java.util.ListIterator;

public class SimpleImageAdapter extends RecyclerView.Adapter<SimpleImageAdapter.MyViewHolder>{

    private Context mContext;
    private RecyclerView recyclerView;
    private ArrayList<String> mImageDataPath;
    private ArrayList<Integer> mSelectedDataIndexSet;
    private OnItemClickLitener mOnItemClickLitener;

    private int sizePerSpan;

    public SimpleImageAdapter(Context context, RecyclerView rec, ArrayList<String> arr){
        mContext = context;
        recyclerView = rec;
        mImageDataPath = arr;
        mSelectedDataIndexSet = new ArrayList<>();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(mContext).inflate(R.layout.grid_item_image, parent, false);
        MyViewHolder holder = new MyViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position){
        ViewGroup.LayoutParams lp = updateViewParams(mImageDataPath.get(position),
                holder.itemView);

        Glide.with(mContext)
                .load("file://" + mImageDataPath.get(position))
                .override(lp.width, lp.height)
                .centerCrop()
                .placeholder(R.drawable.bg_empty_photo)
                .into(holder.photo);

        if(mSelectedDataIndexSet != null && mSelectedDataIndexSet.contains(new Integer(position)) ){
            updateSelectItem(holder);
        }else{
            updateUnselectItem(holder);
        }

        if(mOnItemClickLitener != null){
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickLitener.onItemClick(holder, holder.getAdapterPosition());
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return mOnItemClickLitener.onItemLongClick(holder, holder.getAdapterPosition());
                }
            });
        }
    }

    @Override
    public int getItemCount(){
        return mImageDataPath.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView photo;
        public ImageView checkImage;
        public MyViewHolder(View v){
            super(v);
            photo = (ImageView)v.findViewById(R.id.iv_photo);
            checkImage = (ImageView)v.findViewById(R.id.iv_check);
        }
    }

    public interface OnItemClickLitener {
        void onItemClick(MyViewHolder holder, int position);
        boolean onItemLongClick(MyViewHolder holder, int position);
    }

    public void setOnItemClickLitener(OnItemClickLitener mOnItemClickLitener)
    {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    public ArrayList<Integer> deleteSelectedItems() {
        ArrayList<Integer> arrayList = new ArrayList<Integer>(mSelectedDataIndexSet);
        if (mSelectedDataIndexSet != null && !mSelectedDataIndexSet.isEmpty()){
            Collections.sort(mSelectedDataIndexSet);
            Collections.copy(arrayList, mSelectedDataIndexSet);
            mSelectedDataIndexSet.clear();
        }
        return arrayList;
    }

    public void resetSelectedItems(){
        if(mSelectedDataIndexSet != null){
            ListIterator<Integer> listIterator = mSelectedDataIndexSet.listIterator();
            while(listIterator.hasNext()){
                int position = listIterator.next();
                MyViewHolder holder = (MyViewHolder)recyclerView
                        .findViewHolderForAdapterPosition(position);
                if(holder != null) {
                    updateUnselectItem(holder);
                }else{
                    notifyItemChanged(position);
                }
            }
            mSelectedDataIndexSet.clear();
        }
    }

    public void reverseSelect(MyViewHolder holder, int position){
        if(mSelectedDataIndexSet.contains(position)){
            mSelectedDataIndexSet.remove(new Integer(position));
            updateUnselectItem(holder);
        }else if(!mSelectedDataIndexSet.contains(position)){
            mSelectedDataIndexSet.add(position);
            updateSelectItem(holder);
        }
    }

    public void selectItem(MyViewHolder holder, int position){
        if(!mSelectedDataIndexSet.contains(position)){
            mSelectedDataIndexSet.add(position);
            updateSelectItem(holder);
        }
    }

    private void updateSelectItem(MyViewHolder holder){
        holder.checkImage.setVisibility(View.VISIBLE);
        holder.photo.setColorFilter(Color.argb(120, 00, 00, 00));
    }

    private void updateUnselectItem(MyViewHolder holder){
        holder.checkImage.setVisibility(View.GONE);
        holder.photo.clearColorFilter();
    }

    public void setSizePerSpan(int val){
        sizePerSpan = val;
    }

    private ViewGroup.LayoutParams updateViewParams(String path, View view){
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
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = widthNum * sizePerSpan;
        lp.height = heightNum * sizePerSpan;
        //view.setLayoutParams(lp);
        return lp;
    }

}
