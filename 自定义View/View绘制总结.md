```java
public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow) {
             ViewRootImpl root;
    View panelParentView = null;   
    root = new ViewRootImpl(view.getContext(), display);//1.1 创建ViewRootImpl实例

    view.setLayoutParams(wparams);

    try {
        root.setView(view, wparams, panelParentView);//1.2 将decorview添加到ViewRootImpl
    } catch (RuntimeException e) {
        // BadTokenException or InvalidDisplayException, clean up.
        if (index >= 0) {
            removeViewLocked(index, true);
        }
        throw e;
    }
}
```


```java
//1.1 创建ViewRootImpl实例
public ViewRootImpl(Context context, Display display) {
    ....
    mWidth = -1;
    mHeight = -1;
    ...
    mFirst = true; // true for the first time the view is added
}
```
```java
//1.2 将decorview添加到ViewRootImpl
// base.core.java.android.view.ViewRootImpl
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
    synchronized (this) {
            if (mView == null) {
                mView = view;
                ...
                // Schedule the first layout -before- adding to the window
                // manager, to make sure we do the relayout before receiving
                // any other events from the system.
                requestLayout();
                ....
                view.assignParent(this);
            }
    }
}
```
```java
@Override
public void requestLayout() {
    if (!mHandlingLayoutInLayoutRequest) {
        checkThread();
        mLayoutRequested = true;
        scheduleTraversals();
    }
}

final class TraversalRunnable implements Runnable {
    @Override
    public void run() {
        doTraversal();
    }
}

final TraversalRunnable mTraversalRunnable = new TraversalRunnable();

void scheduleTraversals() {
    if (!mTraversalScheduled) {
        mTraversalScheduled = true;
        mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
        mChoreographer.postCallback(
                Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
        if (!mUnbufferedInputDispatch) {
            scheduleConsumeBatchedInput();
        }
        notifyRendererOfFramePending();
        pokeDrawLockIfNeeded();
    }
}

void doTraversal() {
    if (mTraversalScheduled) {
        mTraversalScheduled = false;

        ....

        performTraversals();
        ...
    }
}
```
```java
private void performTraversals() {
    ......
    if (!mStopped || mReportNextDraw) {
        boolean focusChangedDueToTouchMode = ensureTouchModeLocally(
                (relayoutResult&WindowManagerGlobal.RELAYOUT_RES_IN_TOUCH_MODE) != 0);
        if (focusChangedDueToTouchMode || mWidth != host.getMeasuredWidth()
                || mHeight != host.getMeasuredHeight() || contentInsetsChanged ||
                updatedConfiguration) {
            int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
            int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);

            if (DEBUG_LAYOUT) Log.v(mTag, "Ooops, something changed!  mWidth="
                    + mWidth + " measuredWidth=" + host.getMeasuredWidth()
                    + " mHeight=" + mHeight
                    + " measuredHeight=" + host.getMeasuredHeight()
                    + " coveredInsetsChanged=" + contentInsetsChanged);

                // Ask host how big it wants to be
            performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);//2.1
            .......
        }
    }
    ....
    final boolean didLayout = layoutRequested && (!mStopped || mReportNextDraw);
    ...
    if(didLayout){
        performLayout(lp, mWidth, mHeight);//2.2
        ....
    }
    .....
    performDraw();//2.3
}

//2.1 performMeasure
private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
    Trace.traceBegin(Trace.TRACE_TAG_VIEW, "measure");
    try {
        //这里mView是DecoreView(extends framelayout)
        //childHeightMeasureSpec，chilchildWidthMeasureSpec是由父view计算出的当前vew的大小
        mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);//3.1
    } finally {
        Trace.traceEnd(Trace.TRACE_TAG_VIEW);
    }
}

```

```java
//3.1
//base.core.java.android.view.View
public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
    .....
    //调用requestlayout方法时，会带有PFLAG_FORCE_LAYOUT标志，因此requestlayout才能强制重新layout和measure
    final boolean forceLayout = (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT;
    ...
    final boolean specChanged = widthMeasureSpec != mOldWidthMeasureSpec
        || heightMeasureSpec != mOldHeightMeasureSpec;
    final boolean isSpecExactly = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
        && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY;
    final boolean matchesSpecSize = getMeasuredWidth() == MeasureSpec.getSize(widthMeasureSpec)
        && getMeasuredHeight() == MeasureSpec.getSize(heightMeasureSpec);
    final boolean needsLayout = specChanged
        && (sAlwaysRemeasureExactly || !isSpecExactly || !matchesSpecSize);
    
    if (forceLayout || needsLayout) {
        ....
        // measure ourselves, this should set the measured dimension flag back
        onMeasure(widthMeasureSpec, heightMeasureSpec);//3.2 测量自己及子view
        mPrivateFlags3 &= ~PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT;
        .....

        mPrivateFlags |= PFLAG_LAYOUT_REQUIRED;
    }
    mOldWidthMeasureSpec = widthMeasureSpec;
    mOldHeightMeasureSpec = heightMeasureSpec;
}

//3.2 测量自己及子view
//view类的默认实现
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
            getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
}

/**
    * Utility to return a default size. Uses the supplied size if the
    * MeasureSpec imposed no constraints. Will get larger if allowed
    * by the MeasureSpec.
    *
    * @param size Default size for this view
    * @param measureSpec Constraints imposed by the parent
    * @return The size this view should be.
    */
public static int getDefaultSize(int size, int measureSpec) {
    int result = size;
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);

    switch (specMode) {
    case MeasureSpec.UNSPECIFIED:
        result = size;
        break;
    case MeasureSpec.AT_MOST:
    case MeasureSpec.EXACTLY:
        result = specSize;
        break;
    }
    return result;
}
```
如果是viewgroup，在onMeasure调用measureChildWithMargins()或者measureChild()实现子view的测量
以FrameLayout为例说明
```java
//base.core.java.android.widget.FrameLayout
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int count = getChildCount();
    ....
    for (int i = 0; i < count; i++) {//1.循环遍历子view
        final View child = getChildAt(i);
        if (mMeasureAllChildren || child.getVisibility() != GONE) {
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);//测量子view
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            maxWidth = Math.max(maxWidth,
                    child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            maxHeight = Math.max(maxHeight,
                    child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
            childState = combineMeasuredStates(childState, child.getMeasuredState());
            if (measureMatchParentChildren) {
                if (lp.width == LayoutParams.MATCH_PARENT ||
                        lp.height == LayoutParams.MATCH_PARENT) {
                    mMatchParentChildren.add(child);
                }
            }
        }
    }

    //设置本view的测量后的宽高
    //maxWidth：该view需要的大小
    //wiwidthMeasureSpec：父view给该view提供的大小
    setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
}

/**
    * Ask one of the children of this view to measure itself, taking into
    * account both the MeasureSpec requirements for this view and its padding
    * and margins. The child must have MarginLayoutParams The heavy lifting is
    * done in getChildMeasureSpec.
    *
    * @param child The child to measure
    * @param parentWidthMeasureSpec The width requirements for this view
    * @param widthUsed Extra space that has been used up by the parent
    *        horizontally (possibly by other children of the parent)
    * @param parentHeightMeasureSpec The height requirements for this view
    * @param heightUsed Extra space that has been used up by the parent
    *        vertically (possibly by other children of the parent)
    */
//core.java.android.view.ViewGroup
//根据父view的尺寸，计算出子view的尺寸，然后调用子view的measure方法开始测量
protected void measureChildWithMargins(View child,
        int parentWidthMeasureSpec, int widthUsed,
        int parentHeightMeasureSpec, int heightUsed) {
    final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

    //根据父view的尺寸和子view想要的尺寸(子view的LayoutParams)，来确定父view能提供给子view的大小
    final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
            mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin
                    + widthUsed, lp.width);
    //根据父view的尺寸和子view想要的尺寸(子view的LayoutParams)，来确定父view能提供给子view的大小
    final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
            mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin
                    + heightUsed, lp.height);

    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);//子view开始测量
}


public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
    int specMode = MeasureSpec.getMode(spec);
    int specSize = MeasureSpec.getSize(spec);

    int size = Math.max(0, specSize - padding);

    int resultSize = 0;
    int resultMode = 0;

    switch (specMode) {
    // Parent has imposed an exact size on us
    case MeasureSpec.EXACTLY:
        if (childDimension >= 0) {
            resultSize = childDimension;
            resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.MATCH_PARENT) {
            // Child wants to be our size. So be it.
            resultSize = size;
            resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.WRAP_CONTENT) {
            // Child wants to determine its own size. It can't be
            // bigger than us.
            resultSize = size;
            resultMode = MeasureSpec.AT_MOST;
        }
        break;

    // Parent has imposed a maximum size on us
    case MeasureSpec.AT_MOST:
        if (childDimension >= 0) {
            // Child wants a specific size... so be it
            resultSize = childDimension;
            resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.MATCH_PARENT) {
            // Child wants to be our size, but our size is not fixed.
            // Constrain child to not be bigger than us.
            resultSize = size;
            resultMode = MeasureSpec.AT_MOST;
        } else if (childDimension == LayoutParams.WRAP_CONTENT) {
            // Child wants to determine its own size. It can't be
            // bigger than us.
            resultSize = size;
            resultMode = MeasureSpec.AT_MOST;
        }
        break;

    // Parent asked to see how big we want to be
    case MeasureSpec.UNSPECIFIED:
        if (childDimension >= 0) {
            // Child wants a specific size... let him have it
            resultSize = childDimension;
            resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.MATCH_PARENT) {
            // Child wants to be our size... find out how big it should
            // be
            resultSize = View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
            resultMode = MeasureSpec.UNSPECIFIED;
        } else if (childDimension == LayoutParams.WRAP_CONTENT) {
            // Child wants to determine its own size.... find out how
            // big it should be
            resultSize = View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
            resultMode = MeasureSpec.UNSPECIFIED;
        }
        break;
    }
    //noinspection ResourceType
    return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
}


public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
    final int specMode = MeasureSpec.getMode(measureSpec);
    final int specSize = MeasureSpec.getSize(measureSpec);
    final int result;
    switch (specMode) {
        case MeasureSpec.AT_MOST:
            if (specSize < size) {
                result = specSize | MEASURED_STATE_TOO_SMALL;
            } else {
                result = size;
            }
            break;
        case MeasureSpec.EXACTLY:
            result = specSize;
            break;
        case MeasureSpec.UNSPECIFIED:
        default:
            result = size;
    }
    return result | (childMeasuredState & MEASURED_STATE_MASK);
}

```

## 2.2
```java
//base.core.java.android.view.ViewRootImpl
private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth,
            int desiredWindowHeight) {
    final View host = mView;//DecoreView
    ....
    host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
}

```
```java
//base.core.java.android.view
/**
 *l/t/r/b 都是相对于父view
 * @param l Left position, relative to parent
 * @param t Top position, relative to parent
 * @param r Right position, relative to parent
 * @param b Bottom position, relative to parent
 */
public void layout(int l, int t, int r, int b) {
    int oldL = mLeft;
    int oldT = mTop;
    int oldB = mBottom;
    int oldR = mRight;

    //设置mLeft mTop,mBottom,mRight
    boolean changed = isLayoutModeOptical(mParent) ?
        setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);

    //调用onLayout的条件：位置发生变换或者是有PFLAG_LAYOUT_REQUIRED这个标志位（调用requestLayout方法时会添加此标志位）
    if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
        onLayout(changed, l, t, r, b);
        ....
    }

    mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
}

private boolean setOpticalFrame(int left, int top, int right, int bottom) {
    Insets parentInsets = mParent instanceof View ?
            ((View) mParent).getOpticalInsets() : Insets.NONE;
    Insets childInsets = getOpticalInsets();
    return setFrame(
            left   + parentInsets.left - childInsets.left,
            top    + parentInsets.top  - childInsets.top,
            right  + parentInsets.left + childInsets.right,
            bottom + parentInsets.top  + childInsets.bottom);
}

protected boolean setFrame(int left, int top, int right, int bottom) {
    boolean changed = false;
    if (mLeft != left || mRight != right || mTop != top || mBottom != bottom) {
        changed = true;
        ...
        int oldWidth = mRight - mLeft;
        int oldHeight = mBottom - mTop;
        int newWidth = right - left;
        int newHeight = bottom - top;
        boolean sizeChanged = (newWidth != oldWidth) || (newHeight != oldHeight);

        // Invalidate our old position
        invalidate(sizeChanged);//通知view重新绘制draw

        //为left，mTop，mRight，mBottom赋值
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
        ....
    }
    return changed;
}

/**
    * Called from layout when this view should
    * assign a size and position to each of its children.
    *
    * Derived classes with children should override
    * this method and call layout on each of
    * their children.
    * @param changed This is a new size or position for this view
    * @param left Left position, relative to parent
    * @param top Top position, relative to parent
    * @param right Right position, relative to parent
    * @param bottom Bottom position, relative to parent
    */
//从上面的注释可以看出，onLayout方法是给子view设置位置的。所以View不需要实现此方法，在ViewGroup中会有实现。
protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
}

```
```java
//base.core.java.android.view.ViewGroup
@Override
protected abstract void onLayout(boolean changed,
        int l, int t, int r, int b);
```
可以看到ViewGroup覆盖了onLayout方法，并且设置为abstract，所以继承自ViewGroup的类都需要重写此方法。
接下来一FrameLayout的onLayout为例讲解。

```java
//base.core.java.android.widget.FrameLayout
@Override
protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    layoutChildren(left, top, right, bottom, false /* no force left gravity */);
}

void layoutChildren(int left, int top, int right, int bottom, boolean forceLeftGravity) {
    final int count = getChildCount();//获取ziView的个数
    //以下4行是获取此view内容区域（除去上下左右的padding）的坐标。
    //因为是相对坐标，所以以此view的左上角为圆点。
    final int parentLeft = getPaddingLeftWithForeground();
    final int parentRight = right - left - getPaddingRightWithForeground();

    final int parentTop = getPaddingTopWithForeground();
    final int parentBottom = bottom - top - getPaddingBottomWithForeground();

    for (int i = 0; i < count; i++) {
        final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = DEFAULT_CHILD_GRAVITY;
                }

                final int layoutDirection = getLayoutDirection();
                final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
                        lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        if (!forceLeftGravity) {
                            childLeft = parentRight - width - lp.rightMargin;
                            break;
                        }
                    case Gravity.LEFT:
                    default:
                        childLeft = parentLeft + lp.leftMargin;
                }

                switch (verticalGravity) {
                    case Gravity.TOP:
                        childTop = parentTop + lp.topMargin;
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop = parentTop + (parentBottom - parentTop - height) / 2 +
                        lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = parentBottom - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = parentTop + lp.topMargin;
                }

                child.layout(childLeft, childTop, childLeft + width, childTop + height);//调用子view的layout的方法。然后子view也会重复以上流程
            }
    }
}
```
## 2.3
```java
//  base.core.java.android.view.ViewRootImpl
private void performDraw() {
    ...
    draw(fullRedrawNeeded);
    ...
}

private void draw(boolean fullRedrawNeeded) {
    ...
    if (!drawSoftware(surface, mAttachInfo, xOffset, yOffset, scalingRequired, dirty)) {
        return;
    }
    ...
}

private boolean drawSoftware(Surface surface, AttachInfo attachInfo, int xoff, int yoff,
            boolean scalingRequired, Rect dirty) {
    ...
    mView.draw(canvas);
    ...
}

// base.core.java.android.view
public void draw(Canvas canvas) {
    // Step 1, draw the background, if needed
    int saveCount;

    if (!dirtyOpaque) {
        drawBackground(canvas);
    }

    // Step 2, save the canvas' layers
    //这一步主要是根据layout时设置的位置mleft/mRight,mTop,mBotton来移动画布
    int paddingLeft = mPaddingLeft;   
    ...
    int left = mScrollX + paddingLeft;
    int right = left + mRight - mLeft - mPaddingRight - paddingLeft;
    int top = mScrollY + getFadeTop(offsetRequired);
    int bottom = top + getFadeHeight(offsetRequired); 

    saveCount = canvas.getSaveCount();
    ...
    canvas.saveLayer(left, top, right, top + length, null, flags);
    ....

    // Step 3, draw the content 绘制内容
    if (!dirtyOpaque) onDraw(canvas);

    // Step 4, draw the children 通知子view绘制
    dispatchDraw(canvas);

    // Step 5, draw the fade effect and restore layers

    canvas.restoreToCount(saveCount);

    // Step 6, draw decorations (foreground, scrollbars)
    onDrawForeground(canvas);
}

//base.core.java.android.view.ViewGroup
@Override
protected void dispatchDraw(Canvas canvas) {
    ...
    for (int i = 0; i < childrenCount; i++) {
        ...
        more |= drawChild(canvas, transientChild, drawingTime);
        ....
    }
}
protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    return child.draw(canvas, this, drawingTime);
}

//base.core.java.android.view.View
/**
    * This method is called by ViewGroup.drawChild() to have each child view draw itself.
    *
    * This is where the View specializes rendering behavior based on layer type,
    * and hardware acceleration.
    */
//通过上面的注释可以了解到，这个方法是通过viewgroup的drawchild方法调用的，目的使用了绘制viewgroup中的子view
boolean draw(Canvas canvas, ViewGroup parent, long drawingTime) {
    ....
    //其中一段是这样的
    //判断有没有PFLAG_SKIP_DRAW标志，如果有的话直接调用dispatchDraw方法通知子view绘制，不在调用draw方法。
    if ((mPrivateFlags & PFLAG_SKIP_DRAW) == PFLAG_SKIP_DRAW) {
        mPrivateFlags &= ~PFLAG_DIRTY_MASK;
        dispatchDraw(canvas);
    } else {
        draw(canvas);
    }
}

//创建View Group的时候，会默认设置WILL_NOT_DRAW
public ViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initViewGroup();
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
}

private void initViewGroup() {
        // ViewGroup doesn't draw by default
        if (!debugDraw()) {
            setFlags(WILL_NOT_DRAW, DRAW_MASK);
        }
}

void setFlags(int flags, int mask) {
    ....
    if ((changed & DRAW_MASK) != 0) {
        if ((mViewFlags & WILL_NOT_DRAW) != 0) {
            if (mBackground != null
                    || (mForegroundInfo != null && mForegroundInfo.mDrawable != null)) {
                mPrivateFlags &= ~PFLAG_SKIP_DRAW;
            } else {
                mPrivateFlags |= PFLAG_SKIP_DRAW;
            }
        } else {
            mPrivateFlags &= ~PFLAG_SKIP_DRAW;
        }
        requestLayout();
        invalidate(true);
    }
    ...
}
//上面可以看出设置WILL_NOT_DRAW，在没有给ViewGroup设置背景的情况下实际上会添加PFLAG_SKIP_DRAW标志，从而导致
//ViewGroup绘制时不掉用draw(canvas)方法，而是直接调用disdispatchDraw方法通知子view绘制。
//可以通过调用setWillNotDraw方法或者给ViewGroup设置背景，去掉此标志，使ViewGruop调用draw方法

public void setWillNotDraw(boolean willNotDraw) {
    setFlags(willNotDraw ? WILL_NOT_DRAW : 0, DRAW_MASK);
}
```

# requestLayout 和invalidate的区别

* ## requestLayout()
```java
//base.core.java.android.view
public void requestLayout() {
    ...
    //添加了下面两个标志位
    mPrivateFlags |= PFLAG_FORCE_LAYOUT;
    mPrivateFlags |= PFLAG_INVALIDATED;
    
    //调用父view的requestlayout方法
    //这样一级一级往上调用直到DecoreView,从前面的分析可以知道，DecoreView的parent是ViewRootImpl。
    //所以最后会调用到ViewRootImpl的requestLayout方法，然后按照之前的分析开始measure，layout，draw的调用
    if (mParent != null && !mParent.isLayoutRequested()) {
        mParent.requestLayout();
    }
}


public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
    ....
    //在reqrequestLayout方法中设置了mPrivateFlags |= PFLAG_FORCE_LAYOUT;标志为，所以view会重新计算
    final boolean forceLayout = (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT;
    ....
    if (forceLayout || needsLayout) {
        ...
        onMeasure(widthMeasureSpec, heightMeasureSpec);
        ...
        mPrivateFlags |= PFLAG_LAYOUT_REQUIRED;//设置layout标志位
        ...
    }
    ...
}

public void layout(int l, int t, int r, int b) {
    ...
    //measure之后会设置layout标志位mPrivateFlags |= PFLAG_LAYOUT_REQUIRED，所以onlayout也会调用
    if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
        onLayout(changed, l, t, r, b);
        ....
    }
}
//接下来就会调用draw
```
<font color='red'>**所以reqrequestLayout()方法会使整个view树重新measure，layout，draw**</font>

* ## invalidate()

<font color='red'>**invalidate()方法会使当前view重绘**</font>