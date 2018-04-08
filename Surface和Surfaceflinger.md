# Surface和Surfaceflinger
## 1.Surface定义
Surface在源码中定义
```java
/**
 * Handle onto a raw buffer that is being managed by the screen compositor.
 */  
public class Surface implements Parcelable {
}
```
简单翻译一下就是：**操作原始图像缓冲区的句柄，而原始图像缓冲区是由screen compositor管理。**  
这个screen compositor其实就是Surfaceflinger。
***
## 2.WMS窗口添加过程及Surfacefling的创建
在Activity启动过程中，ActivityThread的handleResumeActivity()方法中，调用WindowMangerde.addView创建ViewRootImpl并将Activity的根View DecorView的父view设置为ViewRootimpl
```java
//ActivityThread.java
final void handleResumeActivity(IBinder token,
            boolean clearHide, boolean isForward, boolean reallyResume) {
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
            wm.addView(decor, l);
        }
    }
}
//WindowManagerGlobal.java
public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow) {
    ...
    ViewRootImpl root;
    ..
    root = new ViewRootImpl(view.getContext(), display);
    ..
    root.setView(view, wparams, panelParentView);
    ...
}
```
```java
//ViewRootImpl.java
/**
 * The top of a view hierarchy, implementing the needed protocol between View
 * and the WindowManager.  This is for the most part an internal implementation
 * detail of {@link WindowManagerGlobal}.
 *
 * {@hide}
 */
public final class ViewRootImpl implements ViewParent,
        View.AttachInfo.Callbacks, ThreadedRenderer.HardwareDrawCallbacks {
    // These can be accessed by any thread, must be protected with a lock.
    // Surface can never be reassigned or cleared (use Surface.clear()).
    final Surface mSurface = new Surface();

    private Choreographer.FrameCallback mRenderProfiler;
    
    public ViewRootImpl(Context context, Display display) {
        mWindowSession = WindowManagerGlobal.getWindowSession();
        mThread = Thread.currentThread();
        mWindow = new W(this);
        mAttachInfo = new View.AttachInfo(mWindowSession, mWindow, display, this, mHandler, this);
    }
}
```

ViewRootImpl源码注释中关于ViewRootImpl的介绍：View层级的最顶端，WindowManager和View之间的接口。  
接下来看下ViewRootImpl的setView方法
```java
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
    ...
    mView = view;
    mAttachInfo.mRootView = view;
    // Schedule the first layout -before- adding to the window
    // manager, to make sure we do the relayout before receiving
    // any other events from the system.
    requestLayout();
    //通过binder ipc调用server端的addToDisplay方法，向wms添加window
    //其中参数mWindow也是binder通信类，用来在server端回调client端的方法
    res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
                            getHostVisibility(), mDisplay.getDisplayId(),
                            mAttachInfo.mContentInsets, mAttachInfo.mStableInsets,
                            mAttachInfo.mOutsets, mInputChannel);
    view.assignParent(this);//设置DecorView的父view为ViewRootImpl
}

static class W extends IWindow.Stub {
}
```
接下来看下通过binder ipc调用server端的addToDisplay方法的实现
```java
//Session.java
@Override
public int addToDisplay(IWindow window, int seq, WindowManager.LayoutParams attrs,
        int viewVisibility, int displayId, Rect outContentInsets, Rect outStableInsets,
        Rect outOutsets, InputChannel outInputChannel) {
    //调用WindowManagerService的addWindow方法
    return mService.addWindow(this, window, seq, attrs, viewVisibility, displayId,
            outContentInsets, outStableInsets, outOutsets, outInputChannel);
}

//WindowManagerService.java
public int addWindow(Session session, IWindow client, int seq,
            WindowManager.LayoutParams attrs, int viewVisibility, int displayId,
            Rect outContentInsets, Rect outStableInsets, Rect outOutsets,
            InputChannel outInputChannel) {
    final int type = attrs.type;
    //根据要添加的window的type类型，检查WindowToken
    ....
    //在WMS中WindowState是Window的抽象
    WindowState win = new WindowState(this, session, client, token,
                    attachedWindow, appOp[0], seq, attrs, viewVisibility, displayContent);
    ...
    if (addToken) {
        mTokenMap.put(attrs.token, token);
    }
    win.attach();
    mWindowMap.put(client.asBinder(), win);
    ...
}

/**
 * A window in the window manager.
 */
 //在WMS眼中，WindowState就代表一个Window
final class WindowState implements WindowManagerPolicy.WindowState {
    void attach() {
        if (WindowManagerService.localLOGV) Slog.v(
            TAG, "Attaching " + this + " token=" + mToken
            + ", list=" + mToken.windows);
        mSession.windowAddedLocked();//调用Seesion方法
    }
}

//Session.jaa
void windowAddedLocked() {
    if (mSurfaceSession == null) {
        if (WindowManagerService.localLOGV) Slog.v(
            TAG_WM, "First window added to " + this + ", creating SurfaceSession");
        mSurfaceSession = new SurfaceSession();//和Surfaceflinger建立连接
        if (SHOW_TRANSACTIONS) Slog.i(
                TAG_WM, "  NEW SURFACE SESSION " + mSurfaceSession);
        mService.mSessions.add(this);
        if (mLastReportedAnimatorScale != mService.getCurrentAnimatorScale()) {
            mService.dispatchNewAnimatorScaleLocked(this);
        }
    }
    mNumWindow++;
}

/**
 * An instance of this class represents a connection to the surface
 * flinger, from which you can create one or more Surface instances that will
 * be composited to the screen.
 * {@hide}
 */
 //一个SurfaceSession实例代表和Surfaceflinger建立连接。一个Surfaceflinger可以混合一个或多个surface到屏幕上。
public final class SurfaceSession {
}
```
**源码分析到这里，我们就清楚Session.addToDisplay方法的作用了，调用此方法后会在WMS中创建一个窗口WindowState，然后和Surfaceflinger建立连接。**
***
## 3.Surface和Surfacelinger建立连接
接下来再回到ViewRootImpl类中。在ViewRootImpl类中有一个relayoutWindow方法，在此方法中调用了Session的relayout方法，其中需要传递Surface参数。那么这个方法会不会是Surface和Surfaceflinger建立联系的方法呢？
带着这个问题我们看下源码
```java
//ViewRootImpl.java
private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility,
            boolean insetsPending) throws RemoteException {
    ....
    int relayoutResult = mWindowSession.relayout(
    mWindow, mSeq, params,
    (int) (mView.getMeasuredWidth() * appScale + 0.5f),
    (int) (mView.getMeasuredHeight() * appScale + 0.5f),
    viewVisibility, insetsPending ? WindowManagerGlobal.RELAYOUT_INSETS_PENDING : 0,
    mWinFrame, mPendingOverscanInsets, mPendingContentInsets, mPendingVisibleInsets,
    mPendingStableInsets, mPendingOutsets, mPendingBackDropFrame, mPendingConfiguration,
    mSurface);
    //mSurface在ViewRootImpl对象创建时被创建。
    ...
}
//Session.java
public int relayout(IWindow window, int seq, WindowManager.LayoutParams attrs,
            int requestedWidth, int requestedHeight, int viewFlags,
            int flags, Rect outFrame, Rect outOverscanInsets, Rect outContentInsets,
            Rect outVisibleInsets, Rect outStableInsets, Rect outsets, Rect outBackdropFrame,
            Configuration outConfig, Surface outSurface) {
    if (false) Slog.d(TAG_WM, ">>>>>> ENTERED relayout from "
            + Binder.getCallingPid());
    int res = mService.relayoutWindow(this, window, seq, attrs,
            requestedWidth, requestedHeight, viewFlags, flags,
            outFrame, outOverscanInsets, outContentInsets, outVisibleInsets,
            outStableInsets, outsets, outBackdropFrame, outConfig, outSurface);
    if (false) Slog.d(TAG_WM, "<<<<<< EXITING relayout to "
            + Binder.getCallingPid());
    return res;
}
//WindowManagerService.java
public int relayoutWindow(Session session, IWindow client, int seq,
            WindowManager.LayoutParams attrs, int requestedWidth,
            int requestedHeight, int viewVisibility, int flags,
            Rect outFrame, Rect outOverscanInsets, Rect outContentInsets,
            Rect outVisibleInsets, Rect outStableInsets, Rect outOutsets, Rect outBackdropFrame,
            Configuration outConfig, Surface outSurface) {//outSurface---->ViewRootImpl中的surface

            WindowState win = windowForClientLocked(session, client, false);
            ...
            result = createSurfaceControl(outSurface, result, win, winAnimator);
}

private int createSurfaceControl(Surface outSurface, int result, WindowState win,
            WindowStateAnimator winAnimator) {
    if (!win.mHasSurface) {
        result |= RELAYOUT_RES_SURFACE_CHANGED;
    }
    //根据Session中SurfaceSession，在WMS中创建一个新的Surface
    WindowSurfaceController surfaceController = winAnimator.createSurfaceLocked();
    if (surfaceController != null) {
        //将outSurface指向WMS创建的surface持有的图像缓冲区域
        //这样Client端就可以操作这块缓冲区域，在这块内存区域绘制。
        //由于在WMS创建Surface的时候已经和Surfaceflinger建立了连接，所以Sufaceflinger可以操作这块内存区域显示在屏幕上
        surfaceController.getSurface(outSurface);
        if (SHOW_TRANSACTIONS) Slog.i(TAG_WM, "  OUT SURFACE " + outSurface + ": copied");
    } else {
        // For some reason there isn't a surface.  Clear the
        // caller's object so they see the same state.
        Slog.w(TAG_WM, "Failed to create surface control for " + win);
        outSurface.release();
    }
    return result;
}

//WindowStateAnimator.java
WindowSurfaceController createSurfaceLocked() {
    ...
    //根据Session中SurfaceSession，在WMS中创建一个新的Surface
    mSurfaceController = new WindowSurfaceController(mSession.mSurfaceSession,
                    attrs.getTitle().toString(),
                    width, height, format, flags, this);
    ...
    mSurfaceController.setPositionAndLayer(mTmpSize.left, mTmpSize.top, layerStack, mAnimLayer);
    return mSurfaceController;
}

//WindowSurfaceController.java
void getSurface(Surface outSurface) {
    outSurface.copyFrom(mSurfaceControl);
}

//Surface.java
/**
* Copy another surface to this one.  This surface now holds a reference
* to the same data as the original surface, and is -not- the owner.
* This is for use by the window manager when returning a window surface
* back from a client, converting it from the representation being managed
* by the window manager to the representation the client uses to draw
* in to it.
* @hide
*/
public void copyFrom(SurfaceControl other) {
    if (other == null) {
        throw new IllegalArgumentException("other must not be null");
    }

    long surfaceControlPtr = other.mNativeObject;
    if (surfaceControlPtr == 0) {
        throw new NullPointerException(
                "SurfaceControl native object is null. Are you using a released SurfaceControl?");
    }
    long newNativeObject = nativeCreateFromSurfaceControl(surfaceControlPtr);

    synchronized (mLock) {
        if (mNativeObject != 0) {
            nativeRelease(mNativeObject);
        }
        setNativeObjectLocked(newNativeObject);
    }
}
```
**通过上面的分析可以知道，在ViewRootImpl中调用relayoutWindow方法后，会将ViewRootImpl中持有的Surface和WMS中的Surfaceflinger关联上。**
***
## Surface在View中的使用
再次回到ViewRootImpl中，我们知道View的绘制始于ViewRootImpl的performDraw()方法。
```java
private void performDraw() {
    ...
    draw(fullRedrawNeeded);
    ...
}

private void draw(boolean fullRedrawNeeded) {
    Surface surface = mSurface;
    if (!surface.isValid()) {
        return;
    }
    ....
    //这里只看没有硬件加速的绘制
    if (!drawSoftware(surface, mAttachInfo, xOffset, yOffset, scalingRequired, dirty)) {
        return;
    }
}

private boolean drawSoftware(Surface surface, AttachInfo attachInfo, int xoff, int yoff,
            boolean scalingRequired, Rect dirty) {
    // Draw with software renderer.
    final Canvas canvas;
    ...
    //以下三行是重点
    canvas = mSurface.lockCanvas(dirty);
    ...
    mView.draw(canvas);
    ...
    surface.unlockCanvasAndPost(canvas);
}
```
下面逐一分析一下
### 1.Surface.lockCanvas
```java
canvas = mSurface.lockCanvas(dirty);
/**
* Gets a {@link Canvas} for drawing into this surface.
*
* After drawing into the provided {@link Canvas}, the caller must
* invoke {@link #unlockCanvasAndPost} to post the new contents to the surface.
*
* @param inOutDirty A rectangle that represents the dirty region that the caller wants
* to redraw.  This function may choose to expand the dirty rectangle if for example
* the surface has been resized or if the previous contents of the surface were
* not available.  The caller must redraw the entire dirty region as represented
* by the contents of the inOutDirty rectangle upon return from this function.
* The caller may also pass <code>null</code> instead, in the case where the
* entire surface should be redrawn.
* @return A canvas for drawing into the surface.
*
* @throws IllegalArgumentException If the inOutDirty rectangle is not valid.
* @throws OutOfResourcesException If the canvas cannot be locked.
*/
//Surface.java
public Canvas lockCanvas(Rect inOutDirty)
            throws Surface.OutOfResourcesException, IllegalArgumentException {
    synchronized (mLock) {
        checkNotReleasedLocked();
        if (mLockedObject != 0) {
            // Ideally, nativeLockCanvas() would throw in this situation and prevent the
            // double-lock, but that won't happen if mNativeObject was updated.  We can't
            // abandon the old mLockedObject because it might still be in use, so instead
            // we just refuse to re-lock the Surface.
            throw new IllegalArgumentException("Surface was already locked");
        }
        mLockedObject = nativeLockCanvas(mNativeObject, mCanvas, inOutDirty);
        return mCanvas;
    }
}
```
```cpp
//android_view_surface.cpp
static jlong nativeLockCanvas(JNIEnv* env, jclass clazz,
        jlong nativeObject, jobject canvasObj, jobject dirtyRectObj) {
    sp<Surface> surface(reinterpret_cast<Surface *>(nativeObject));//得到Native端的surface

    if (!isSurfaceValid(surface)) {
        doThrowIAE(env);
        return 0;
    }

    Rect dirtyRect(Rect::EMPTY_RECT);
    Rect* dirtyRectPtr = NULL;

    if (dirtyRectObj) {//根据传入的重绘区域，设置dirtyRect
        dirtyRect.left   = env->GetIntField(dirtyRectObj, gRectClassInfo.left);
        dirtyRect.top    = env->GetIntField(dirtyRectObj, gRectClassInfo.top);
        dirtyRect.right  = env->GetIntField(dirtyRectObj, gRectClassInfo.right);
        dirtyRect.bottom = env->GetIntField(dirtyRectObj, gRectClassInfo.bottom);
        dirtyRectPtr = &dirtyRect;
    }

    ANativeWindow_Buffer outBuffer;
    status_t err = surface->lock(&outBuffer, dirtyRectPtr);//创建一个buffer，buffer用来保存重绘区域的像素信息
    if (err < 0) {
        const char* const exception = (err == NO_MEMORY) ?
                OutOfResourcesException :
                "java/lang/IllegalArgumentException";
        jniThrowException(env, exception, NULL);
        return 0;
    }


    //创建SKImageInfo
    SkImageInfo info = SkImageInfo::Make(outBuffer.width, outBuffer.height,
                                         convertPixelFormat(outBuffer.format),
                                         outBuffer.format == PIXEL_FORMAT_RGBX_8888 ?
                                         kOpaque_SkAlphaType : kPremul_SkAlphaType);
    //创建SKBitmap，用于绘制
    SkBitmap bitmap;
    ssize_t bpr = outBuffer.stride * bytesPerPixel(outBuffer.format);
    bitmap.setInfo(info, bpr);
    if (outBuffer.width > 0 && outBuffer.height > 0) {
        bitmap.setPixels(outBuffer.bits);
    } else {
        // be safe with an empty bitmap.
        bitmap.setPixels(NULL);
    }

    //创建Native的canvas，并且和SKBitmap绑定
    Canvas* nativeCanvas = GraphicsJNI::getNativeCanvas(env, canvasObj);
    nativeCanvas->setBitmap(bitmap);

    if (dirtyRectPtr) {
        nativeCanvas->clipRect(dirtyRect.left, dirtyRect.top,
                dirtyRect.right, dirtyRect.bottom);
    }

    if (dirtyRectObj) {
        env->SetIntField(dirtyRectObj, gRectClassInfo.left,   dirtyRect.left);
        env->SetIntField(dirtyRectObj, gRectClassInfo.top,    dirtyRect.top);
        env->SetIntField(dirtyRectObj, gRectClassInfo.right,  dirtyRect.right);
        env->SetIntField(dirtyRectObj, gRectClassInfo.bottom, dirtyRect.bottom);
    }

    // Create another reference to the surface and return it.  This reference
    // should be passed to nativeUnlockCanvasAndPost in place of mNativeObject,
    // because the latter could be replaced while the surface is locked.
    sp<Surface> lockedSurface(surface);
    lockedSurface->incStrong(&sRefBaseOwner);
    return (jlong) lockedSurface.get();
}
```
**通过上面的代码可以知道，canvas.lockCanvas方法作用是：根据重绘区域的大小和显示的格式（RGB/ARGB等）创建一个绘制的buffer，并且根据buffer的大小创建一个用于绘制的SKImage。然后创建一个canvas并将SKImage绑定到canvas上**

### 2.View.draw(canvas);
这个流程就比较简单了。把canvas传递到各个View后，在draw方法中调用canvas的api进行绘制。