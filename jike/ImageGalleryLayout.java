package com.lzp.testgallery;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Scroller;

import java.util.List;

/**
 * Created by lillian on 2018/5/24.
 */

public class ImageGalleryLayout extends ViewGroup {
    private ImageView mImage1, mImage2;
    private List<Drawable> mDrawables;
    private boolean mFlag = true;//true image1 image2,false image2 image1
    private Scroller mScroller;
    private int mState = 1;//scroll state==0 ,1
    private int mIndex = 0;

    public ImageGalleryLayout(Context context) {
        this(context, null);
    }

    public ImageGalleryLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public ImageGalleryLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new Scroller(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left, top;
        left = top = 0;
        mImage1.layout(left, top, left + getMeasuredWidth(), top + getMeasuredHeight());

        left = 0;
        top = -getMeasuredHeight();
        mImage2.layout(left, top, left + getMeasuredWidth(), top + getMeasuredHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                child.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
            }
        }
    }

    public void startSmooth() {
        if (mState == 1) {
            mState = 0;
            int dy = -getHeight();
            mScroller.startScroll(0, 0, 0, dy, 1000);
            invalidate();
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
            invalidate();
        } else if (mState == 0) {
            mState = 1;
            mImage1.setImageDrawable(getDrawable(mIndex));

            setScrollY(0);
            requestLayout();
            post(new Runnable() {
                @Override
                public void run() {
                    mImage2.setImageDrawable(getDrawable(++mIndex));
                }
            });
        }
    }

    private void addImageView() {
        removeAllViews();

        mImage1 = new ImageView(getContext());
        mImage1.setImageDrawable(getDrawable(mIndex));
//        mImage1.setBackgroundColor(Color.YELLOW);

        mImage2 = new ImageView(getContext());
        mImage2.setImageDrawable(getDrawable(++mIndex));
//        mImage2.setBackgroundColor(Color.RED);

        LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addViewInLayout(mImage1, -1, params, true);
        addViewInLayout(mImage2, -1, params, true);
    }

    public void addDrawables(List<Drawable> drawables) {
        if (drawables == null || drawables.isEmpty()) return;

        resetFlag();
        mDrawables = drawables;
        addImageView();
    }

    private void resetFlag() {
        mState = 1;
        mIndex = 0;
    }

    private Drawable getDrawable(int index) {
        int i = index % mDrawables.size();
        return mDrawables.get(i);
    }

    @Override
    public void addView(View child) {
        throw new RuntimeException("not support method addView");
    }

    @Override
    public void addView(View child, int index) {
        throw new RuntimeException("not support method addView");
    }

    @Override
    public void addView(View child, int width, int height) {
        throw new RuntimeException("not support method addView");
    }

    @Override
    public void addView(View child, LayoutParams params) {
        throw new RuntimeException("not support method addView");
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        throw new RuntimeException("not support method addView");
    }
}
