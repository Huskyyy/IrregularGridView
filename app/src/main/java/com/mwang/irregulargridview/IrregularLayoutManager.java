package com.mwang.irregulargridview;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;

/**
 * An IrregularLayoutManager which composed of items with 4 kinds of sizes.
 * 1x1, 1x2, 2x1 and 2x2.
 */
public class IrregularLayoutManager extends RecyclerView.LayoutManager {

    /* Default spanCount. */
    private static final int DEFAULT_SPAN_COUNT = 4;
    /* Current spanCount. */
    private int mSpanCount = DEFAULT_SPAN_COUNT;

    /* Store the bottom of each span. */
    private int[] spanBottom;
    /* The minimum  of the spanBottom. */
    private int spanBottomMin;
    /* The maximum of the spanBottom. */
    private int spanBottomMax;
    /* The top of each span. */
    private int[] spanTop;
    /* The minimum of the spanTop. */
    private int spanTopMin;
    /* The maximum of the spanTop. */
    private int spanTopMax;

    /* Store the first span index where the first and the second span are both empty. */
    private int firstTwoEmptyBottomSpanIndex;
    /* The index of the first empty span. */
    private int firstOneEmptyBottomSpanIndex;

    /* The top border of the area to be filled. */
    private int topBorder;
    /* The bottom border. */
    private int bottomBorder;

    /**
     * Store the left and right borders for each span according to the span count.
     * This is the same as the mCachedBorders in the GridLayoutManager.
      */
    private int[] spanWidthBorders;
    /* Size for the smallest span. */
    private int sizePerSpan;
    /* The current position we need to lay out. */
    private int mCurrentPosition;
    /* The first and the last position of attached items. */
    private int firstAttachedItemPosition;
    private int lastAttachedItemPosition;

    /**
     * Store the layout widthNum and heightNum for each item.
     * The layout width is about widthNum * sizePerSpan.
     * The layout height is heightNum * sizePerSpan.
     */
    private SparseIntArray itemLayoutWidthCache;
    private SparseIntArray itemLayoutHeightCache;
    /* The first span index the item occupied. */
    private SparseIntArray itemOccupiedStartSpan;

    /* Determine whether the item is generate with random size for the first time. */
    private boolean isRandomSize;

    /* The scroll offset. */
    private int scrollOffset;

    /* The cache for the position of items removed. */
    private HashSet<Integer> itemRemovedPositionCache;
    /**
     * Store the position of current attached and not removed items for the "real" layout.
     * This is calculated during the pre-layout.
     */
    private HashSet<Integer> itemNeededPositionCache;

    /* The first item which is removed with notifyItemRemoved(). */
    private int firstChangedPosition;
    /* The number of removed items except for the items out of the bottom border. */
    private int removedTopAndBoundPositionCount;
    /**
     * If it is true, we need to update some parameters,
     * i.e., firstChangedPosition, removedTopAndBoundPositionCount, itemRemovedPositionCache.
     */
    private boolean isBeforePreLayout;

    /* A disappearing view cache with descending order. */
    private TreeMap<Integer, DisappearingView> disappearingViewCache;

    /* Determine whether onLayoutChildren() is triggered with notifyDataSetChanged(). */
    private boolean isNotifyDataSetChanged;

    /* The Rect for the items to be laid out. */
    final Rect mDecorInsets = new Rect();

    /**
     * Default number of the spans is DEFAULT_SPAN_COUNT.
     * @param context
     */
    public IrregularLayoutManager(Context context){
        this(context, DEFAULT_SPAN_COUNT);
    }

    /**
     * You can set the number of spans with this constructor.
     * @param context
     * @param spanCount The number of spans.
     */
    public IrregularLayoutManager(Context context, int spanCount){
        super();
        setSpanCount(spanCount);
        isRandomSize = true;
    }

    /**
     * set the number of span, it should be at least 2.
     * @param spanCount The number of spans.
     */
    private void setSpanCount(int spanCount){
        if(spanCount == mSpanCount)
                return;
        if(spanCount < 2)
                throw new IllegalArgumentException("Span count should be at least 2. Provided "
                                                        + spanCount);
        mSpanCount = spanCount;
    }

    /**
     * If you want to generate a gridView with random size, set it as true.
     * Otherwise, the IrregularLayoutManager will generate item according the size of the view.
     * The default value is true.
     * @param flag
     */
    public void setRandomSize(boolean flag){
        isRandomSize = flag;
    }

    /**
     * If you want to customize the animation, it should return true.
     * @return
     */
    @Override
    public boolean supportsPredictiveItemAnimations() {
        return true;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();
    }

    /**
     * This method is triggered with notifyDataSetChanged().
     * We need the label the isNotifyDataSetChanged before onLayoutChildren.
     * @param recyclerView
     */
    @Override
    public void onItemsChanged(RecyclerView recyclerView){
        isNotifyDataSetChanged = true;
    }

    /**
     * Triggered with notifyItemRemoved().
     * This method will be triggered before the pre-layout for invisible items (out of bounds)
     * and after the pre-layout for visible items.
     * If there are items removed out of the top border, we update the firstChangedPosition
     * and removedTopAndBoundPositionCount.
     * For each removed item, we store the position with itemRemovedPositionCache.
     * @param recyclerView
     * @param positionStart The start position of removed items.
     * @param itemCount The number of removed items from the start position.
     */
    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        if(isBeforePreLayout){
            if(firstChangedPosition > positionStart || firstChangedPosition == -1)
                firstChangedPosition = positionStart;
            if(firstChangedPosition < firstAttachedItemPosition)
                removedTopAndBoundPositionCount += itemCount;
            for(int i = 0; i < itemCount; i++) {
                itemRemovedPositionCache.add(positionStart + i);
            }
        }
    }

    /**
     * Called when it is initial layout, or the data set is changed.
     * If supportsPredictiveItemAnimations() returns true, it will be called twice,
     * i.e., the pre-layout and the real layout.
     * @param recycler
     * @param state
     */
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        // Nothing to be laid out, just clear attached views and return.
        if(state.getItemCount() == 0){
            detachAndScrapAttachedViews(recycler);
            return;
        }
        // For the pre-layout, we need to layout current attached views and appearing views.
        if(state.isPreLayout()){
            // If nothing is attached, just return.
            if(getChildCount() == 0)
                return;
            // For the current attached views, find the views removed and update
            // itemRemovedPositionCache, removedTopAndBoundPositionCount and firstChangedPosition.
            final int childCount = getChildCount();
            for(int i = 0; i < childCount; i++){
                View child = getChildAt(i);
                RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
                if(lp.isItemRemoved()){
                    itemRemovedPositionCache.add(firstAttachedItemPosition + i);
                    removedTopAndBoundPositionCount++;
                    if(firstChangedPosition == -1 ||
                            firstAttachedItemPosition + i < firstChangedPosition){
                        firstChangedPosition = firstAttachedItemPosition + i;
                    }
                }else{
                    // For views not removed, calculate the position for the real layout
                    // and store the position with itemNeededPositionCache.
                    itemNeededPositionCache.add(
                            firstAttachedItemPosition + i - removedTopAndBoundPositionCount);
                }
            }
            // If removedTopAndBoundPositionCount = 0, items changes out of the bottom border,
            // So we have nothing to do during the pre-layout.
            // Otherwise we need to lay out current attached views and appearing views.
            if(removedTopAndBoundPositionCount != 0){
                layoutPotentialAppearingViews(recycler, state);
            }
            // Reset isBeforePreLayout after the pre-layout ends.
            isBeforePreLayout = false;
            return;
        }

        // The real layout.
        // First or empty layout, initialize layout parameters and fill.
        if(getChildCount() == 0){
            initializeLayoutParameters();
            fillGrid(recycler, state, true);
            return;
        }
        // If it is triggered with notifyDataSetChanged(),
        // we just clear attached views and layout from the beginning.
        if(isNotifyDataSetChanged){
            detachAndScrapAttachedViews(recycler);
            initializeLayoutParameters();
            fillGrid(recycler, state, true);
            isNotifyDataSetChanged = false;
        }

        // Adapter data set changes.
        if(firstChangedPosition == -1){ // No item is removed
            // reset parameters.
            mCurrentPosition = firstAttachedItemPosition;
            lastAttachedItemPosition = firstAttachedItemPosition;
            topBorder = getPaddingTop();
            bottomBorder = getHeight() - getPaddingBottom();
            spanBottom = Arrays.copyOf(spanTop, mSpanCount);
            updateSpanBottomParameters();
            // Fill the area.
            detachAndScrapAttachedViews(recycler);
            fillGrid(recycler, state, true);

            // Reset isBeforePreLayout.
            isBeforePreLayout = true;
//            firstChangedPosition = -1;
//            removedTopAndBoundPositionCount = 0;
//            itemRemovedPositionCache.clear();
//            itemNeededPositionCache.clear();
            return;
        }
        // There are removed items.
        // Clear the cache from the firstChangedPosition
        for(int i = firstChangedPosition; i < state.getItemCount(); i++){
            if(itemLayoutWidthCache.get(i, 0) != 0){
                itemLayoutWidthCache.delete(i);
                itemLayoutHeightCache.delete(i);
                itemOccupiedStartSpan.delete(i);
            }
        }
        detachAndScrapAttachedViews(recycler);

        // There are removed items out of the upper bound.
        if(firstChangedPosition < firstAttachedItemPosition) {
            // Calculate the spanTop begin with the firstChangedPosition
            // and update layout parameters.
            topBorder = getPaddingTop() - scrollOffset;
            Arrays.fill(spanTop, topBorder);
            for (int i = 0; i < firstChangedPosition; i++) {
                for (int j = 0; j < itemLayoutWidthCache.get(i); j++) {
                    int spanIndex = itemOccupiedStartSpan.get(i) + j;
                    spanTop[spanIndex] += itemLayoutHeightCache.get(i) * sizePerSpan;
                }
            }
            updateSpanTopParameters();
            bottomBorder = getHeight() - getPaddingBottom();
            spanBottom = Arrays.copyOf(spanTop, mSpanCount);
            updateSpanBottomParameters();
            mCurrentPosition = firstChangedPosition;
            // Fill from the spanTop until bottomBorder.
            // Note that we just lay out visible views.
            // The firstAttachedItemPosition may change,
            // set it as -1 and update it during the layout
            firstAttachedItemPosition = -1;
            lastAttachedItemPosition = -1;
            fillGrid(recycler, state, true, true);
            // After fill, we need to update layout parameters
            topBorder = getPaddingTop();
            updateSpanTopParameters();
            // If it cannot fill until the bottomBorder, call  scrollBy() to fill.
            if(spanBottomMax < bottomBorder){
                scrollBy(spanBottomMax - bottomBorder, recycler, state);
            }
            // Finally, we layout disappearing views.
            layoutDisappearingViews(recycler, state);
        }else{ // There are no removed items out of the upper bound.
            // Just set layout parameters and fill the visible area.
            mCurrentPosition = firstAttachedItemPosition;
            lastAttachedItemPosition = firstAttachedItemPosition;
            topBorder = getPaddingTop();
            bottomBorder = getHeight() - getPaddingBottom();
            spanBottom = Arrays.copyOf(spanTop, mSpanCount);
            updateSpanBottomParameters();
            fillGrid(recycler, state, true);
            // The number of items is too small, call scrollBy() to fill.
            if(spanBottomMax - bottomBorder < 0){
                scrollBy(spanBottomMax - bottomBorder, recycler, state);
            }
        }
        // After the real layout, we need to clear some parameters.
        isBeforePreLayout = true;
        firstChangedPosition = -1;
        removedTopAndBoundPositionCount = 0;
        itemRemovedPositionCache.clear();
        itemNeededPositionCache.clear();
        disappearingViewCache.clear();
    }

    /**
     * Return true to indicate that it supports scrolling vertically.
     * @return
     */
    @Override
    public boolean canScrollVertically(){
        return true;
    }

    /**
     * We need to fill some extra space and offset children in this method.
     * @param dy The distance scrolled.
     * @param recycler
     * @param state
     * @return
     */
    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state){
        // Nothing to do when there are no attached items or dy = 0.
        if(getChildCount() == 0 || dy == 0){
            return 0;
        }
        return scrollBy(dy, recycler, state);

    }

    /**
     * The real logic for scroll.
     * @param dy The distance scrolled.
     * @param recycler
     * @param state
     * @return
     */
    private int scrollBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state){

        int delta = 0;
        // When scroll down, layout from left to right, top to bottom
        // Scroll down, update bottomBorder and fill.
        if(dy > 0){
            topBorder = getPaddingTop();
            bottomBorder += dy;
            mCurrentPosition = lastAttachedItemPosition + 1;
            fillGrid(recycler, state, true);
            // Offset child views.
            if(spanBottomMin >= bottomBorder) {
                delta = dy;
                bottomBorder -= dy;
            }else { // There are no more items we need to lay out.
                bottomBorder = getHeight() - getPaddingBottom();
                if(spanBottomMax - bottomBorder >= dy){
                    delta = dy;
                }else{
                    delta = Math.max(0, spanBottomMax - bottomBorder);
                }
            }
            offsetChildrenVertical(-delta);
            // After offset children, we need to update parameters.
            for(int i = 0; i < mSpanCount; i++) {
                spanTop[i] -= delta;
                spanBottom[i] -= delta;
            }
            spanTopMin -= delta;
            spanTopMax -= delta;
            spanBottomMin -= delta;
            spanBottomMax -= delta;
            // Recycle views out of the topBorder
            recycleTopInvisibleViews(recycler);
        }else{ // dy < 0
            // Scroll up, update topBorder and fill.
            topBorder += dy;
            bottomBorder = getHeight() - getPaddingBottom();
            // Happens when we delete too much items,
            // and the item for firstAttachedItemPosition is null.
            if(firstAttachedItemPosition == -1 ||
                    firstAttachedItemPosition >= state.getItemCount()){
                firstAttachedItemPosition = state.getItemCount() - 1;
                lastAttachedItemPosition = firstAttachedItemPosition;
                mCurrentPosition = firstAttachedItemPosition;
            }else{
                mCurrentPosition = firstAttachedItemPosition - 1;
            }
            fillGrid(recycler, state, false);
            // Offset child views.
            if(spanTopMax <= topBorder) {
                delta = dy;
                topBorder -= dy;
            }else { // There are no more items we need to lay out.
                topBorder = getPaddingTop();
                if(spanTopMin - topBorder <= dy){
                    delta = dy;
                }else{
                    delta = -Math.max(0, topBorder - spanTopMin);
                }
            }
            offsetChildrenVertical(-delta);
            // After offset children, we need to update parameters.
            for(int i = 0; i < mSpanCount; i++) {
                spanTop[i] -= delta;
                spanBottom[i] -= delta;
            }
            spanTopMin -= delta;
            spanTopMax -= delta;
            spanBottomMin -= delta;
            spanBottomMax -= delta;
            // Recycle views out of the bottomBorder.
            recycleBottomInvisibleViews(recycler);
        }
        // Update scrollOffset.
        scrollOffset += delta;
        return delta;
    }

    /**
     * Initialize necessary parameters.
     */
    private void initializeLayoutParameters(){

        topBorder = getPaddingTop();
        bottomBorder = getHeight() - getPaddingBottom();
        spanTop = new int[mSpanCount];
        Arrays.fill(spanTop, getPaddingTop());
        spanBottom = new int[mSpanCount];
        Arrays.fill(spanBottom,getPaddingTop());
        spanTopMin = getPaddingTop();
        spanTopMax = getPaddingTop();
        spanBottomMin = getPaddingTop();
        spanBottomMax = getPaddingTop();
        firstOneEmptyBottomSpanIndex = 0;
        firstTwoEmptyBottomSpanIndex = 0;
        spanWidthBorders = new int[mSpanCount + 1];
        calculateSpanWidthBorders(getWidth() - getPaddingLeft() - getPaddingRight());
        mCurrentPosition = 0;
        firstAttachedItemPosition = 0;
        lastAttachedItemPosition = 0;
        itemLayoutWidthCache = new SparseIntArray();
        itemLayoutHeightCache = new SparseIntArray();
        itemOccupiedStartSpan = new SparseIntArray();
        //isRandomSize = true;
        scrollOffset = 0;
        isBeforePreLayout = true;
        itemRemovedPositionCache = new HashSet<>();
        itemNeededPositionCache = new HashSet<>();
        firstChangedPosition = -1;
        removedTopAndBoundPositionCount = 0;
        disappearingViewCache = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer lhs, Integer rhs) {
                return rhs.compareTo(lhs);
            }
        });
        isNotifyDataSetChanged = false;
    }

    /**
     * Update spanBottomMin, spanBottomMax,
     * firstOneEmptyBottomSpanIndex and firstTwoEmptyBottomSpanIndex.
     */
    private void updateSpanBottomParameters(){
        spanBottomMin = spanBottom[0];
        spanBottomMax = spanBottom[0];
        for(int i = 1; i < mSpanCount; i++){
            if(spanBottomMin > spanBottom[i])
                spanBottomMin = spanBottom[i];
            if(spanBottomMax < spanBottom[i])
                spanBottomMax = spanBottom[i];
        }
        for(int i = 0; i < mSpanCount; i++){
            if(spanBottom[i] == spanBottomMin){
                firstOneEmptyBottomSpanIndex = i;
                break;
            }
        }
        firstTwoEmptyBottomSpanIndex = -1;
        for(int i = firstOneEmptyBottomSpanIndex; i < mSpanCount - 1; i++){
            if(spanBottom[i] == spanBottomMin && spanBottom[i + 1] == spanBottomMin){
                firstTwoEmptyBottomSpanIndex = i;
                break;
            }
        }
    }

    /**
     * Update spanTopMin and spanTopMax.
     */
    private void updateSpanTopParameters(){
        spanTopMin = spanTop[0];
        spanTopMax = spanTop[0];
        for(int i = 1; i < mSpanCount; i++){
            if(spanTopMin > spanTop[i])
                spanTopMin = spanTop[i];
            if(spanTopMax < spanTop[i])
                spanTopMax = spanTop[i];
        }
    }

    /**
     * Calculate spanWidthBorders.
     * This is the same as calculateItemBorders(int totalSpace) in the GridLayoutManager.
     * @param totalSpace
     */
    private void calculateSpanWidthBorders(int totalSpace){
        if(spanWidthBorders == null || spanWidthBorders.length != mSpanCount + 1 || spanWidthBorders[spanWidthBorders.length - 1] != totalSpace){
            spanWidthBorders = new int[mSpanCount + 1];
        }
        spanWidthBorders[0] = 0;
        sizePerSpan = totalSpace / mSpanCount;
        int sizePerSpanRemainder = totalSpace % mSpanCount;
        int consumedPixels = 0;
        int additionalSize = 0;
        for (int i = 1; i <= mSpanCount; i++) {
            int itemSize = sizePerSpan;
            additionalSize += sizePerSpanRemainder;
            if (additionalSize > 0 && (mSpanCount - additionalSize) < sizePerSpanRemainder) {
                itemSize += 1;
                additionalSize -= mSpanCount;
            }
            consumedPixels += itemSize;
            spanWidthBorders[i] = consumedPixels;
        }
    }


    /**
     * fill the area between the topBorder and the bottomBorder.
     * @param recycler
     * @param state
     * @param isFillBottom The direction, from the top to the bottom or the reverse.
     */
    private void fillGrid(RecyclerView.Recycler recycler, RecyclerView.State state, boolean isFillBottom) {
        while ( ( (isFillBottom && spanBottomMin <= bottomBorder) || (!isFillBottom && spanTopMax >= topBorder) )
                && mCurrentPosition >=0 && mCurrentPosition < state.getItemCount()) {
            layoutChunk(recycler, state, isFillBottom);
        }
    }

    /**
     * Fill the area for animation.
     * After notifyItemRemoved() is called, this method is called during the real layout.
     * We just lay out views in the visible area.
     * @param recycler
     * @param state
     * @param isFillBottom
     * @param isForAnimation
     */
    private void fillGrid(RecyclerView.Recycler recycler, RecyclerView.State state, boolean isFillBottom, boolean isForAnimation) {
        while ( ( (isFillBottom && spanBottomMin <= bottomBorder) || (!isFillBottom && spanTopMax >= topBorder) )
                && mCurrentPosition >=0 && mCurrentPosition < state.getItemCount()) {
            layoutChunk(recycler, state, isFillBottom, isForAnimation);
        }
    }


    /**
     * Here we lay out current attached views and potential appearing views.
     * @param recycler
     * @param state
     */
    private void layoutPotentialAppearingViews(RecyclerView.Recycler recycler, RecyclerView.State state){

        // Store the layout parameters.
        int firstAttachedItemPositionTemp = firstAttachedItemPosition;
        int lastAttachedItemPositionTemp = lastAttachedItemPosition;
        int[] spanTopTemp = Arrays.copyOf(spanTop, mSpanCount);
        int[] spanBottomTemp = Arrays.copyOf(spanBottom, mSpanCount);

        topBorder = getPaddingTop();
        bottomBorder = getHeight() - getPaddingBottom();
        spanBottom = Arrays.copyOf(spanTop, mSpanCount);
        updateSpanBottomParameters();

        detachAndScrapAttachedViews(recycler);

        // Lay out current attached views.
        mCurrentPosition = firstAttachedItemPosition;
        lastAttachedItemPosition = firstAttachedItemPosition;
        fillGrid(recycler, state, true);

        // Lay out potential appearing views out of the top border.
        // Here we just lay out extra items based on the estimate.
        int num = firstAttachedItemPosition - itemRemovedPositionCache.size() * 6;
        mCurrentPosition = firstAttachedItemPosition - 1;
        while(mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount() && mCurrentPosition >= num){
            layoutChunk(recycler, state, false);
        }

        // Lay out potential appearing views out of the bottom border.
        num = lastAttachedItemPosition + itemRemovedPositionCache.size() * 6;
        mCurrentPosition = lastAttachedItemPosition + 1;
        while(mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount() && mCurrentPosition <= num){
            layoutChunk(recycler, state, true);
        }

        // Restore the layout parameters.
        firstAttachedItemPosition = firstAttachedItemPositionTemp;
        lastAttachedItemPosition = lastAttachedItemPositionTemp;
        spanTop = Arrays.copyOf(spanTopTemp, mSpanCount);
        spanBottom = Arrays.copyOf(spanBottomTemp, mSpanCount);
        updateSpanTopParameters();
        updateSpanBottomParameters();

    }

    // Lay out disappearing views from the last one to the first one
    private void layoutDisappearingViews(RecyclerView.Recycler recycler, RecyclerView.State state){
        Iterator<DisappearingView> iterator = disappearingViewCache.values().iterator();
        while(iterator.hasNext()){
            DisappearingView disView = iterator.next();
            addDisappearingView(disView.view, 0);
            disView.view.measure(disView.widthSpec, disView.heightSpec);
            layoutDecorated(disView.view, disView.left, disView.top, disView.right, disView.bottom);
        }
    }

    /**
     * The layout process for each item.
     * @param recycler
     * @param state
     * @param isFillBottom
     */
    private void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state, boolean isFillBottom){
        layoutChunk(recycler, state, isFillBottom, false);
    }

    /**
     * The layout process for each item.
     * If isForAnimation = true, we just lay out views in the visible area.
     * Otherwise we lay out views between the topBorder and the bottomBorder.
     * @param recycler
     * @param state
     * @param isFillBottom
     */
    private void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state, boolean isFillBottom, boolean isForAnimation){

        Random r = new Random();
        int widthNum = 0;
        int heightNum = 0;
        int nextItemIndex = 0;
        View view;
        DisappearingView disappearingView = null;
        // If disappearingViewCache contains the current view to be laid out,
        // just get it. This happens when too many items are removed,
        // and the fillGird() cannot fill to the bottom. Then scrollBy() is called.
        if(disappearingViewCache.containsKey(mCurrentPosition)){
            disappearingView = disappearingViewCache.get(mCurrentPosition);
            view = disappearingView.view;
        }else { // Otherwise we get view from the recycler.
            view = recycler.getViewForPosition(mCurrentPosition);
        }
        final LayoutParams lp = (LayoutParams) view.getLayoutParams();

        // Calculate the widthNum and the heightNum.
        // If the cache contains the widthNum and heightNum, get them from the cache.
        if(itemLayoutWidthCache.get(mCurrentPosition, 0) != 0){
            widthNum = itemLayoutWidthCache.get(mCurrentPosition);
            heightNum = itemLayoutHeightCache.get(mCurrentPosition);
            nextItemIndex = itemOccupiedStartSpan.get(mCurrentPosition);
        }else{
            // Otherwise, if LayoutParams contains them and isRandomSize = true,
            // get them from the LayoutParams.
            if(lp.widthNum != 0 && isRandomSize){
                widthNum = lp.widthNum;
                heightNum = lp.heightNum;
            }else{
                // Otherwise, calculate the widthNum and the heightNum randomly
                // or according to the size of the child view.
                if(isRandomSize) {
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
                }else{
                    widthNum = Math.min(2, Math.max(1, lp.width / sizePerSpan));
                    heightNum = Math.min(2, Math.max(1, lp.height / sizePerSpan));
                }
                // Store the original widthNum and heightNum in the LayoutParams.
                lp.widthNum = widthNum;
                lp.heightNum = heightNum;
            }
            // If widthNum = 2 and there are no two sequential empty spans, just set widthNum as 1.
            if (isFillBottom && firstTwoEmptyBottomSpanIndex == -1) {
                widthNum = 1;
            }
            // Store the layout widthNum and heightNum (different from the original one).
            itemLayoutWidthCache.put(mCurrentPosition, widthNum);
            itemLayoutHeightCache.put(mCurrentPosition, heightNum);
            // Calculate the index of the first occupied span.
            if(isFillBottom) {
                nextItemIndex = widthNum == 1 ?
                        firstOneEmptyBottomSpanIndex : firstTwoEmptyBottomSpanIndex;
            }
            // Store the index of the first occupied span, which is useful when scrolling up.
            itemOccupiedStartSpan.put(mCurrentPosition, nextItemIndex);
        }

        // Calculate the left, right, top and bottom of the view to be laid out.
        int left = 0, right = 0, top = 0, bottom = 0;
        // We do not need to calculate decorations for views in the disappearingViewCache.
        if(disappearingView == null) {
            calculateItemDecorationsForChild(view, mDecorInsets);
        }
        left = getPaddingLeft() + spanWidthBorders[nextItemIndex] + lp.leftMargin;
        right = getPaddingLeft() + spanWidthBorders[nextItemIndex + widthNum] - lp.rightMargin;
        if(isFillBottom){
            top = getPaddingTop() + spanBottomMin + lp.topMargin;
            bottom = getPaddingTop() + spanBottomMin + sizePerSpan * heightNum - lp.bottomMargin;
        }else{
            bottom = getPaddingTop() + spanTop[nextItemIndex] - lp.bottomMargin;
            top = getPaddingTop() + spanTop[nextItemIndex] - sizePerSpan * heightNum + lp.topMargin;
        }

        // If we lay out the view to fill bottom, add the view to the end.
        if(isFillBottom) {

            if(!isForAnimation){
                addView(view);
            }else{
                // If we layout for animation, we just lay out views in the visible area.
                if(bottom + lp.bottomMargin >= getPaddingTop() || firstAttachedItemPosition != -1){
                    addView(view);
                }
            }
        }else if(!isFillBottom){ // Otherwise it is added to the beginning.
            addView(view, 0);
        }

        // Make measureSpec.
        int widthSpec, heightSpec;
        if(disappearingView == null) {
            widthSpec = View.MeasureSpec.makeMeasureSpec(
                    right - left - mDecorInsets.left - mDecorInsets.right, View.MeasureSpec.EXACTLY);
            heightSpec = View.MeasureSpec.makeMeasureSpec(
                    bottom - top - mDecorInsets.top - mDecorInsets.bottom, View.MeasureSpec.EXACTLY);
        }else{
            // If disappearingViewCache contains the current view,
            // get the widthSpec and the heightSpec from it.
            widthSpec = disappearingView.widthSpec;
            heightSpec = disappearingView.heightSpec;
        }
        // Measure child.
        // If isForAnimation = true, we just measure and lay out views in the visible area.
        if(!isForAnimation ||
                (isForAnimation &&
                        (bottom + lp.bottomMargin >= getPaddingTop() ||
                                firstAttachedItemPosition != -1))){
            view.measure(widthSpec, heightSpec);
            layoutDecorated(view, left, top, right, bottom);
        }
        // If isForAnimation = true, for current disappearing views, we put it into cache.
        if(isForAnimation && bottom + lp.bottomMargin < getPaddingTop() &&
                firstAttachedItemPosition == -1 &&
                itemNeededPositionCache.contains(mCurrentPosition)){
            disappearingViewCache.put(mCurrentPosition,
                    new DisappearingView(view, widthSpec, heightSpec, left, top, right, bottom));
        }
        // For the normal layout,
        // if we lay out a disappearing view, it should be removed from the cache.
        if(!isForAnimation && disappearingView != null){
            disappearingViewCache.remove(mCurrentPosition);
        }

        // update some parameters
        if(isFillBottom){
            for (int i = 0; i < widthNum; i++)
                spanBottom[nextItemIndex + i] += sizePerSpan * heightNum;
            updateSpanBottomParameters();
            if(!isForAnimation){
                lastAttachedItemPosition = mCurrentPosition;
            }else{
                // If isForAnimation = true,
                // we need to update firstAttachedItemPosition and lastAttachedItemPosition.
                if(firstAttachedItemPosition == -1 && bottom + lp.bottomMargin >= getPaddingTop()){
                    firstAttachedItemPosition = mCurrentPosition;
                }else{
                    lastAttachedItemPosition = mCurrentPosition;
                }
            }
            mCurrentPosition++;
        }else{
            for (int i = 0; i < widthNum; i++)
                spanTop[nextItemIndex + i] -= sizePerSpan * heightNum;
            updateSpanTopParameters();
            firstAttachedItemPosition = mCurrentPosition;
            mCurrentPosition--;
        }
        // If isForAnimation = true, we need to update spanTop.
        if(isForAnimation && firstAttachedItemPosition == -1){
            for (int i = 0; i < widthNum; i++)
                spanTop[nextItemIndex + i] += sizePerSpan * heightNum;
        }
    }

    /**
     * Recycle views out of the top border.
     * @param recycler
     */
    private void recycleTopInvisibleViews(RecyclerView.Recycler recycler){
        final int childCount = getChildCount();
        for(int i = 0; i <= childCount; i++){
            View child = getChildAt(i);
            // Recycle views from here.
            if(getDecoratedEnd(child) > topBorder){
                recycleChildren(recycler, 0, i - 1);
                firstAttachedItemPosition += i;
                updateSpanTopParameters();
                return;
            }
            // Update spanTop.
            int heightNum = itemLayoutHeightCache.get(firstAttachedItemPosition + i);
            for(int j = 0; j < itemLayoutWidthCache.get(firstAttachedItemPosition + i); j++){
                int spanIndex = itemOccupiedStartSpan.get(firstAttachedItemPosition + i) + j;
                spanTop[spanIndex] += heightNum * sizePerSpan;
            }
        }
    }

    /**
     * Recycle views out of the bottom border.
     * @param recycler
     */
    private void recycleBottomInvisibleViews(RecyclerView.Recycler recycler){
        final int childCount = getChildCount();
        for(int i = childCount - 1; i >= 0; i--){
            View child = getChildAt(i);
            // Recycle views from here.
            if(getDecoratedStart(child) < bottomBorder){
                recycleChildren(recycler, i + 1, childCount - 1);
                lastAttachedItemPosition -= (childCount - 1 - i);
                updateSpanBottomParameters();
                return;
            }
            // Update spanBottom.
            int position = lastAttachedItemPosition - (childCount - 1 - i);
            int heightNum = itemLayoutHeightCache.get(position);
            for(int j = 0; j < itemLayoutWidthCache.get(position); j++){
                int spanIndex = itemOccupiedStartSpan.get(position) + j;
                spanBottom[spanIndex] -= heightNum * sizePerSpan;
            }
        }
    }

    /**
     * Recycle views from the endIndex to the startIndex.
     * @param startIndex inclusive
     * @param endIndex inclusive
     */
    private void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex){
        if(startIndex > endIndex){
            return;
        }
        for(int i = endIndex; i >= startIndex; i--){
            removeAndRecycleViewAt(i, recycler);
        }
    }

    /**
     * Helper method to get the top of the view including the decoration and the margin.
     * @param view
     * @return
     */
    public int getDecoratedStart(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedTop(view) - params.topMargin;
    }

    /**
     * Helper method to get the bottom of the view including the decoration and the margin.
     * @param view
     * @return
     */
    public int getDecoratedEnd(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedBottom(view) + params.bottomMargin;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }
    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }
    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {

        //Original widthNum.
        public int widthNum;
        //Original heightNum.
        public int heightNum;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }
        public LayoutParams(int width, int height) {
            super(width, height);
        }
        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }
        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }
    }

    public static class DisappearingView{
        View view;
        int left, right, top, bottom;
        int widthSpec, heightSpec;
        DisappearingView(View view, int widthSpec, int heightSpec,
                         int left, int top, int right, int bottom){
            this.view = view;
            this.widthSpec = widthSpec;
            this.heightSpec = heightSpec;
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }
}
