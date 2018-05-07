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