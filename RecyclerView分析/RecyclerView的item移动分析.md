我们知道RecyclerView支持局部更新item，如下面的代码移动item的位置
```java
Collections.swap(mDatas,fromPostion,toPostion);
ViewHolderAdapter.notifyItemMoved(fromPostion,toPostion);
```
那么RecylcerView是怎么实现的呢？
```java
//RecyclerView$Adapter.java
public final void notifyItemMoved(int fromPosition, int toPosition) {
    mObservable.notifyItemMoved(fromPosition, toPosition);
}

//RecyclerView$AdapterDataObservable.java
public void notifyItemMoved(int fromPosition, int toPosition) {
    for (int i = mObservers.size() - 1; i >= 0; i--) {
        mObservers.get(i).onItemRangeMoved(fromPosition, toPosition, 1);
    }
}

//RecyclerView$RecyclerViewDataObserver.java
@Override
public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
    assertNotInLayoutOrScroll(null);
    if (mAdapterHelper.onItemRangeMoved(fromPosition, toPosition, itemCount)) {//Step1：添加move操作
        triggerUpdateProcessor();//Step2：处理move操作
    }
}
```
## Step1：添加move操作
```java
//AdapterHelper.java
boolean onItemRangeMoved(int from, int to, int itemCount) {
    if (from == to) {
        return false; // no-op
    }
    if (itemCount != 1) {
        throw new IllegalArgumentException("Moving more than 1 item is not supported yet");
    }
    //创建一个UpdateOp对象保存操作的cmd以及位置（from，to）
    //然后将UpdateOp对象添加的mPendingUpdates集合中
    mPendingUpdates.add(obtainUpdateOp(UpdateOp.MOVE, from, to, null));
    mExistingUpdateTypes |= UpdateOp.MOVE;
    return mPendingUpdates.size() == 1;
}

@Override
public UpdateOp obtainUpdateOp(int cmd, int positionStart, int itemCount, Object payload) {
    UpdateOp op = mUpdateOpPool.acquire();
    if (op == null) {
        op = new UpdateOp(cmd, positionStart, itemCount, payload);
    } else {
        op.cmd = cmd;
        op.positionStart = positionStart;
        op.itemCount = itemCount;
        op.payload = payload;
    }
    return op;
}

static class UpdateOp {
    static final int ADD = 1;

    static final int REMOVE = 1 << 1;

    static final int UPDATE = 1 << 2;

    static final int MOVE = 1 << 3;

    static final int POOL_SIZE = 30;

    int cmd;

    int positionStart;

    Object payload;

    // holds the target position if this is a MOVE
    int itemCount;

    UpdateOp(int cmd, int positionStart, int itemCount, Object payload) {
        this.cmd = cmd;
        this.positionStart = positionStart;
        this.itemCount = itemCount;
        this.payload = payload;
    }
}
```
## Step2：处理move操作
```java
//RecyclerView.java
void triggerUpdateProcessor() {
    if (POST_UPDATES_ON_ANIMATION && mHasFixedSize && mIsAttached) {
        ViewCompat.postOnAnimation(RecyclerView.this, mUpdateChildViewsRunnable);
    } else {
        mAdapterUpdateDuringMeasure = true;
        //调用RecyclerView的requestLayout()方法重新布局item
        //由于调用了requestLayout()方法，所以RecyclerView的onMeasure和onLayout会被调用
        requestLayout();
    }
}
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
        mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);

        final boolean measureSpecModeIsExactly =
                widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY;
        if (measureSpecModeIsExactly || mAdapter == null) {
            return;
        }

        if (mState.mLayoutStep == State.STEP_START) {
            dispatchLayoutStep1();
        }
        ...
    }
}

private void dispatchLayoutStep1() {
    ....
    processAdapterUpdatesAndSetAnimationFlags();
    ....
}

private void processAdapterUpdatesAndSetAnimationFlags() {
    ...
    if (predictiveItemAnimationsEnabled()) {
        mAdapterHelper.preProcess();
    } else {
        mAdapterHelper.consumeUpdatesInOnePass();
    }
    ...
}

//AdapterHelper.java
void preProcess() {
    mOpReorderer.reorderOps(mPendingUpdates);
    final int count = mPendingUpdates.size();
    for (int i = 0; i < count; i++) {
        UpdateOp op = mPendingUpdates.get(i);
        switch (op.cmd) {
            case UpdateOp.ADD:
                applyAdd(op);
                break;
            case UpdateOp.REMOVE:
                applyRemove(op);
                break;
            case UpdateOp.UPDATE:
                applyUpdate(op);
                break;
            case UpdateOp.MOVE:
                applyMove(op);
                break;
        }
        if (mOnItemProcessedCallback != null) {
            mOnItemProcessedCallback.run();
        }
    }
    mPendingUpdates.clear();
}

private void applyMove(UpdateOp op) {
    // MOVE ops are pre-processed so at this point, we know that item is still in the adapter.
    // otherwise, it would be converted into a REMOVE operation
    postponeAndUpdateViewHolders(op);
}

private void postponeAndUpdateViewHolders(UpdateOp op) {
    if (DEBUG) {
        Log.d(TAG, "postponing " + op);
    }
    mPostponedList.add(op);
    switch (op.cmd) {
        case UpdateOp.ADD:
            mCallback.offsetPositionsForAdd(op.positionStart, op.itemCount);
            break;
        case UpdateOp.MOVE:
            mCallback.offsetPositionsForMove(op.positionStart, op.itemCount);
            break;
        case UpdateOp.REMOVE:
            mCallback.offsetPositionsForRemovingLaidOutOrNewView(op.positionStart,
                    op.itemCount);
            break;
        case UpdateOp.UPDATE:
            mCallback.markViewHoldersUpdated(op.positionStart, op.itemCount, op.payload);
            break;
        default:
            throw new IllegalArgumentException("Unknown update op type for " + op);
    }
}

//RecyclerView.java
@Override
public void offsetPositionsForMove(int from, int to) {
    offsetPositionRecordsForMove(from, to);
    // should we create mItemsMoved ?
    mItemsAddedOrRemoved = true;
}
```
<font color='red' size=8>item移动的重点</font>
```java
//RecyclerView.java
void offsetPositionRecordsForMove(int from, int to) {
    //遍历RecyclerView中所有的child
    //1.判断view的位置是否在from和to之间
    //2.如果view的位置==from，将viewHolder.mPosition的位置移动到to
    //3.其余view的ViewHolder.mPosition加1或减1
    //4.调用requestLayout，重新布局
    final int childCount = mChildHelper.getUnfilteredChildCount();
    final int start, end, inBetweenOffset;
    if (from < to) {
        start = from;
        end = to;
        inBetweenOffset = -1;
    } else {
        start = to;
        end = from;
        inBetweenOffset = 1;
    }

    for (int i = 0; i < childCount; i++) {
        final ViewHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
        if (holder == null || holder.mPosition < start || holder.mPosition > end) {
            continue;
        }
        if (DEBUG) {
            Log.d(TAG, "offsetPositionRecordsForMove attached child " + i + " holder "
                    + holder);
        }
        if (holder.mPosition == from) {
            holder.offsetPosition(to - from, false);
        } else {
            holder.offsetPosition(inBetweenOffset, false);
        }

        mState.mStructureChanged = true;
    }
    mRecycler.offsetPositionRecordsForMove(from, to);
    requestLayout();
}
```
根据之前对RecyclerView的分析可以知道，RecyclerView在复用ViewHolder的时候，会判断当前要添加的View的位置是否等于缓存的view的ViewHolder.mPosition，这样的话在布局过程中取到的就是move后的viewholder。举个例子：
把position=1的view1移动到position=3的view3的位置。
1.首先遍历RecyclerView的child，从判断child.ViewHolder.mPositon是否在[1,3]之间
2.对[1,3]之间的view的ViewHolder.mPosition进行更改
    view1.ViewHolder.mPosition=3
    view2.ViewHolder.mPosition=1
    view3.ViewHolder.mPosition=2
3.然后重新布局
4.布局过程中，从缓存查找ViewHolder
    viewHolder=get from cache
    if(pos==viewHolder.mPosition)
    最后找到的结果就是
    pos1=view2.ViewHolder.mPosition=1
    pos2=view3.ViewHolder.mPosition=2
    po3=view1.ViewHolder.mPosition=3
    然后把这些view添加到RecyclerView中，这样就实现了view的移
