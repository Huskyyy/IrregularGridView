# IrregularGridView

An irregular GridView based on the RecyclerView.

It is composed of child views with 4 kinds of sizes. 

##Preview

![image](https://github.com/MWang1991/IrregularGridView/blob/master/gif/Base.gif ) ![image](https://github.com/MWang1991/IrregularGridView/blob/master/gif/Gallery.gif ) 

##Motivation

It is similar to the [DynamicCardLayout](https://github.com/dodola/DynamicCardLayout). However, the DynamicCardLayout does not support recycle of views, which may require too much memory.

##Support

- Recycle of views.
- Remove animation.

##Usage

- RandomSize Mode.

		// n is the number of spans.
		IrregularLayoutManager layoutManager = new IrregularLayoutManager(getContext(), n);
        recyclerView.setLayoutManager(layoutManager);
		recyclerView.setItemAnimator(new DynamicItemAnimator());
That's all~
Note that in the randomsize mode, you cannot determine the size of each child view. The IrregularLayoutManager will generate sizes for child views randomly.

- CustomSize Mode.

Firstly, initialize parameters.
		
		// n is the number of spans.
		IrregularLayoutManager layoutManager = new IrregularLayoutManager(getContext(), n);
        layoutManager.setRandomSize(false);
        recyclerView.setLayoutManager(layoutManager);
		recyclerView.setItemAnimator(new DynamicItemAnimator());
Then, you should set the prefered layout size for each view during onBindViewHolder(). Here is an example.

		/**
	     * This is an example method to set the size for each child view.
	     * @param path The absolute path of the image.
	     * @param view holder.itemView
	     * @return
	     */
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

Of course you can try any other methods that have the same effect.

The IrregularLayoutManager will "try" to layout child views with the sizes you set. The child view may be shrunk in the horizontal direction due to space limit.