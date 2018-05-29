  通常我们都是通过Activity中的一个setContentView方法进行初始化布局的。现在就从Activity的setContentView开始进行讨论。

    /**
     * Set the activity content from a layout resource.  The resource will be
     * inflated, adding all top-level views to the activity.
     *
     * @param layoutResID Resource ID to be inflated.
     *
     * @see #setContentView(android.view.View)
     * @see #setContentView(android.view.View, android.view.ViewGroup.LayoutParams)
     */
    public void setContentView(@LayoutRes int layoutResID) {
        getWindow().setContentView(layoutResID);
        initWindowDecorActionBar();
    }
getWindow()方法返回了Window对象，可以看出setContentView其实是调用了Window.setContentView进行添加布局的。

    /**
     * Retrieve the current {@link android.view.Window} for the activity.
     * This can be used to directly access parts of the Window API that
     * are not available through Activity/Screen.
     *
     * @return Window The current window, or null if the activity is not
     *         visual.
     */
    public Window getWindow() {
        return mWindow;
    }
Window是一个抽象类,根据注释我们能够看到PhoneWindow是唯一的实现类。其实在Activity的attach方法中也可以看到mWindow其实是PhoneWindow类型。

    /**
     * Abstract base class for a top-level window look and behavior policy.  An
     * instance of this class should be used as the top-level view added to the
     * window manager. It provides standard UI policies such as a background, title
     * area, default key processing, etc.
     *
     * <p>The only existing implementation of this abstract class is
     * android.view.PhoneWindow, which you should instantiate when needing a
     * Window.
     */
    public abstract class Window {
        ...........
        ...........
        
        /**
         * Convenience for
         * {@link #setContentView(View, android.view.ViewGroup.LayoutParams)}
         * to set the screen content from a layout resource.  The resource will be
         * inflated, adding all top-level views to the screen.
         *
         * @param layoutResID Resource ID to be inflated.
         * @see #setContentView(View, android.view.ViewGroup.LayoutParams)
         */
        public abstract void setContentView(@LayoutRes int layoutResID);
    }
PhoneWindow，该类继承自Window是Window类的具体实现，即我们可以通过该类具体去绘制窗口。并且，该类内部包含了一个DecorView对象，改DecorView对象时所有应用窗口(Activity界面)的根View。简而言之，PhoneWindow类是把一个FrameLayout类即DecorView对象进行一定的包装，将它作为应用窗口的根View,并且提供一组通用的窗口操作接口。其中还有一个ViewGroup成员mContentParent。因为一个Activity显示的内容还有状态栏和ActionBar，mContentParent就是指放置我们布局文件的区域，他也是DecorView的一个子视图。


    /**
     * Android-specific Window.
     * <p>
     * todo: need to pull the generic functionality out into a base class
     * in android.widget.
     *
     * @hide
     */
    public class PhoneWindow extends Window implements MenuBuilder.Callback {
        ......
        ......
        ......
        // This is the top-level view of the window, containing the window decor.
        private DecorView mDecor;

        // This is the view in which the window contents are placed. It is either
        // mDecor itself, or a child of mDecor where the contents go.
        private ViewGroup mContentParent;

        private ViewGroup mContentRoot;
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
        }
    }
