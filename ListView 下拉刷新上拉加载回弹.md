# ListView 上拉下拉
### ListView中已经实现了下拉和上拉以及回弹效果，只不过是在源码中禁用而已
```java
//AbsListView.java
private void initAbsListView() {
        ....
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mOverscrollDistance = configuration.getScaledOverscrollDistance();//overscroll的距离
        .....
    }


    //ViewConfiguration.java

    /**
    * Max distance in dips to overscroll for edge effects
    */
   private static final int OVERSCROLL_DISTANCE = 0;

   /**
     * Minimum velocity to initiate a fling, as measured in dips per second
     */
    private static final int MINIMUM_FLING_VELOCITY = 50;

    /**
         * @deprecated Use {@link android.view.ViewConfiguration#get(android.content.Context)} instead.
         */
        @Deprecated
        public ViewConfiguration() {
            ......
            mMinimumFlingVelocity = MINIMUM_FLING_VELOCITY;
            mOverscrollDistance = OVERSCROLL_DISTANCE;
            .......
        }

    /**
     * @return The maximum distance a View should overscroll by when showing edge effects (in
     * pixels).
     */
    public int getScaledOverscrollDistance() {
        return mOverscrollDistance;
    }

    /**
     * @return Minimum velocity to initiate a fling, as measured in pixels per second.
     */
    public int getScaledMinimumFlingVelocity() {
        return mMinimumFlingVelocity;
    }
```
从上面的代码可以看到mOverscrollDistance就是ViewConfiguration中的OVERSCROLL_DISTANCE=0。
可以给mOverscrollDistance设置一个值如mOverscrollDistance = Integer.MAX_VALUE，如果给ListView设置了overScrollHeader和overScrollFooter的情况下，下拉或者上拉就会出现overscroll了。
```html
<com.lzp.test8.widget.ListView
        android:id="@+id/listview"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:overScrollHeader="@drawable/aio_combo_9"
        android:overScrollFooter="@drawable/aio_combo_9"
        android:entries="@array/items"/>
```


### 从源码的角度分析一下
```java
//AbsListView.java
@Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (action & MotionEvent.ACTION_MASK) {
           case MotionEvent.ACTION_DOWN: {
               ....
               if (touchMode != TOUCH_MODE_FLING && motionPosition >= 0) {
                    // User clicked on an actual view (and was not stopping a fling).
                    // Remember where the motion event started
                    v = getChildAt(motionPosition - mFirstPosition);
                    mMotionViewOriginalTop = v.getTop();
                    mMotionX = x;
                    mMotionY = y;
                    mMotionPosition = motionPosition;
                    mTouchMode = TOUCH_MODE_DOWN;
                    clearScrollingCache();
                }
                ....
           }
    }
```
滑动屏幕首先触发了onInterceptTouchEvent，设置mTouchMode = TOUCH_MODE_DOWN;，ACTION_DOWN事件最后在AbsListView的OnTouchEvent方法中被处理。之后所有事件直接到OnTouchEvent中处理。
```java
@Override
    public boolean onTouchEvent(MotionEvent ev) {
        case MotionEvent.ACTION_DOWN: {
            ....
        }
        case MotionEvent.ACTION_MOVE: {
                int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex == -1) {
                    pointerIndex = 0;
                    mActivePointerId = ev.getPointerId(pointerIndex);
                }
                final int y = (int) ev.getY(pointerIndex);
                switch (mTouchMode) {
                    case TOUCH_MODE_DOWN:
                    case TOUCH_MODE_TAP:
                    case TOUCH_MODE_DONE_WAITING:
                        // Check if we have moved far enough that it looks more like a
                        // scroll than a tap
                        startScrollIfNeeded(y);
                        break;
                    case TOUCH_MODE_SCROLL:
                    case TOUCH_MODE_OVERSCROLL:
                        scrollIfNeeded(y);
                        break;
                }
                break;
            }
    }
```
touch事件变为ACTION_MOVE后传递到onTouchEvent，此时mTouchMode = TOUCH_MODE_DOWN，执行
startScrollIfNeeded()方法。
```java
 private boolean startScrollIfNeeded(int y) {
     final int deltaY = y - mMotionY;
        final int distance = Math.abs(deltaY);
        final boolean overscroll = mScrollY != 0;
        if (overscroll || distance > mTouchSlop) {
            createScrollingCache();
            if (overscroll) {
                mTouchMode = TOUCH_MODE_OVERSCROLL;
                mMotionCorrection = 0;
            } else {
                mTouchMode = TOUCH_MODE_SCROLL;
                mMotionCorrection = deltaY > 0 ? mTouchSlop : -mTouchSlop;
            }
            .....
            scrollIfNeeded(y);
        }
 }
```
由于是首次响应ACTION_MOVE，所以mScrooY=0，所以执行到mTouchMode = TOUCH_MODE_SCROLL。
接着执行scrollIfNeeded()方法。
```java
private void scrollIfNeeded(int y) {
    final int rawDeltaY = y - mMotionY;
        final int deltaY = rawDeltaY - mMotionCorrection;
        int incrementalDeltaY = mLastY != Integer.MIN_VALUE ? y - mLastY : deltaY;

        if (mTouchMode == TOUCH_MODE_SCROLL) {
            .....
            boolean atEdge = false;
            if (incrementalDeltaY != 0) {
                atEdge = trackMotionScroll(deltaY, incrementalDeltaY);
            }
            if (motionView != null) {
                    // Check if the top of the motion view is where it is
                    // supposed to be
                    final int motionViewRealTop = motionView.getTop();
                    if (atEdge) {
                        int overscroll = -incrementalDeltaY -
                                (motionViewRealTop - motionViewPrevTop);
                        overScrollBy(0, overscroll, 0, mScrollY, 0, 0,
                                0, mOverscrollDistance, true);
                    }
                    mMotionY = y;
                    invalidate();
            }
        }
        .....
}
```
此时mTouchMode为TOUCH_MODE_SCROLL，进入if分支，接着代码判断是否在上边缘或下边缘，如果
在边缘，则执行overScrollBy(0, overscroll, 0, mScrollY, 0, 0,0, mOverscrollDistance, true);
倒数第二个参数mOverscrollDistance在AbsListView初始化的时候被设置为0，这是原生ListView不能下拉上拉及回弹的关键。接着进入overScrollBy()方法看一下
```java
protected boolean overScrollBy(int deltaX, int deltaY,
           int scrollX, int scrollY,
           int scrollRangeX, int scrollRangeY,
           int maxOverScrollX, int maxOverScrollY,
           boolean isTouchEvent) {
               ....
               int newScrollY = scrollY + deltaY;
               ...
               final int left = -maxOverScrollX;
        final int right = maxOverScrollX + scrollRangeX;
        final int top = -maxOverScrollY;
        final int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;

}
```
在原生ListView中maxOverScrollY=0。如果是下拉，newScrollY<0，最后会进入186行的else if分子，newScrollY被赋值为top，因为top=0所以newScrollY = 0。最后调用onOverScrolled方法将新的newScrollY回传给AbsListView
```java
@Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (mScrollY != scrollY) {
            onScrollChanged(mScrollX, scrollY, mScrollX, mScrollY);
            mScrollY = scrollY;
            invalidateParentIfNeeded();

            awakenScrollBars();
        }
    }
```
执行到203行mScrollY=0。之后调用invalidateParentIfNeeded()方法更新ListView，由于mScrollY为0
所以不会有下拉上拉以及回弹显示。

# ListView回弹
```java
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        ......
        case MotionEvent.ACTION_UP: {
            switch (mTouchMode) {
                case TOUCH_MODE_OVERSCROLL:
                        if (mFlingRunnable == null) {
                            mFlingRunnable = new FlingRunnable();
                        }
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        final int initialVelocity = (int) velocityTracker.getYVelocity
                                (mActivePointerId);

                        reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                       if (Math.abs(initialVelocity) > mMinimumVelocity) {
                           mFlingRunnable.startOverfling(-initialVelocity);
                       } else {
                           mFlingRunnable.startSpringback();
                       }
                    break;
            }
        }
    }
```
当手指抬起时，触发ACTION_UP事件。此时mTouchMode为TOUCH_MODE_OVERSCROLL。这是代码中获取当前的Y轴方向的速度。mMinimumVelocity在AbsListView
初始化的时候进行了赋值，他的值等于ViewConfiguration.MINIMUM_FLING_VELOCITY。
这里根据Y轴的速度进行判断，>50时走startOverfling(),<50是走startSpringback()回弹.这里分析一下startSpringback()方法.
```java
void startSpringback() {
      if (mScroller.springBack(0, mScrollY, 0, 0, 0, 0)) {
           mTouchMode = TOUCH_MODE_OVERFLING;
           invalidate();
           post(this);
      } else {
           mTouchMode = TOUCH_MODE_REST;
           reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
      }
}
```
调用OverScroll的springBack()方法，此方法
```java
/**
     * Call this when you want to 'spring back' into a valid coordinate range.
     *
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param minX Minimum valid X value
     * @param maxX Maximum valid X value
     * @param minY Minimum valid Y value
     * @param maxY Minimum valid Y value
     * @return true if a springback was initiated, false if startX and startY were already within the valid range.
     */
    public boolean springBack(int startX, int startY, int minX, int maxX, int minY, int maxY)
    {
//        Log.e("Test","springBack");
        mMode = FLING_MODE;

        // Make sure both methods are called.
        final boolean spingbackX = mScrollerX.springback(startX, minX, maxX);
        final boolean spingbackY = mScrollerY.springback(startY, minY, maxY);
        return spingbackX || spingbackY;
    }
```
其中参数minX,maxX,minY,maxY是回弹之后view在x轴y轴上的最后位置。原生ListView中由于设置为0，所以，回弹的时候直接运动到
ListView的顶部。
