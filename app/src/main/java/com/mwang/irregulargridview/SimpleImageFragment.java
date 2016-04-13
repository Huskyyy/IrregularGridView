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

public class SimpleImageFragment extends Fragment implements ActionMode.Callback{

    private RecyclerView mGridView;
    private ArrayList<String> mImageDataPath;
    private SimpleImageAdapter mImageAdapter;
    private ActionMode mActionMode;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initImageData();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_base, container, false);

        mGridView = (RecyclerView)view.findViewById(R.id.irregular_gridview);

        IrregularLayoutManager layoutManager = new IrregularLayoutManager(getContext(), 4);
        layoutManager.setRandomSize(false);
        mGridView.setLayoutManager(layoutManager);

        mImageAdapter = new SimpleImageAdapter(getContext(), mGridView, mImageDataPath);
        mImageAdapter.setSizePerSpan(getWindowWidth() / 4 + 1);
        mImageAdapter.setOnItemClickLitener(new SimpleImageAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(SimpleImageAdapter.MyViewHolder holder, int position) {
                if (mActionMode != null) {
                    mImageAdapter.reverseSelect(holder, position);
                }
            }

            @Override
            public boolean onItemLongClick(SimpleImageAdapter.MyViewHolder holder, int position) {
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.appbar_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_select:
                mActionMode = ((AppCompatActivity)getContext())
                        .startSupportActionMode(SimpleImageFragment.this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // Called when the action mode is created; startActionMode() was called
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
        return true;
    }

    // Called each time the action mode is shown. Always called after onCreateActionMode, but
    // may be called multiple times if the mode is invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false; // Return false if nothing is done
    }

    // Called when the user selects a contextual menu item
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                deleteSelectedItems();
                mode.finish(); // Action picked, so close the CAB
                return true;
            default:
                return false;
        }
    }

    // Called when the user exits the action mode
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        resetSelectedItems();
        mActionMode = null;
    }

    public void initImageData(){
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

    public void deleteSelectedItems() {
        ArrayList<Integer> list = mImageAdapter.deleteSelectedItems();
        for (int i = list.size() - 1; i >= 0; i--){
            int index = list.get(i);
            mImageDataPath.remove(index);
            mImageAdapter.notifyItemRemoved(index);
        }
    }

    public void resetSelectedItems(){
        mImageAdapter.resetSelectedItems();
    }

    public int getWindowWidth(){
        WindowManager wm = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        wm.getDefaultDisplay().getSize(point);
        return point.x;
    }
}

