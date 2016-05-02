package com.mwang.irregulargridview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Random;

public class SimpleFragment extends BaseFragment {

    private RecyclerView mGridView;
    private ArrayList<String> mStringData;
    private ArrayList<Integer> widthNums;
    private ArrayList<Integer> heightNums;
    private SimpleAdapter mAdapter;

    public SimpleFragment() {
        // Required empty public constructor
    }

    public static SimpleFragment newInstance() {
        SimpleFragment fragment = new SimpleFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_base, container, false);

        mGridView = (RecyclerView)view.findViewById(R.id.irregular_gridview);
        //mGridView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        IrregularLayoutManager layoutManager = new IrregularLayoutManager(getContext(), 4);
        mGridView.setLayoutManager(layoutManager);

        mAdapter = new SimpleAdapter(getContext(), mGridView, mStringData, widthNums, heightNums);
        mAdapter.setOnItemClickLitener(new BaseAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(BaseAdapter.VH holder, int position) {
                if (mActionMode != null) {
                    mAdapter.reverseSelect(holder, position);
                }
            }

            @Override
            public boolean onItemLongClick(BaseAdapter.VH holder, int position) {
                if (mActionMode == null) {
                    mActionMode = ((AppCompatActivity) getContext()).startSupportActionMode(SimpleFragment.this);
                }
                mAdapter.selectItem(holder, position);
                return true;
            }
        });
        mGridView.setAdapter(mAdapter);
        DynamicItemAnimator animator = new DynamicItemAnimator();
        mGridView.setItemAnimator(new DefaultItemAnimator());

        return view;
    }

    @Override
    public void initData(){
        mStringData = new ArrayList<>();
        for(int i = 0; i < 100; i++){
            mStringData.add("" + i);
        }
        Random r = new Random();
        widthNums = new ArrayList<>();
        heightNums = new ArrayList<>();
        for(int i = 0; i < mStringData.size(); i++){
            int widthNum, heightNum;
            int nextInt = r.nextInt(100);
            if (nextInt > 80) {
                widthNum = 2;
                heightNum = 2;
            } else if (nextInt > 60) {
                widthNum = 2;
                heightNum = 1;
            } else if (nextInt > 40) {
                widthNum = 1;
                heightNum = 2;
            } else {
                widthNum = 1;
                heightNum = 1;
            }
            widthNums.add(widthNum);
            heightNums.add(heightNum);
        }
    }

    @Override
    public void deleteSelectedItems() {
        ArrayList<Integer> list = mAdapter.getSelectedItems();
        for (int i = list.size() - 1; i >= 0; i--){
            int index = list.get(i);
            mStringData.remove(index);
            widthNums.remove(index);
            heightNums.remove(index);
            mAdapter.notifyItemRemoved(index);
        }
    }

    @Override
    public void resetSelectedItems(){
        mAdapter.resetSelectedItems();
    }
}
