package com.mwang.irregulargridview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.MyViewHolder>{

    private Context mContext;
    private RecyclerView recyclerView;
    private ArrayList<String> mDataSet;
    private ArrayList<Integer> mSelectedDataIndexSet;
    private OnItemClickLitener mOnItemClickLitener;

    public SimpleAdapter(Context context, RecyclerView rec, ArrayList<String> arr){
        mContext = context;
        recyclerView = rec;
        mDataSet = arr;
        mSelectedDataIndexSet = new ArrayList<>();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(mContext).inflate(R.layout.grid_item_text,parent,false);
        MyViewHolder holder = new MyViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position){
        holder.tv.setText(mDataSet.get(position));
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
        return mDataSet.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView tv;
        public ImageView checkImage;
        public MyViewHolder(View v){
            super(v);
            tv = (TextView)v.findViewById(R.id.tv_num);
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
                MyViewHolder holder = (MyViewHolder)recyclerView.findViewHolderForAdapterPosition(position);
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
        holder.tv.setActivated(true);
    }

    private void updateUnselectItem(MyViewHolder holder){
        holder.checkImage.setVisibility(View.GONE);
        holder.tv.setActivated(false);
    }

}
