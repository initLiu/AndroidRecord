Scroll类是滚动的一个封装类，可以实现View的平滑滚动效果。
典型用法   
`

	private void smoothScrollBy(int dx, int dy) {
		mScroller.startScroll(getScrollX(), 0, dx, dy, 500);
		invalidate();
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
		}
	}

`
通过`Scroll.startScroll(int startX, int startY, int dx, int dy, int duration)`设定滚动的开始位置以及滚动的距离和完成滚动需要的时间。进入到Scroll的源码发现，其实这个方法什么也没有做只是对设定的值就行了保存，源码如下：
`

    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        mMode = SCROLL_MODE;//设定当前的模式为SCROLL_MODE
        mFinished = false;
        mDuration = duration;
        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        mStartX = startX;
        mStartY = startY;
        mFinalX = startX + dx;
        mFinalY = startY + dy;
        mDeltaX = dx;
        mDeltaY = dy;
        mDurationReciprocal = 1.0f / (float) mDuration;
    }

`
可以看出源码设定了当前的模式为SCROLL_MODE，保存了滚动开始的位置mStartX,mStartY，以及滚动结束的位置mFinalX,mFinalY，滚动的时间mDuration，滚动的距离mDeltaX,mDeltaY。
之后调用invalidate()方法，此方法会触发View的draw()方法，在draw()方法中会调用computeScroll()方法。mScroller.computeScrollOffset()用来判断滚动有没有结束。如果没有结束，computeScrollOffset()方法根据当前时间和滚动时间计算出现在的位置mCurrX ,mCurrY。然后sscrollTo(mScroller.getCurrX(), mScroller.getCurrY())将View移动到(mCurrX,mCurrY)位置处。调用postInvalidate()方法通知view重新绘制，在view的draw()方法中又会调用computeScrollOffset()方法如此反复知道滚动结束，源码如下：
`

    /**
     * Call this when you want to know the new location.  If it returns true,
     * the animation is not yet finished.
     */ 
    public boolean computeScrollOffset() {
        if (mFinished) {//滚动结束，返回false
            return false;
        }

        int timePassed = (int)(AnimationUtils.currentAnimationTimeMillis() - mStartTime);//计算已经滚动了多长时间
    
        if (timePassed < mDuration) {//小于滚动持续时间
            switch (mMode) {
            case SCROLL_MODE://在startScroll时设置的mode
                final float x = mInterpolator.getInterpolation(timePassed * mDurationReciprocal);
                mCurrX = mStartX + Math.round(x * mDeltaX);//根据滚动的时间计算当前应该移动到什么位置
                mCurrY = mStartY + Math.round(x * mDeltaY);
                break;
            case FLING_MODE:
             ........
            }
        }
        else {
            mCurrX = mFinalX;
            mCurrY = mFinalY;
            mFinished = true;
        }
        return true;
    }
`

view.getScroll()方法解释：
此方法取到的是view的左边框的x坐标
