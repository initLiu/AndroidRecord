# RecyclerView.setAdapter--->requestLayout
```java
这里简单的介绍一下State类
```java
//用来保存RecyclerView一些有用状态的类
public static class State {
    static final int STEP_START = 1;
    static final int STEP_LAYOUT = 1 << 1;
    static final int STEP_ANIMATIONS = 1 << 2;

    private int mTargetPosition = RecyclerView.NO_POSITION;
    @LayoutState
    int mLayoutStep = STEP_START;

    /**
    * Number of items adapter has.
    */
    int mItemCount = 0;

}

public class RecyclerView extends ViewGroup implements ScrollingView, NestedScrollingChild2 {
    ...
    LayoutManager mLayout;
    ...
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (mLayout == null) {
            defaultOnMeasure(widthSpec, heightSpec);
            return;
        }
        if (mLayout.isAutoMeasureEnabled()) {
            final int widthMode = MeasureSpec.getMode(widthSpec);
            final int heightMode = MeasureSpec.getMode(heightSpec);

            /**
             * This specific call should be considered deprecated and replaced with
             * {@link #defaultOnMeasure(int, int)}. It can't actually be replaced as it could
             * break existing third party code but all documentation directs developers to not
             * override {@link LayoutManager#onMeasure(int, int)} when
             * {@link LayoutManager#isAutoMeasureEnabled()} returns true.
             */
             //Step1：调用layoutmanager的onMeasure方法，这里以LinearLayoutManager为例
            mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);

            final boolean measureSpecModeIsExactly =
                    widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY;
            if (measureSpecModeIsExactly || mAdapter == null) {//width和height如果都是MATCH_PARENT/具体数值，或者没有设置adapter，退出measure流程。
                return;
            }

            //Step2：dispatchLayoutStep1
            if (mState.mLayoutStep == State.STEP_START) {//初始时mState.mLayoutStep=State.STEP_START
                dispatchLayoutStep1();
            }
            // set dimensions in 2nd step. Pre-layout should happen with old dimensions for
            // consistency
            //Step3：保存测量好的width和height
            mLayout.setMeasureSpecs(widthSpec, heightSpec);
            mState.mIsMeasuring = true;
            //Step4：dispatchLayoutStep2
            dispatchLayoutStep2();

            // now we can get the width and height from the children.
            //Step5：调用RecyclerView.setMeasuredDimension方法，设置测量的宽高
            mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);

            // if RecyclerView has non-exact width and height and if there is at least one child
            // which also has non-exact width & height, we have to re-measure.
            if (mLayout.shouldMeasureTwice()) {
                mLayout.setMeasureSpecs(
                        MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
                mState.mIsMeasuring = true;
                dispatchLayoutStep2();
                // now we can get the width and height from the children.
                mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
            }
        }
        ...
    }
}
```
## Step1：调用layoutmanager的onMeasure方法
```java
//RecyclerView.LayoutManager
public void onMeasure(Recycler recycler, State state, int widthSpec, int heightSpec) {
    mRecyclerView.defaultOnMeasure(widthSpec, heightSpec);
}

void defaultOnMeasure(int widthSpec, int heightSpec) {
    // calling LayoutManager here is not pretty but that API is already public and it is better
    // than creating another method since this is internal.
    final int width = LayoutManager.chooseSize(widthSpec,
            getPaddingLeft() + getPaddingRight(),
            ViewCompat.getMinimumWidth(this));
    final int height = LayoutManager.chooseSize(heightSpec,
            getPaddingTop() + getPaddingBottom(),
            ViewCompat.getMinimumHeight(this));

    setMeasuredDimension(width, height);
}
```
## Step2：dispatchLayoutStep1
```java
private void dispatchLayoutStep1() {
    //从这里可以看到，之后再layoutstep为STEP_START才会执行dispatchLayoutStep1逻辑
    mState.assertLayoutStep(State.STEP_START);
    ...
    mState.mIsMeasuring = false;
    ...
    mState.mTrackOldChangeHolders = mState.mRunSimpleAnimations && mItemsChanged;
    mItemsAddedOrRemoved = mItemsChanged = false;
    mState.mInPreLayout = mState.mRunPredictiveAnimations;
    mState.mItemCount = mAdapter.getItemCount();
    //初始状态下mRunSimpleAnimations为false
    if (mState.mRunSimpleAnimations) {
        ...
    }
    //初始状态下mRunPredictiveAnimations为false
    if (mState.mRunPredictiveAnimations) {
        ...
    }else {
        clearOldPositions();
    }
    ...
    mState.mLayoutStep = State.STEP_LAYOUT;
}
```
## Step4：dispatchLayoutStep2
```java
private void dispatchLayoutStep2() {
    startInterceptRequestLayout();
    onEnterLayoutOrScroll();
    //设置state的layoutstep
    mState.assertLayoutStep(State.STEP_LAYOUT | State.STEP_ANIMATIONS);
    mAdapterHelper.consumeUpdatesInOnePass();
    //设置itemcount
    mState.mItemCount = mAdapter.getItemCount();
    mState.mDeletedInvisibleItemCountSincePreviousLayout = 0;

    // Step 2: Run layout
    mState.mInPreLayout = false;
    //调用layoutmanager的onLayoutChild方法，添加item，这里稍后详细展开---Step6：调用Layoutmanager的onLayoutChildren方法
    mLayout.onLayoutChildren(mRecycler, mState);

    mState.mStructureChanged = false;
    mPendingSavedState = null;

    // onLayoutChildren may have caused client code to disable item animations; re-check
    mState.mRunSimpleAnimations = mState.mRunSimpleAnimations && mItemAnimator != null;
    mState.mLayoutStep = State.STEP_ANIMATIONS;//代码执行到这里layoutstep=STEP_ANIMATIONS，这里很重要，在layout流程中会用到
    onExitLayoutOrScroll();
    stopInterceptRequestLayout(false);
}
```
onMeasure方法执行完之后，接下来就会调用到RecyclerView的onLayout方法。
```java
@Override
protected void onLayout(boolean changed, int l, int t, int r, int b) {
    TraceCompat.beginSection(TRACE_ON_LAYOUT_TAG);
    dispatchLayout();
    TraceCompat.endSection();
    mFirstLayoutComplete = true;
}

void dispatchLayout() {
    if (mAdapter == null) {
        Log.e(TAG, "No adapter attached; skipping layout");
        // leave the state in START
        return;
    }
    if (mLayout == null) {
        Log.e(TAG, "No layout manager attached; skipping layout");
        // leave the state in START
        return;
    }
    mState.mIsMeasuring = false;
    //在step4中，可以看到mLayoutStep被设置为 State.STEP_ANIMATIONS，
    //在onMeasure过程中设置了width和height，
    //所以if和else if分支都不会走
    if (mState.mLayoutStep == State.STEP_START) {
        dispatchLayoutStep1();
        mLayout.setExactMeasureSpecsFrom(this);
        dispatchLayoutStep2();
    } else if (mAdapterHelper.hasUpdates() || mLayout.getWidth() != getWidth()
            || mLayout.getHeight() != getHeight()) {
        // First 2 steps are done in onMeasure but looks like we have to run again due to
        // changed size.
        mLayout.setExactMeasureSpecsFrom(this);
        dispatchLayoutStep2();
    } else {
        // always make sure we sync them (to ensure mode is exact)
        mLayout.setExactMeasureSpecsFrom(this);
    }
    //Step5：dispatchLayoutStep3
    dispatchLayoutStep3();
}
```
## Step5：dispatchLayoutStep3
```java
private void dispatchLayoutStep3() {
    //从这里可以看到，之后再layoutstep为STEP_ANIMATIONS才会执行dispatchLayoutStep3逻辑
    mState.assertLayoutStep(State.STEP_ANIMATIONS);
    startInterceptRequestLayout();
    onEnterLayoutOrScroll();
    mState.mLayoutStep = State.STEP_START;//layoutstep被设置为STEP_START
    if (mState.mRunSimpleAnimations) {
        ...
    }
    mState.mPreviousLayoutItemCount = mState.mItemCount;
    mDataSetHasChangedAfterLayout = false;
    mDispatchItemsChangedEvent = false;
    mState.mRunSimpleAnimations = false;

    mState.mRunPredictiveAnimations = false;
    mLayout.mRequestedSimpleAnimations = false;
    if (mRecycler.mChangedScrap != null) {
        mRecycler.mChangedScrap.clear();
    }
    ...
    mLayout.onLayoutCompleted(mState);
}
```
## Step6：调用Layoutmanager的onLayoutChildren方法
```java
public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
    ...
    ensureLayoutState();
    mLayoutState.mRecycle = false;
    ...
    //Step7：detachAndScrapAttachedViews
    detachAndScrapAttachedViews(recycler);
    ...
    //Step8: 填充RecyclerView
    //以从上往下填充为例
    // fill towards end
    //初始化填充是需要一些坐标高度等信息
    updateLayoutStateToFillEnd(mAnchorInfo);
    mLayoutState.mExtra = extraForEnd;
    //开始填充
    fill(recycler, mLayoutState, state, false);
    endOffset = mLayoutState.mOffset;
    final int lastElement = mLayoutState.mCurrentPosition;
    if (mLayoutState.mAvailable > 0) {
        extraForStart += mLayoutState.mAvailable;
    }
    // fill towards start
    updateLayoutStateToFillStart(mAnchorInfo);
    mLayoutState.mExtra = extraForStart;
    mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
    fill(recycler, mLayoutState, state, false);
    startOffset = mLayoutState.mOffset;

    if (mLayoutState.mAvailable > 0) {
        extraForEnd = mLayoutState.mAvailable;
        // start could not consume all it should. add more items towards end
        updateLayoutStateToFillEnd(lastElement, endOffset);
        mLayoutState.mExtra = extraForEnd;
        fill(recycler, mLayoutState, state, false);
        endOffset = mLayoutState.mOffset;
    }
    ...
}

void ensureLayoutState() {
    if (mLayoutState == null) {
        mLayoutState = createLayoutState();
    }
}

LayoutState createLayoutState() {
    return new LayoutState();
}

//LayoutManager的一个帮助类，用来临时存储一些状态
static class LayoutState {

    static final String TAG = "LLM#LayoutState";

    static final int LAYOUT_START = -1;

    static final int LAYOUT_END = 1;

    static final int INVALID_LAYOUT = Integer.MIN_VALUE;

    static final int ITEM_DIRECTION_HEAD = -1;

    static final int ITEM_DIRECTION_TAIL = 1;

    static final int SCROLLING_OFFSET_NaN = Integer.MIN_VALUE;

    /**
        * We may not want to recycle children in some cases (e.g. layout)
        */
    boolean mRecycle = true;

    /**
        * Current position on the adapter to get the next item.
        */
    int mCurrentPosition;
}
```
## Step7：detachAndScrapAttachedViews
```java
public void detachAndScrapAttachedViews(Recycler recycler) {
    //在setAdapter之前，child个数为0，所以首次调用setAdapter时，是不会进行下面的回收逻辑的。
    //这里我们假设child个数不为0，看下是如何回收view的
    final int childCount = getChildCount();
    for (int i = childCount - 1; i >= 0; i--) {
        final View v = getChildAt(i);
        //Step9：回收view
        scrapOrRecycleView(recycler, i, v);
    }
}
```
## Step9：回收view
```java
private void scrapOrRecycleView(Recycler recycler, int index, View view) {
    final ViewHolder viewHolder = getChildViewHolderInt(view);//获取view对应的ViewHolder
    if (viewHolder.shouldIgnore()) {//viewholder如果设置了FLAG_IGNORE标志位，不回收此view
        if (DEBUG) {
            Log.d(TAG, "ignoring view " + viewHolder);
        }
        return;
    }
    //viewholder设置了FLAG_INVALID标志位并且没有设置FLAG_REMOVED标志位并且adapter没有设置stableIds
    if (viewHolder.isInvalid() && !viewHolder.isRemoved()
            && !mRecyclerView.mAdapter.hasStableIds()) {
        removeViewAt(index);//从RecyclerView中删除此item
        //Step10：添加ViewHolder到mCachedViews或RecycledViewPool缓存中
        recycler.recycleViewHolderInternal(viewHolder);//添加进mCachedViews或RecycledViewPool缓存中
    } else {
        detachViewAt(index);//从RecyclerView中detach掉此item
        recycler.scrapView(view);//Step11：添加ViewHolder到mAttachedScrap缓存中
        mRecyclerView.mViewInfoStore.onViewDetached(viewHolder);
    }
}
```
## Step10：添加ViewHolder到mCachedViews或RecycledViewPool缓存中
```java
void recycleViewHolderInternal(ViewHolder holder) {
    ...
    if (forceRecycle || holder.isRecyclable()) {
        if (mViewCacheMax > 0
                && !holder.hasAnyOfTheFlags(ViewHolder.FLAG_INVALID
                | ViewHolder.FLAG_REMOVED
                | ViewHolder.FLAG_UPDATE
                | ViewHolder.FLAG_ADAPTER_POSITION_UNKNOWN)) {
            ...
            //把viewholder添加到mCachedViews缓存中
            mCachedViews.add(targetCacheIndex, holder);
            cached = true;
            ...
        }
        if (!cached) {
            //把viewholder添加到RecycledViewPool缓存中
            addViewHolderToRecycledViewPool(holder, true);
            recycled = true;
        }
    }
    ...
}
```
<font color='red' size=5>从上面可以看到RecyclerView缓存的是viewholder</font>

## Step11：添加ViewHolder到mAttachedScrap缓存中
```java
void scrapView(View view) {
    final ViewHolder holder = getChildViewHolderInt(view);
    if (holder.hasAnyOfTheFlags(ViewHolder.FLAG_REMOVED | ViewHolder.FLAG_INVALID)
            || !holder.isUpdated() || canReuseUpdatedViewHolder(holder)) {
        if (holder.isInvalid() && !holder.isRemoved() && !mAdapter.hasStableIds()) {
            throw new IllegalArgumentException("Called scrap view with an invalid view."
                    + " Invalid views cannot be reused from scrap, they should rebound from"
                    + " recycler pool." + exceptionLabel());
        }
        holder.setScrapContainer(this, false);//设置ViewHolder的mScrapContainer
        mAttachedScrap.add(holder);
    } else {
        if (mChangedScrap == null) {
            mChangedScrap = new ArrayList<ViewHolder>();
        }
        holder.setScrapContainer(this, true);设置ViewHolder的mScrapContainer
        mChangedScrap.add(holder);
    }
}
```
***
## Step8：填充RecyclerView
### 1.初始化填充是需要用到的高度位置等信息
```java
private void updateLayoutStateToFillEnd(AnchorInfo anchorInfo) {
    updateLayoutStateToFillEnd(anchorInfo.mPosition, anchorInfo.mCoordinate);
}

private void updateLayoutStateToFillEnd(int itemPosition, int offset) {
    mLayoutState.mAvailable = mOrientationHelper.getEndAfterPadding() - offset;//RecyclerView中可填充的高度
    mLayoutState.mItemDirection = mShouldReverseLayout ? LayoutState.ITEM_DIRECTION_HEAD :
            LayoutState.ITEM_DIRECTION_TAIL;
    mLayoutState.mCurrentPosition = itemPosition;//第一个要填充的item的position
    mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;//填充的方向，向下填充LAYOUT_END=1，向上填充LAYOUT_START=-1
    mLayoutState.mOffset = offset;
    mLayoutState.mScrollingOffset = LayoutState.SCROLLING_OFFSET_NaN;//!!!注意：fill填充是会用到
}
```
### 2.开始填充
```java
fill(recycler, mLayoutState, state, false);
int fill(RecyclerView.Recycler recycler, LayoutState layoutState,
            RecyclerView.State state, boolean stopOnFocusable) {
    final int start = layoutState.mAvailable;
    if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
        // TODO ugly bug fix. should not happen
        if (layoutState.mAvailable < 0) {
            layoutState.mScrollingOffset += layoutState.mAvailable;
        }
        recycleByLayoutState(recycler, layoutState);
    }
    int remainingSpace = layoutState.mAvailable + layoutState.mExtra;//RecyclerView中可用的填充范围
    LayoutChunkResult layoutChunkResult = mLayoutChunkResult;
    while ((layoutState.mInfinite || remainingSpace > 0) && layoutState.hasMore(state)) {
        layoutChunkResult.resetInternal();
        if (VERBOSE_TRACING) {
            TraceCompat.beginSection("LLM LayoutChunk");
        }
        layoutChunk(recycler, state, layoutState, layoutChunkResult);//3.从缓存去ViewHolder或者创建新的ViewHolder
        if (VERBOSE_TRACING) {
            TraceCompat.endSection();
        }
        if (layoutChunkResult.mFinished) {
            break;
        }
        layoutState.mOffset += layoutChunkResult.mConsumed * layoutState.mLayoutDirection;//记录已经添加到RecyclerView中的view的高度
        /**
            * Consume the available space if:
            * * layoutChunk did not request to be ignored
            * * OR we are laying out scrap children
            * * OR we are not doing pre-layout
            */
        if (!layoutChunkResult.mIgnoreConsumed || mLayoutState.mScrapList != null
                || !state.isPreLayout()) {
            layoutState.mAvailable -= layoutChunkResult.mConsumed;//计算剩余的可填充的高度
            // we keep a separate remaining space because mAvailable is important for recycling
            remainingSpace -= layoutChunkResult.mConsumed;//计算剩余的可填充的高度
        }

        if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
            layoutState.mScrollingOffset += layoutChunkResult.mConsumed;
            if (layoutState.mAvailable < 0) {
                layoutState.mScrollingOffset += layoutState.mAvailable;
            }
            recycleByLayoutState(recycler, layoutState);
        }
        if (stopOnFocusable && layoutChunkResult.mFocusable) {
            break;
        }
    }
    if (DEBUG) {
        validateChildOrder();
    }
    return start - layoutState.mAvailable;
}

//LinearLayoutManager.LayoutState
boolean hasMore(RecyclerView.State state) {
    return mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount();
}
```
### 3.从缓存去ViewHolder或者创建新的ViewHolder
```java
void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state,
            LayoutState layoutState, LayoutChunkResult result) {
    View view = layoutState.next(recycler);
    f (view == null) {
        if (DEBUG && layoutState.mScrapList == null) {
            throw new RuntimeException("received null view when unexpected");
        }
        // if we are laying out views in scrap, this may return null which means there is
        // no more items to layout.
        result.mFinished = true;
        return;
    }
    //item添加到RecyclerView中
    LayoutParams params = (LayoutParams) view.getLayoutParams();
    if (layoutState.mScrapList == null) {
        if (mShouldReverseLayout == (layoutState.mLayoutDirection
                == LayoutState.LAYOUT_START)) {
            addView(view);
        } else {
            addView(view, 0);
        }
    } else {
        if (mShouldReverseLayout == (layoutState.mLayoutDirection
                == LayoutState.LAYOUT_START)) {
            addDisappearingView(view);
        } else {
            addDisappearingView(view, 0);
        }
    }

    measureChildWithMargins(view, 0, 0);//测量子view的宽高
    result.mConsumed = mOrientationHelper.getDecoratedMeasurement(view);//获得子view 的高度(height+topmargin+bottommargin)
    ...
    layoutDecoratedWithMargins(view, left, top, right, bottom);//layout子view
    ...
    // Consume the available space if the view is not removed OR changed
    if (params.isItemRemoved() || params.isItemChanged()) {
        result.mIgnoreConsumed = true;
    }
    result.mFocusable = view.hasFocusable();
}
//LinearLayoutManager.LayoutState
View next(RecyclerView.Recycler recycler) {
    if (mScrapList != null) {
        return nextViewFromScrapList();
    }
    final View view = recycler.getViewForPosition(mCurrentPosition);
    mCurrentPosition += mItemDirection;//移动下一个要添加的子view的mCurrentPosition（向下填充mCurrentPosition+1，向上填充mCurrentPosition-1）
    return view;
}

//RecyclerView.Recycler
public View getViewForPosition(int position) {
    return getViewForPosition(position, false);//这里传递参数dryRun为false
}

View getViewForPosition(int position, boolean dryRun) {
    return tryGetViewHolderForPositionByDeadline(position, dryRun, FOREVER_NS).itemView;
}
//重点:复用ViewHolder！！！！
ViewHolder tryGetViewHolderForPositionByDeadline(int position,
                boolean dryRun, long deadlineNs) {
    if (position < 0 || position >= mState.getItemCount()) {
        throw new IndexOutOfBoundsException("Invalid item position " + position
                + "(" + position + "). Item count:" + mState.getItemCount()
                + exceptionLabel());
    }
    boolean fromScrapOrHiddenOrCache = false;
    ViewHolder holder = null;
    // 0) If there is a changed scrap, try to find from there
    if (mState.isPreLayout()) {
        holder = getChangedScrapViewForPosition(position);
        fromScrapOrHiddenOrCache = holder != null;
    }
    if (holder == null) {
        //4.从缓存中获取ViewHolder
        holder = getScrapOrHiddenOrCachedHolderForPosition(position, dryRun);
        if (holder != null) {
            //判断ViewHolder是否可用，可用的条件是：
            //1.holder.mPosition < 0 || holder.mPosition >= mAdapter.getItemCount()
            //2.holder.getItemViewType()==mAdapter.getItemViewType(holder.mPosition)
            //3.mAdapter.hasStableIds()?holder.getItemId() == mAdapter.getItemId(holder.mPosition):true
            if (!validateViewHolderForOffsetPosition(holder)) {
                // recycle holder (and unscrap if relevant) since it can't be used
                if (!dryRun) {
                    // we would like to recycle this but need to make sure it is not used by
                    // animation logic etc.
                    holder.addFlags(ViewHolder.FLAG_INVALID);//给ViewHolder设置FLAG_INVALID标志位
                    if (holder.isScrap()) {//当首次setAdapter时，由于child个数为0，在Step7中没有回收ViewHolder，因此没有设置mScrapContainer
                        removeDetachedView(holder.itemView, false);
                        holder.unScrap();
                    } else if (holder.wasReturnedFromScrap()) {//如果设置了FLAG_RETURNED_FROM_SCRAP标志位，清除FLAG_RETURNED_FROM_SCRAP标志位
                        holder.clearReturnedFromScrapFlag();
                    }
                    recycleViewHolderInternal(holder);//把ViewHolder添加到mCachedViews或RecycledViewPool缓存中
                }
                holder = null;
            } else {
                fromScrapOrHiddenOrCache = true;
            }
        }
    }
    if (holder == null) {
        final int offsetPosition = mAdapterHelper.findPositionOffset(position);
        if (offsetPosition < 0 || offsetPosition >= mAdapter.getItemCount()) {
            throw new IndexOutOfBoundsException("Inconsistency detected. Invalid item "
                    + "position " + position + "(offset:" + offsetPosition + ")."
                    + "state:" + mState.getItemCount() + exceptionLabel());
        }

        final int type = mAdapter.getItemViewType(offsetPosition);
        // 2) Find from scrap/cache via stable ids, if exists
        if (mAdapter.hasStableIds()) {
            holder = getScrapOrCachedViewForId(mAdapter.getItemId(offsetPosition),
                    type, dryRun);
            if (holder != null) {
                // update position
                holder.mPosition = offsetPosition;
                fromScrapOrHiddenOrCache = true;
            }
        }
        if (holder == null && mViewCacheExtension != null) {
            // We are NOT sending the offsetPosition because LayoutManager does not
            // know it.
            final View view = mViewCacheExtension
                    .getViewForPositionAndType(this, position, type);
            if (view != null) {
                holder = getChildViewHolder(view);
                if (holder == null) {
                    throw new IllegalArgumentException("getViewForPositionAndType returned"
                            + " a view which does not have a ViewHolder"
                            + exceptionLabel());
                } else if (holder.shouldIgnore()) {
                    throw new IllegalArgumentException("getViewForPositionAndType returned"
                            + " a view that is ignored. You must call stopIgnoring before"
                            + " returning this view." + exceptionLabel());
                }
            }
        }
        if (holder == null) { // fallback to pool
            if (DEBUG) {
                Log.d(TAG, "tryGetViewHolderForPositionByDeadline("
                        + position + ") fetching from shared pool");
            }
            holder = getRecycledViewPool().getRecycledView(type);
            if (holder != null) {
                holder.resetInternal();
                if (FORCE_INVALIDATE_DISPLAY_LIST) {
                    invalidateDisplayListInt(holder);
                }
            }
        }
        //从mAttachedScrap、mCachedViews、mViewCacheExtension、RecycledViewPool没有找到匹配的ViewHolder的情况下，调用Adapter.createViewHolder创建新的ViewHolder
        if (holder == null) {
            long start = getNanoTime();
            if (deadlineNs != FOREVER_NS
                    && !mRecyclerPool.willCreateInTime(type, start, deadlineNs)) {
                // abort - we have a deadline we can't meet
                return null;
            }
            holder = mAdapter.createViewHolder(RecyclerView.this, type);
            if (ALLOW_THREAD_GAP_WORK) {
                // only bother finding nested RV if prefetching
                RecyclerView innerView = findNestedRecyclerView(holder.itemView);
                if (innerView != null) {
                    holder.mNestedRecyclerView = new WeakReference<>(innerView);
                }
            }

            long end = getNanoTime();
            mRecyclerPool.factorInCreateTime(type, end - start);
            if (DEBUG) {
                Log.d(TAG, "tryGetViewHolderForPositionByDeadline created new ViewHolder");
            }
        }
    }
    ...
    final ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
    final LayoutParams rvLayoutParams;
    if (lp == null) {
        rvLayoutParams = (LayoutParams) generateDefaultLayoutParams();
        holder.itemView.setLayoutParams(rvLayoutParams);
    } else if (!checkLayoutParams(lp)) {
        rvLayoutParams = (LayoutParams) generateLayoutParams(lp);
        holder.itemView.setLayoutParams(rvLayoutParams);
    } else {
        rvLayoutParams = (LayoutParams) lp;
    }
    rvLayoutParams.mViewHolder = holder;
    rvLayoutParams.mPendingInvalidate = fromScrapOrHiddenOrCache && bound;
    return holder;
}
```
### 4.从缓存中获取ViewHolder
```java
ViewHolder getScrapOrHiddenOrCachedHolderForPosition(int position, boolean dryRun) {
    final int scrapCount = mAttachedScrap.size();

    // Try first for an exact, non-invalid match from scrap.
    for (int i = 0; i < scrapCount; i++) {
        final ViewHolder holder = mAttachedScrap.get(i);
        //1.ViewHolder首次从mAttachedScrap中复用，
        //2.position和ViewHolder保存的一致
        //public final int getLayoutPosition() {
        //     return mPreLayoutPosition == NO_POSITION ? mPosition : mPreLayoutPosition;
        //}
        //ViewHolder没有FLAG_INVALID标志位
        if (!holder.wasReturnedFromScrap() && holder.getLayoutPosition() == position
                && !holder.isInvalid() && (mState.mInPreLayout || !holder.isRemoved())) {
            holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP);//添加FLAG_RETURNED_FROM_SCRAP标志位，表示此ViewHolder从缓存中复用过
            return holder;
        }
    }

    if (!dryRun) {
        View view = mChildHelper.findHiddenNonRemovedView(position);
        if (view != null) {
            // This View is good to be used. We just need to unhide, detach and move to the
            // scrap list.
            final ViewHolder vh = getChildViewHolderInt(view);
            mChildHelper.unhide(view);
            int layoutIndex = mChildHelper.indexOfChild(view);
            if (layoutIndex == RecyclerView.NO_POSITION) {
                throw new IllegalStateException("layout index should not be -1 after "
                        + "unhiding a view:" + vh + exceptionLabel());
            }
            mChildHelper.detachViewFromParent(layoutIndex);
            scrapView(view);
            vh.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP
                    | ViewHolder.FLAG_BOUNCED_FROM_HIDDEN_LIST);
            return vh;
        }
    }

    // Search in our first-level recycled view cache.
    //从mCachedViews缓存中查找ViewHolder，匹配条件是
    //1.ViewHolder没有设置FLAG_INVALID标志位
    //2.并且position一致
    final int cacheSize = mCachedViews.size();
    for (int i = 0; i < cacheSize; i++) {
        final ViewHolder holder = mCachedViews.get(i);
        // invalid view holders may be in cache if adapter has stable ids as they can be
        // retrieved via getScrapOrCachedViewForId
        if (!holder.isInvalid() && holder.getLayoutPosition() == position) {
            if (!dryRun) {
                mCachedViews.remove(i);//匹配到后，从缓存中删除
            }
            if (DEBUG) {
                Log.d(TAG, "getScrapOrHiddenOrCachedHolderForPosition(" + position
                        + ") found match in cache: " + holder);
            }
            return holder;
        }
    }
    return null;
}
```
