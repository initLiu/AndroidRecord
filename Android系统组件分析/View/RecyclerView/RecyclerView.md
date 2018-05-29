```java
/**
 *用来保存RecyclerView一些有用的信息，如滚动的目标位置、item的个数等
 */
public static class State {
    private int mTargetPosition = RecyclerView.NO_POSITION;
    ArrayMap<ViewHolder, ItemHolderInfo> mPreLayoutHolderMap =
                new ArrayMap<ViewHolder, ItemHolderInfo>();
    ArrayMap<ViewHolder, ItemHolderInfo> mPostLayoutHolderMap =
            new ArrayMap<ViewHolder, ItemHolderInfo>();
    // nullable
    ArrayMap<Long, ViewHolder> mOldChangedHolders = new ArrayMap<Long, ViewHolder>();

    // we use this like a set
    final List<View> mDisappearingViewsInLayoutPass = new ArrayList<View>();

    private SparseArray<Object> mData;

    /**
        * Number of items adapter has.
        */
    int mItemCount = 0;
    ....
}
```
```java
/**
* A Recycler is responsible for managing scrapped or detached item views for reuse.
*
* <p>A "scrapped" view is a view that is still attached to its parent RecyclerView but
* that has been marked for removal or reuse.</p>
*
* <p>Typical use of a Recycler by a {@link LayoutManager} will be to obtain views for
* an adapter's data set representing the data at a given position or item ID.
* If the view to be reused is considered "dirty" the adapter will be asked to rebind it.
* If not, the view can be quickly reused by the LayoutManager with no further work.
* Clean views that have not {@link android.view.View#isLayoutRequested() requested layout}
* may be repositioned by a LayoutManager without remeasurement.</p>
*/
//上面的注释说的已经很清楚了。
//Recycler负责管理和重用废弃的和分离的item views。一般情况下LayoutManager根据item的位置或id从Recycle中获取View。
public final class Recycler {
}
```


```java
@Override
protected void onMeasure(int widthSpec, int heightSpec) {
    ...
    //根据是否设置了adapter，设置state中itemcount的值
    if (mAdapter != null) {
        mState.mItemCount = mAdapter.getItemCount();
    } else {
        mState.mItemCount = 0;
    }
    //根据是否设置了layoutmanager，执行不的measure流程。
    //如果设置了layoutmanager，则有layoutmanager接管measure流程。
    if (mLayout == null) {
        defaultOnMeasure(widthSpec, heightSpec);
    } else {
        mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
    }

    mState.mInPreLayout = false; // clear
}
```
```java
static class LayoutState {
    ...
    /**
    * Number of pixels that we should fill, in the layout direction.
    */
    //RecyclerView中一屏能够显示的高度
    int mAvailable;
    /**
    * Pixel offset where layout should start
    */
    //当前要添加的item的开始位置
    int mOffset;

    /**
    * Used when LayoutState is constructed in a scrolling state.
    * It should be set the amount of scrolling we can make without creating a new view.
    * Settings this is required for efficient view recycling.
    */
    //RecycleView的最后的一个View如果没有完全显示，这个就是没有显示出来的高度
    int mScrollingOffset;
    ...
}
```
