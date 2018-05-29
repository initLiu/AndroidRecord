# 调用mListView.setAdapter(mAdapter);之后的故事
```java
//android.widget.ListView.java
@Override
protected void layoutChildren() {
    ...
    final int childCount = getChildCount();//子view的个数（和listview的item的个数不一定是相等。childCount限制在一屏可显示的范围内。）此时childCount==0
    ...
    boolean dataChanged = mDataChanged;//Adapter中的data是不是更新，调用adapter的notifyDataSetChanged后mDataChanged为true
    if (dataChanged) {
        handleDataChanged();
    }
    ...
    // Pull all children into the RecycleBin.
    // These views will be reused if possible
    final int firstPosition = mFirstPosition;//第一个可见item的position
    final RecycleBin recycleBin = mRecycler;
    if (dataChanged) {
        for (int i = 0; i < childCount; i++) {
            recycleBin.addScrapView(getChildAt(i), firstPosition+i);
        }
    } else {
        recycleBin.fillActiveViews(childCount, firstPosition);
    }

    // Clear out old views
    detachAllViewsFromParent();//把所有的子view重parent中detach

    switch (mLayoutMode) {
        ...
        default:
            if (childCount == 0) {//一开始childCount为0
                if (!mStackFromBottom) {
                    final int position = lookForSelectablePosition(0, true);
                    setSelectedPositionInt(position);
                    sel = fillFromTop(childrenTop);//1.1
                } else {
                    final int position = lookForSelectablePosition(mItemCount - 1, false);
                    setSelectedPositionInt(position);
                    sel = fillUp(mItemCount - 1, childrenBottom);
                }
            } else {
                if (mSelectedPosition >= 0 && mSelectedPosition < mItemCount) {
                    sel = fillSpecific(mSelectedPosition,
                            oldSel == null ? childrenTop : oldSel.getTop());
                } else if (mFirstPosition < mItemCount) {
                    sel = fillSpecific(mFirstPosition,
                            oldFirst == null ? childrenTop : oldFirst.getTop());
                } else {
                    sel = fillSpecific(0, childrenTop);
                }
            }
        break;
    }

    // Flush any cached views that did not get reused above
    recycleBin.scrapActiveViews();//1.2
}
```

```java
//1.1
//从上往下填充listview
/**
 * Fills the list from top to bottom, starting with mFirstPosition
 *
 * @param nextTop The location where the top of the first item should be
 *        drawn第一个item的位置
 *
 * @return The view that is currently selected
 */
private View fillFromTop(int nextTop) {
    mFirstPosition = Math.min(mFirstPosition, mSelectedPosition);
    mFirstPosition = Math.min(mFirstPosition, mItemCount - 1);
    if (mFirstPosition < 0) {
        mFirstPosition = 0;
    }
    //上面的代码是找到第一个item的position
    return fillDown(mFirstPosition, nextTop);
}

//从指定的位置开始往下填充listview
/**
 * Fills the list from pos down to the end of the list view.
 *
 * @param pos The first position to put in the list
 *
 * @param nextTop The location where the top of the item associated with pos
 *        should be drawn
 *
 * @return The view that is currently selected, if it happens to be in the
 *         range that we draw.
 */
 private View fillDown(int pos, int nextTop) {
    ...
    int end = (mBottom - mTop);//最后一个item的位置（等于listview的底部）
    if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
        end -= mListPadding.bottom;
    }

    //循环调用makeAndAddView方法（pos小于listview底部并且pos小于mItemCount）
    while (nextTop < end && pos < mItemCount) {//mItemCount是adapter的item的个数
        // is this the selected item?
        boolean selected = pos == mSelectedPosition;
        View child = makeAndAddView(pos, nextTop, true, mListPadding.left, selected);

        nextTop = child.getBottom() + mDividerHeight;
        if (selected) {
            selectedView = child;
        }
        pos++;
    }
 }

private View makeAndAddView(int position, int y, boolean flow, int childrenLeft,
            boolean selected) {
    View child;


    if (!mDataChanged) {
        // Try to use an existing view for this position
        child = mRecycler.getActiveView(position);//child为null
        if (child != null) {
            // Found it -- we're using an existing child
            // This just needs to be positioned
            setupChild(child, position, y, flow, childrenLeft, selected, true);

            return child;
        }
    }

    // Make a new view for this position, or convert an unused view if possible
    child = obtainView(position, mIsScrap);

    // This needs to be positioned and measured
    setupChild(child, position, y, flow, childrenLeft, selected, mIsScrap[0]);

    return child;
}

/**
 * Add a view as a child and make sure it is measured (if necessary) and
 * positioned properly.
 * 把child item添加到listview中，对child item进行测量
 */
private void setupChild(View child, int position, int y, boolean flowDown, int childrenLeft,
            boolean selected, boolean recycled) {
    ...
    if ((recycled && !p.forceAdd) || (p.recycledHeaderFooter &&
                p.viewType == AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER)) {
        attachViewToParent(child, flowDown ? -1 : 0, p);//调用这个方法之后child才会真正add到ViewGroup
    } else {
        p.forceAdd = false;
        if (p.viewType == AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
            p.recycledHeaderFooter = true;
        }
        addViewInLayout(child, flowDown ? -1 : 0, p, true);//调用这个方法之后child才会真正add到ViewGroup
    }
    ...

}
```
```java
//1.2

```
listview滑动时
```java
boolean trackMotionScroll(int deltaY, int incrementalDeltaY) {
    final int childCount = getChildCount();
    if (childCount == 0) {
        return true;
    }
    ...
    final boolean down = incrementalDeltaY < 0;
    if (down) {//上滑
        int top = -incrementalDeltaY;
        if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
            top += listPadding.top;
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getBottom() >= top) {//item还在没有从屏幕滑动出去
                break;
            } else {//item已经滑动出去
                count++;
                int position = firstPosition + i;
                if (position >= headerViewsCount && position < footerViewsStart) {
                    // The view will be rebound to new data, clear any
                    // system-managed transient state.
                    child.clearAccessibilityFocus();
                    mRecycler.addScrapView(child, position);//将换出的item加入到scrap数组中
                }
            }
        }
    } else {//下滑
        ....
    }
    ...
    final int absIncrementalDeltaY = Math.abs(incrementalDeltaY);
    if (spaceAbove < absIncrementalDeltaY || spaceBelow < absIncrementalDeltaY) {
        fillGap(down);//将滑入的item添加进来
    }
}
```
