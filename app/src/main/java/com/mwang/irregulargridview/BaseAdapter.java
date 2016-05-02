package com.mwang.irregulargridview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;

public abstract class BaseAdapter extends RecyclerView.Adapter<BaseAdapter.VH>{

    protected Context mContext;
    protected RecyclerView recyclerView;
    private ArrayList<Integer> mSelectedDataIndexSet;
    private OnItemClickLitener mOnItemClickLitener;

    public BaseAdapter(Context context, RecyclerView rec){
        mContext = context;
        recyclerView = rec;
        mSelectedDataIndexSet = new ArrayList<>();
    }

    @Override
    public void onBindViewHolder(final VH holder, int position){

        if(mSelectedDataIndexSet != null && mSelectedDataIndexSet.contains(new Integer(position)) ){
            updateSelectedItem(holder);
        }else{
            updateUnselectedItem(holder);
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

    public static class VH extends RecyclerView.ViewHolder {
        public VH(View v){
            super(v);
        }
    }

    public interface OnItemClickLitener {
        void onItemClick(VH holder, int position);
        boolean onItemLongClick(VH holder, int position);
    }

    public void setOnItemClickLitener(OnItemClickLitener mOnItemClickLitener)
    {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    public ArrayList<Integer> getSelectedItems() {
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
                VH holder = (VH)recyclerView.findViewHolderForAdapterPosition(position);
                if(holder != null) {
                    updateUnselectedItem(holder);
                }else{
                    notifyItemChanged(position);
                }
            }
            mSelectedDataIndexSet.clear();
        }
    }

    public void reverseSelect(VH holder, int position){
        if(mSelectedDataIndexSet.contains(position)){
            mSelectedDataIndexSet.remove(new Integer(position));
            updateUnselectedItem(holder);
        }else if(!mSelectedDataIndexSet.contains(position)){
            mSelectedDataIndexSet.add(position);
            updateSelectedItem(holder);
        }
    }

    public void selectItem(VH holder, int position){
        if(!mSelectedDataIndexSet.contains(position)){
            mSelectedDataIndexSet.add(position);
            updateSelectedItem(holder);
        }
    }

    protected abstract void updateSelectedItem(VH holder);

    protected abstract void updateUnselectedItem(VH holder);

}
