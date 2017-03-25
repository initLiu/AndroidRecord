View的绘制是从ViewRootImpl的scheduleTraversals()方法开始的。
```java

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
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);//mTraversalRunnable中执行view的绘制
            if (!mUnbufferedInputDispatch) {
                scheduleConsumeBatchedInput();
            }
            notifyRendererOfFramePending();
            pokeDrawLockIfNeeded();
        }
    }

void doTraversal() {
    .......
            performTraversals();//真正的绘制是从这里开始的
    ......
    }

private void performTraversals() {
    ......
    Rect frame = mWinFrame;
        if (mFirst) {
            //首次调用会将这两个变量都置为true
            mFullRedrawNeeded = true;
            mLayoutRequested = true;
            ......
        } else{
            //不是首次调用的话，判断宽高是否改变，给mLayoutRequested赋值。
            desiredWindowWidth = frame.width();
            desiredWindowHeight = frame.height();
            if (desiredWindowWidth != mWidth || desiredWindowHeight != mHeight) {
                if (DEBUG_ORIENTATION) Log.v(TAG,
                        "View " + host + " resized to: " + frame);
                mFullRedrawNeeded = true;
                mLayoutRequested = true;
                windowSizeMayChange = true;
            }
        }
        .....
        boolean layoutRequested = mLayoutRequested && (!mStopped || mReportNextDraw);

        .....

        //layoutRequested布局发生变更并且view的大小发生了改变
        boolean windowShouldResize = layoutRequested && windowSizeMayChange
            && ((mWidth != host.getMeasuredWidth() || mHeight != host.getMeasuredHeight())
                || (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT &&
                        frame.width() < desiredWindowWidth && frame.width() != mWidth)
                || (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT &&
                        frame.height() < desiredWindowHeight && frame.height() != mHeight));
        if (mFirst || windowShouldResize || insetsChanged ||
                viewVisibilityChanged || params != null) {

                ....
                if (focusChangedDueToTouchMode || mWidth != host.getMeasuredWidth()
                        || mHeight != host.getMeasuredHeight() || contentInsetsChanged) {
                    int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
                    int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);
                    ......
                    performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
                    ...
                    if (lp.horizontalWeight > 0.0f) {
                        width += (int) ((mWidth - width) * lp.horizontalWeight);
                        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width,
                                MeasureSpec.EXACTLY);
                        measureAgain = true;
                    }
                    if (lp.verticalWeight > 0.0f) {
                        height += (int) ((mHeight - height) * lp.verticalWeight);
                        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height,
                                MeasureSpec.EXACTLY);
                        measureAgain = true;
                    }
                    //如果LayoutParams设置了horizontalWeight或者verticalWeight，则需要重新进行一次measure
                    if (measureAgain) {
                        if (DEBUG_LAYOUT) Log.v(TAG,
                                "And hey let's measure once more: width=" + width
                                + " height=" + height);
                        performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
                    }
                }
        }

        final boolean didLayout = layoutRequested && (!mStopped || mReportNextDraw);
        ....
        if (didLayout) {
            //开始layout
            performLayout(lp, desiredWindowWidth, desiredWindowHeight);
            ......
        }

        if (!cancelDraw && !newSurface) {
            if (!skipDraw || mReportNextDraw) {
                if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                    for (int i = 0; i < mPendingTransitions.size(); ++i) {
                        mPendingTransitions.get(i).startChangingAnimations();
                    }
                    mPendingTransitions.clear();
                }

                performDraw();//开始draw
            }
        } else {
            if (viewVisibility == View.VISIBLE) {
                // Try again
                scheduleTraversals();
            } else if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                for (int i = 0; i < mPendingTransitions.size(); ++i) {
                    mPendingTransitions.get(i).endChangingAnimations();
                }
                mPendingTransitions.clear();
            }
        }
}
```
view绘制流程大致上经历了performMeasure->performLayout->performDraw三个过程

# View绘制流程第一步：递归measure源码分析

```java
private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
        Trace.traceBegin(Trace.TRACE_TAG_VIEW, "measure");
        try {
            mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);//对于Activity来说这里的mView就是DecorView
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_VIEW);
        }
}

/**
     * <p>
     * This is called to find out how big a view should be. The parent
     * supplies constraint information in the width and height parameters.
     * </p>
     *
     * <p>
     * The actual measurement work of a view is performed in
     * {@link #onMeasure(int, int)}, called by this method. Therefore, only
     * {@link #onMeasure(int, int)} can and must be overridden by subclasses.
     * </p>
     *
     *
     * @param widthMeasureSpec Horizontal space requirements as imposed by the
     *        parent
     * @param heightMeasureSpec Vertical space requirements as imposed by the
     *        parent
     *
     * @see #onMeasure(int, int)
     */
public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
    .....
    onMeasure(widthMeasureSpec, heightMeasureSpec);
    ......
}
```
measure方法为整个View数计算实际的大小，然后设置实际的高和宽，每个View空间的实际宽高都是由父视图和自身决定的。时间的
测量是在onMeasure方法进行，所以在View的子类需要重写onMeasure方法，这是因为measure方法是final的，不允许重载，所以View子类
只能通过重载onMeasure来实现自己的测量逻辑。
这个方法的两个参数都是父View传递过来的，也就是代表了父view的规格。他有两部分组成，高2位表示MODE，定义在MeasureSpec类中，有
三种类型，MeasureSpec.EXACTLY表示确定大小，MeasureSpec.AT_MOST表示最大大小，MeasureSpec.UNSPECIFIED不确定。低30位表示size，
也就是父View的大小。对于系统Window类的DecorView对象Mode一般都为MeasureSpec.EXACTLY，而size分别对应屏幕宽高。
在这里可以看出measure方法最终回调了View的onMeasure方法，
```java
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }
```
对于非ViewGroup的View而言，通过调用上面默认的onMeasure即可完成View的测量，当然你也可以重载onMeasure并调用
setMeasuredDimension来设置任意大小的布局。

我们来看一下onMeasure的默认实现，仅调用了setMeasuredDimension方法。setMeasuredDimension对view的成员变量mMeasuredWidth和mMeasuredHeight进行赋值。
measure的主要目的就是对view树中的每个view的mMeasuredHeight和mMeasuredWidth进行赋值，所以一旦这两个变量被赋值意味着该View的测量工作结束。
```java
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
到此一次最基础的元素View的measure过程就完成了。上面说了View实际是嵌套的，而且measure是递归传递的，所以每个View都需要measure。实际能够嵌套的View一般都是ViewGroup的子类。所以在ViewGroup中定义了
measureChildren、measureChild、measureChildWithMargins方法来对子视图进行测量。以LinearLayout为例。
```java
@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mOrientation == VERTICAL) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }
```

# View绘制流程第一步：递归layout源码分析
```java
private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth,
            int desiredWindowHeight) {
    .....
    host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
    .....
}
```
既然layout是递归结构，那我们先下ViewGroup的layout方法。
```java
@Override
    public final void layout(int l, int t, int r, int b) {
        if (!mSuppressLayout && (mTransition == null || !mTransition.isChangingLayout())) {
            if (mTransition != null) {
                mTransition.layoutChange(this);
            }
            super.layout(l, t, r, b);
        } else {
            // record the fact that we noop'd it; request layout when transition finishes
            mLayoutCalledWhileSuppressed = true;
        }
    }
```
从代码看最后还是调用了父类View的layout方法。
```java
public void layout(int l, int t, int r, int b) {
    .....

    //实质都是调用setFrame方法把参数mLeft,mTop,mRight,mBottom这几个变量
    //判断View的位置是否发生过变化，已确定有没有必要对当前的view进行重新layout
    boolean changed = isLayoutModeOptical(mParent) ?
                setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);

        if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
            onLayout(changed, l, t, r, b);
            ......
        }
    ......
}
```
对比上面View的layout和ViewGroup的layout可以发现，View的layout方法是可以在子类重写的，而ViewGroup的layout是不能在子类重写，
言外之意就是说ViewGroup中只能通过重写onLayout方法，那我们接下来看看ViewGroup的onlayout方法。
```java
@Override
    protected abstract void onLayout(boolean changed,
            int l, int t, int r, int b);
```
ViewGroupde的onlayout方法是一个抽象方法，这就是所有ViewGroup的子类都必须重写这个方法。所以在自定义ViewGroup空间中，
onLayout配合onMeasure方法一起使用可以实现自定义View的复杂布局。自定义View首先调用onMeasure进行测量，然后调用onLayout
方法动态获取子View和子View的测量大小，然后进行layout布局。重载onLayout的目的就是安排其children在父View的具体位置，重载
onLayout通常做法就是写一个for循环调用每一个子视图的layout函数，来确定每个子视图在父视图中的显示位置。

# View绘制流程第三步：递归draw源码分析
draw过程也是在ViewRootImpl的performTraversals()内部调用的，其调用顺序在measure()和layout()之后，这里的mView对于Activity来说
就是PhoneWindow.DecorView,ViewRootImpl中的代码会创建一个Canvas对象，然后调用View的draw()方法来执行具体的绘制工作。

```java
//ViewRootImpl.java
private void performDraw() {
    ....
    draw(fullRedrawNeeded);
    ....
}

 private boolean drawSoftware(Surface surface, AttachInfo attachInfo, int xoff, int yoff,
            boolean scalingRequired, Rect dirty) {
    final Canvas canvas;
    .....
    canvas = mSurface.lockCanvas(dirty);
    .....
    mView.draw(canvas);
    ....
}
```
由于ViewGroup没有重写View的draw方法，所以直接从View的draw方法开始分析：
```java
public void draw(Canvas canvas) {
    ....
    // Step 1, draw the background, if needed
    int saveCount;
    if (!dirtyOpaque) {
        drawBackground(canvas);
    }
    // skip step 2 & 5 if possible (common case)
    
    if (!verticalEdges && !horizontalEdges) {
        // Step 3, draw the content
        if (!dirtyOpaque) onDraw(canvas);
        // Step 4, draw the children
            dispatchDraw(canvas);
        // Step 6, draw decorations (foreground, scrollbars)
            onDrawForeground(canvas);
        return;
    }

    // Step 2, save the canvas' layers
    if (drawTop) {
        canvas.saveLayer(left, top, right, top + length, null, flags);
    }
    .....
    // Step 3, draw the content
    if (!dirtyOpaque) onDraw(canvas);

    // Step 4, draw the children
    dispatchDraw(canvas);

    // Step 5, draw the fade effect and restore layers
    if (drawTop) {
            matrix.setScale(1, fadeHeight * topFadeStrength);
            matrix.postTranslate(left, top);
            fade.setLocalMatrix(matrix);
            p.setShader(fade);
            canvas.drawRect(left, top, right, top + length, p);
    }
    canvas.restoreToCount(saveCount);

    // Step 6, draw decorations (foreground, scrollbars)
    onDrawForeground(canvas);
}
```
### 第一步对View背景进行绘制
```java
private void drawBackground(Canvas canvas) {
    .....
}
```
### 第三步对view的内容进行绘制
```java
/**
     * Implement this to do your drawing.
     *
     * @param canvas the canvas on which the background will be drawn
     */
protected void onDraw(Canvas canvas) {
}
```
可以看见，这是一个空方法。因为每个View的内容部分是各不相同的，所以需要由子类去实现具体逻辑。

### 第四步，对当前View的所有子View进行绘制
```java
/**
     * Called by draw to draw the child views. This may be overridden
     * by derived classes to gain control just before its children are drawn
     * (but after its own view has been drawn).
     * @param canvas the canvas on which to draw the view
     */
protected void dispatchDraw(Canvas canvas) {

    }
```
View的dispatchDraw()方法是一个空方法，而且注释说明了如果View包含子类需要重写他，所以我们有必要看下ViewGroup的dispatchDraw方法源码（这也就是刚刚说的对当前View的所有子View进行绘制，如果当前的View没有子View就不需要进行绘制的原因，因为如果是View调运该方法是空的，而ViewGroup才有实现），如下：
```java
protected void dispatchDraw(Canvas canvas) {
    ....
    for (int i = 0; i < childrenCount; i++) {
            while (transientIndex >= 0 && mTransientIndices.get(transientIndex) == i) {
                final View transientChild = mTransientViews.get(transientIndex);
                if ((transientChild.mViewFlags & VISIBILITY_MASK) == VISIBLE ||
                        transientChild.getAnimation() != null) {
                    more |= drawChild(canvas, transientChild, drawingTime);
                }
                transientIndex++;
                if (transientIndex >= transientCount) {
                    transientIndex = -1;
                }
            }
            int childIndex = customOrder ? getChildDrawingOrder(childrenCount, i) : i;
            final View child = (preorderedList == null)
                    ? children[childIndex] : preorderedList.get(childIndex);
            if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE || child.getAnimation() != null) {
                more |= drawChild(canvas, child, drawingTime);
            }
        }
    // Draw any disappearing views that have animations
        if (mDisappearingChildren != null) {
            final ArrayList<View> disappearingChildren = mDisappearingChildren;
            final int disappearingCount = disappearingChildren.size() - 1;
            // Go backwards -- we may delete as animations finish
            for (int i = disappearingCount; i >= 0; i--) {
                final View child = disappearingChildren.get(i);
                more |= drawChild(canvas, child, drawingTime);
            }
        }
}
```
可以看见，ViewGroup确实重写了View的dispatchDraw()方法，该方法内部会遍历每个子View，然后调用drawChild()方法，我们可以看下ViewGroup的drawChild方法，如下：
```java
protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        return child.draw(canvas, this, drawingTime);
}
```
可以看见drawChild()方法调运了子View的draw()方法。所以说ViewGroup类已经为我们重写了dispatchDraw()的功能实现，我们一般不需要重写该方法，但可以重载父类函数实现具体的功能。


# DecorView的创建过程
Activity显示视图是通过setContentView设置的
```java
public void setContentView(@LayoutRes int layoutResID) {
        getWindow().setContentView(layoutResID);
        initWindowDecorActionBar();
    }
```
代码中显示最后调用了Window的setContentView设置布局。
```java
final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config, String referrer, IVoiceInteractor voiceInteractor,
            Window window) {
        attachBaseContext(context);
        ......

        mWindow = new PhoneWindow(this, window);//mWindow是PhoneWindow类型
        ......
}
```
```java
//PhoneWindow.java
public class PhoneWindow extends Window implements MenuBuilder.Callback {
    ....
    // This is the top-level view of the window, containing the window decor.
    private DecorView mDecor;//这是最顶成的View
    .....
    // This is the view in which the window contents are placed. It is either
    // mDecor itself, or a child of mDecor where the contents go.
    ViewGroup mContentParent;//DecorView子view或者是DecorView本身
    ......
    @Override
    public void setContentView(int layoutResID) {
        // Note: FEATURE_CONTENT_TRANSITIONS may be set in the process of installing the window
        // decor, when theme attributes and the like are crystalized. Do not check the feature
        // before this happens.
        if (mContentParent == null) {
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            mContentParent.removeAllViews();
        }

        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            final Scene newScene = Scene.getSceneForLayout(mContentParent, layoutResID,
                    getContext());
            transitionTo(newScene);
        } else {
            mLayoutInflater.inflate(layoutResID, mContentParent);
        }
        mContentParent.requestApplyInsets();
        final Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();
        }
        mContentParentExplicitlySet = true;
    }
}

public class DecorView extends FrameLayout implements RootViewSurfaceTaker, WindowCallbacks {
    .....
    //DecorView继承FrameLayout
}
```
mContentParent为null，调用installDecor方法初始化DecorView.
```java
private void installDecor() {
    mForceDecorInstall = false;
    if (mDecor == null) {
        mDecor = generateDecor(-1);//构建DecorView
        mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        mDecor.setIsRootNamespace(true);
        if (!mInvalidatePanelMenuPosted && mInvalidatePanelMenuFeatures != 0) {
            mDecor.postOnAnimation(mInvalidatePanelMenuRunnable);
        }
    } else {
        mDecor.setWindow(this);
    }

    if (mContentParent == null) {
        mContentParent = generateLayout(mDecor);//构建mContentParent
    }
}

//初始化DecorView
protected DecorView generateDecor(int featureId) {
        // System process doesn't have application context and in that case we need to directly use
        // the context we have. Otherwise we want the application context, so we don't cling to the
        // activity.
        Context context;
        if (mUseDecorContext) {
            Context applicationContext = getContext().getApplicationContext();
            if (applicationContext == null) {
                context = getContext();
            } else {
                context = new DecorContext(applicationContext, getContext().getResources());
                if (mTheme != -1) {
                    context.setTheme(mTheme);
                }
            }
        } else {
            context = getContext();
        }
        return new DecorView(context, featureId, this, getAttributes());
    }
 protected ViewGroup generateLayout(DecorView decor) {
     .....
     ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);//com.android.internal.R.id.content;
     .....
     return contentParent;
 }
```
查找com.android.internal.R.id.content的布局，我们接下来看看findViewById方法的实现
```java
 public View findViewById(@IdRes int id) {
        return getDecorView().findViewById(id);
    }
```
原来是从DecorView中查找R.id.content。可以看出mContentParent是DecorView的一个子View。


# ViewRootImpl的创建过程
Activity 的onResume方法是通过ActivityThread的handleResumeActivity方法调用启动的。
```java
//ActivityThread.java
final void handleResumeActivity(IBinder token,
            boolean clearHide, boolean isForward, boolean reallyResume, int seq, String reason) {
    .....
    // TODO Push resumeArgs into the activity for consideration
    r = performResumeActivity(token, clearHide, reason);//resume Activity
    ....
    if (r.window == null && !a.mFinished && willBeVisible) {
        .....
        r.window = r.activity.getWindow();
        View decor = r.window.getDecorView();
        decor.setVisibility(View.INVISIBLE);
        ViewManager wm = a.getWindowManager();//通过调用关系可以找到ViewManager是WindowManagerImpl类型
        a.mDecor = decor;
        ...
        if (a.mVisibleFromClient && !a.mWindowAdded) {
            a.mWindowAdded = true;
            wm.addView(decor, l);//调用WindowManagerImpl的addView方法
        }
    }
}
```
```java
//WindowManagerImpl.java
@Override
public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
    applyDefaultToken(params);
    mGlobal.addView(view, params, mContext.getDisplay(), mParentWindow);
}

//WindowManagerGlobal.java
public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow) {
    ....
    ViewRootImpl root;
    View panelParentView = null;
    .....
    root = new ViewRootImpl(view.getContext(), display);

    view.setLayoutParams(wparams);

    mViews.add(view);
    mRoots.add(root);
    mParams.add(wparams);
    ...
     // do this last because it fires off messages to start doing things
    root.setView(view, wparams, panelParentView);
    ....
}

//ViewRootImpl.java
 /**
     * We have one child
     */
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
    synchronized (this) {
        if (mView == null) {
            mView = view;//这里mView就等于DecorView
            ......
            view.assignParent(this);//为DecorView设置mParent。这里就解释了为什么ViewRootImpl是DecorView的parent
            ....
        }
        .....
    }
    ...........
}

//View.java
/*
     * Caller is responsible for calling requestLayout if necessary.
     * (This allows addViewInLayout to not request a new layout.)
     */
    void assignParent(ViewParent parent) {
        if (mParent == null) {
            mParent = parent;
        } else if (parent == null) {
            mParent = null;
        } else {
            throw new RuntimeException("view " + this + " being added, but"
                    + " it already has a parent");
        }
    }
```