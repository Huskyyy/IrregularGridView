package com.mwang.irregulargridview;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class SimpleFragment extends Fragment implements ActionMode.Callback{

    private RecyclerView mGridView;
    private ArrayList<String> mStringData;
    private SimpleAdapter mAdapter;
    private ActionMode mActionMode;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initStringData();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_base, container, false);

        mGridView = (RecyclerView)view.findViewById(R.id.irregular_gridview);
        mGridView.setLayoutManager(new IrregularLayoutManager(getContext(), 4));

        mAdapter = new SimpleAdapter(getContext(), mGridView, mStringData);
        mAdapter.setOnItemClickLitener(new SimpleAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(SimpleAdapter.MyViewHolder holder, int position) {
                if(mActionMode != null){
                    mAdapter.reverseSelect(holder, position);
                }
            }
            @Override
            public boolean onItemLongClick(SimpleAdapter.MyViewHolder holder, int position) {
                if(mActionMode == null){
                    mActionMode = ((AppCompatActivity)getContext()).startSupportActionMode(SimpleFragment.this);
                }
                mAdapter.selectItem(holder, position);
                return true;
            }
        });
        mGridView.setAdapter(mAdapter);

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
                mActionMode = ((AppCompatActivity)getContext()).startSupportActionMode(SimpleFragment.this);
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

    public void initStringData(){
        mStringData = new ArrayList<>();
        for(int i = 0; i < 100; i++){
            mStringData.add("" + i);
        }
    }

    public void deleteSelectedItems() {
        ArrayList<Integer> list = mAdapter.deleteSelectedItems();
        for (int i = list.size() - 1; i >= 0; i--){
            int index = list.get(i);
            mStringData.remove(index);
            mAdapter.notifyItemRemoved(index);
        }
    }

    public void resetSelectedItems(){
        mAdapter.resetSelectedItems();
    }
}
