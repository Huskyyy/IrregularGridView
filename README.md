# IrregularGridView

An irregular GridView based on the RecyclerView.

It is composed of child views with 4 kinds of sizes. 

##Preview

![image](https://github.com/MWang1991/IrregularGridView/blob/master/gif/Base.gif ) ![image](https://github.com/MWang1991/IrregularGridView/blob/master/gif/Gallery.gif ) 

##Motivation

It is similar to the [DynamicCardLayout](https://github.com/dodola/DynamicCardLayout). However, the DynamicCardLayout does not support recycle of views, which may require too much memory.

##Function

- Recycle of views.
- Remove animation.

##Usage



Firstly, initialize parameters.
		
		// n is the number of spans.
		IrregularLayoutManager layoutManager = new IrregularLayoutManager(getContext(), n);
        recyclerView.setLayoutManager(layoutManager);
		recyclerView.setItemAnimator(new DynamicItemAnimator());
Then, you should set the prefered layout size for each view during onBindViewHolder(). In fact, your only need to set the number of spans occupied by the view in each direction. Here is an example.

		/**
	     * This is an example method to set the size for each child view.
	     * @param view holder.itemView.
	     * @param position the adapter position of the holder.
	     */
		private void setViewParams(View v, int position){
	        IrregularLayoutManager.LayoutParams lp = (IrregularLayoutManager.LayoutParams)v.getLayoutParams();
	        lp.widthNum = yourPreferedWidthSpanNum;
	        lp.heightNum = yourPreferedHeightSpanNum;
	    }

The IrregularLayoutManager will "try" to layout child views with the sizes you set. The child view may be shrunk in the horizontal direction due to space limit.