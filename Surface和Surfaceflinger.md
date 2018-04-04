# Surface和Surfaceflinger
Surface在源码中定义
/**
 * Handle onto a raw buffer that is being managed by the screen compositor.
 */
public class Surface implements Parcelable {
}
简单翻译一下就是：操作原始图像缓冲区的句柄，而始图像缓冲区是由screen compositor管理。
这个screen compositor其实就是Surfaceflinger。

在Activity启动过程中，ActivityThread的handleResumeActivity()方法中，调用WindowMangerde.addView创建ViewRootImpl并将Activity的根View DecorView的父view设置为ViewRootimpl
```java
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
```