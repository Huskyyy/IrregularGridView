package com.mwang.irregulargridview;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class SimpleImageFragment extends BaseFragment{

    private RecyclerView mGridView;
    private ArrayList<String> mImageDataPath;
    private SimpleImageAdapter mImageAdapter;

    public SimpleImageFragment() {
        // Required empty public constructor
    }

    public static SimpleImageFragment newInstance() {
        SimpleImageFragment fragment = new SimpleImageFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_base, container, false);

        mGridView = (RecyclerView)view.findViewById(R.id.irregular_gridview);

        IrregularLayoutManager layoutManager = new IrregularLayoutManager(getContext(), 4);
        mGridView.setLayoutManager(layoutManager);

        mImageAdapter = new SimpleImageAdapter(getContext(), mGridView, mImageDataPath);
        mImageAdapter.setOnItemClickLitener(new BaseAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(BaseAdapter.VH holder, int position) {
                if (mActionMode != null) {
                    mImageAdapter.reverseSelect(holder, position);
                }
            }
            @Override
            public boolean onItemLongClick(BaseAdapter.VH holder, int position) {
                if (mActionMode == null) {
                    mActionMode = ((AppCompatActivity) getContext())
                            .startSupportActionMode(SimpleImageFragment.this);
                }
                mImageAdapter.selectItem(holder, position);
                return true;
            }
        });
        mGridView.setAdapter(mImageAdapter);

        mGridView.setItemAnimator(new DynamicItemAnimator());

        return view;
    }

    @Override
    public void initData(){
        mImageDataPath = new ArrayList<>();

        String picFolder = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).getAbsolutePath();
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = MediaStore.Images.Media.query(getContext().getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj,
                "", null, MediaStore.Images.Media.DATE_TAKEN + " DESC");
        if(cursor != null){
            int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            while(cursor.moveToNext()){
                String path = cursor.getString(dataColumn);
                if(path != null && path.startsWith(picFolder)){
                    mImageDataPath.add(path);
                }
            }
        }
        cursor.close();
    }

    @Override
    public void deleteSelectedItems() {
        ArrayList<Integer> list = mImageAdapter.getSelectedItems();
        for (int i = list.size() - 1; i >= 0; i--){
            int index = list.get(i);
            mImageDataPath.remove(index);
            mImageAdapter.notifyItemRemoved(index);
        }
    }

    @Override
    public void resetSelectedItems(){
        mImageAdapter.resetSelectedItems();
    }

}

