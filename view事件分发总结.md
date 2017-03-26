## down事件
如果intercept返回true，本次和以后的move、up等事件直接传递到自身的onTouchEvent，不再向下传递。
如果intercept返回false，递归调用子view的intercept方法，如果其中一个子view的intercept返回true，传递到该子view的onTouchEvent方法消费，事件不在向下传递。如果所有的子view的intercept返回false，那么从最后一个view的onTouchEvent开始回掉，直到有子view的onTouchEvent消费掉了该事件。
如果所有的onTouchEvent都没有消费此事件,事件终止，不会再有move和up等事件传递过来。


## 非down事件
如果mFristTarget不存在，事件直接传递到自身的onTouchEvent，不再向下传递。
如果mFristTarget存在，事件会调用自身的intercept方法，然后将事件传递到子View，重复之前的判断。最后事件传递到mFristTaget的onTouchEvent消费掉，不会在向上回掉onTouchEvent方法。并且intercept方法的返回值不会影响事件的传递。


<font color=red>对于View来说，如果给view设置了onTouchListener,View会优先响应onTouchListener中的onTouch事件，如果onTouch返回了true，消费了此事件，就不会传递到View的onTouchEvent中,如果没有消费,事件会传递到View的onTouchEvent中</font>
```java
public boolean dispatchTouchEvent(MotionEvent event) {
    .....
    ListenerInfo li = mListenerInfo;
    if (li != null && li.mOnTouchListener != null
            && (mViewFlags & ENABLED_MASK) == ENABLED
            && li.mOnTouchListener.onTouch(this, event)) {
        result = true;
    }

    if (!result && onTouchEvent(event)) {
        result = true;
    }
    .....
}
```