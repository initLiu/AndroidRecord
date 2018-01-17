```java
//android.app.ActivityThread
public void handleMessage(Message msg) {
    switch (msg.what) {
        ..
        case LAUNCH_ACTIVITY: {
            Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
            final ActivityClientRecord r = (ActivityClientRecord) msg.obj;

            r.packageInfo = getPackageInfoNoCheck(
                    r.activityInfo.applicationInfo, r.compatInfo);
            handleLaunchActivity(r, null);//启动activity
            Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        } break;

        ...
    }
}
private void handleLaunchActivity(ActivityClientRecord r, Intent customIntent) {
    ....
    Activity a = performLaunchActivity(r, customIntent);//1.1 开始启动Activity
    ..
    handleResumeActivity(r.token, false, r.isForward,
                    !r.activity.mFinished && !r.startsNotResumed);//1.2 处理activity resume
    if (!r.activity.mFinished && r.startsNotResumed) {//1.3 如果activi finish
        mInstrumentation.callActivityOnPause(r.activity);//1.4 调用Activity的onpause
    }
    .....
}

//1.1 开始启动Activity
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
    ....
    Activity activity = null;
    //创建一个activity对象
    try {
        java.lang.ClassLoader cl = r.packageInfo.getClassLoader();
        activity = mInstrumentation.newActivity(
                cl, component.getClassName(), r.intent);
        StrictMode.incrementExpectedActivityCount(activity.getClass());
        r.intent.setExtrasClassLoader(cl);
        r.intent.prepareToEnterProcess();
        if (r.state != null) {
            r.state.setClassLoader(cl);
        }
    } catch (Exception e) {
        if (!mInstrumentation.onException(activity, e)) {
            throw new RuntimeException(
                "Unable to instantiate activity " + component
                + ": " + e.toString(), e);
        }
    }

    ...
    Application app = r.packageInfo.makeApplication(false, mInstrumentation);
    ...
    if (activity != null) {
        Context appContext = createBaseContextForActivity(r, activity);//创建context
        ...
        //2.1 调用attach方法，设置Activity的content window等对象
        activity.attach(appContext, this, getInstrumentation(), r.token,
                        r.ident, app, r.intent, r.activityInfo, title, r.parent,
                        r.embeddedID, r.lastNonConfigurationInstances, config,
                        r.referrer, r.voiceInteractor);
        ...
        int theme = r.activityInfo.getThemeResource();
        if (theme != 0) {
            activity.setTheme(theme);
        }
        ...
        activity.mCalled = false;
        //调用activity的oncreate方法
        if (r.isPersistable()) {
            mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);
        } else {
            mInstrumentation.callActivityOnCreate(activity, r.state);
        }

        //调用Activity的onStart方法
        if (!r.activity.mFinished) {
            activity.performStart();
            r.stopped = false;
        }
    }
}

//1.2
final void handleResumeActivity(IBinder token,
            boolean clearHide, boolean isForward, boolean reallyResume) {
    ...
    ActivityClientRecord r = performResumeActivity(token, clearHide);//调用activity的onResume方法
    ...
    final Activity a = r.activity;
    ...
    if (r.window == null && !a.mFinished && willBeVisible) {
        r.window = r.activity.getWindow();
        View decor = r.window.getDecorView();
        decor.setVisibility(View.INVISIBLE);
        ViewManager wm = a.getWindowManager();
        WindowManager.LayoutParams l = r.window.getAttributes();
        a.mDecor = decor;
        l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
        l.softInputMode |= forwardBit;
        if (a.mVisibleFromClient) {
            a.mWindowAdded = true;
            wm.addView(decor, l);//3.1 调用WindowManager(WindowManagerImpl)添加decorView
        }
    }

}
```
```java
//2.1
//android.app.Activity
final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config, String referrer, IVoiceInteractor voiceInteractor) {
    attachBaseContext(context);
    //设置Activity的content，实际上调用了
    //ContextThemeWrapper.attachBaseContext
    //ContextWrapper.attachBaseContext
    //然后把context赋值给了mBase
    ...
    mWindow = new PhoneWindow(this);
    ...
    mToken = token;
    ...
    //设置windowmanager
    mWindow.setWindowManager(
                (WindowManager)context.getSystemService(Context.WINDOW_SERVICE),
                mToken, mComponent.flattenToString(),
                (info.flags & ActivityInfo.FLAG_HARDWARE_ACCELERATED) != 0);
    mWindowManager = mWindow.getWindowManager();
}
```
```java
//3.1
//android.view.WindowManagerImpl.java
public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
    applyDefaultToken(params);
    mGlobal.addView(view, params, mDisplay, mParentWindow);
}
//android.view.WindowManagerGlobal.java
public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow) {
    if (view == null) {
        throw new IllegalArgumentException("view must not be null");
    }
    if (display == null) {
        throw new IllegalArgumentException("display must not be null");
    }
    ...
    ViewRootImpl root;
    root = new ViewRootImpl(view.getContext(), display);//4.1 创建一个ViewRootImpl对象
    ...
    view.setLayoutParams(wparams);
    ...
    root.setView(view, wparams, panelParentView);//4.2 
    ...
}
```
```java
//4.1
//android.view.ViewRootImpl.java
public ViewRootImpl(Context context, Display display) {
    mContext = context;
    mWindowSession = WindowManagerGlobal.getWindowSession();
    ...
    mWindow = new W(this);
    ...
    mFirst = true; // true for the first time the view is added
    mAdded = false;
    mAttachInfo = new View.AttachInfo(mWindowSession, mWindow, display, this, mHandler, this);
}
//4.2
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
    synchronized (this) {
        if (mView == null) {
            mView = view;
            ....
            mAdded = true;
            // Schedule the first layout -before- adding to the window
            // manager, to make sure we do the relayout before receiving
            // any other events from the system.
            requestLayout();//4.3 最后调用到performTraversals 第一次measure，layout，draw
            ...
            try {
                mOrigWindowType = mWindowAttributes.type;
                mAttachInfo.mRecomputeGlobalAttributes = true;
                collectViewAttributes();
                //把window
                res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
                        getHostVisibility(), mDisplay.getDisplayId(),
                        mAttachInfo.mContentInsets, mAttachInfo.mStableInsets,
                        mAttachInfo.mOutsets, mInputChannel);
            } catch (RemoteException e) {
                ....
            }
            ...
            view.assignParent(this);//把viewrootimpl设置为decorview的parent
            ...
        }
        ....
    }
}
//4.3
private void performTraversals() {
    final View host = mView;
    ...
    if (mFirst) {
        ....
        host.dispatchAttachedToWindow(mAttachInfo, 0);//5.1 给每个子view设置mAttachInfo
        ....
    }
}
```
```java
//5.1
//android.view.View
void dispatchAttachedToWindow(AttachInfo info, int visibility) {
    mAttachInfo = info;
    ....
    onAttachedToWindow();
    ....
    int vis = info.mWindowVisibility;
    if (vis != GONE) {
        onWindowVisibilityChanged(vis);
    }
    ...
}
```