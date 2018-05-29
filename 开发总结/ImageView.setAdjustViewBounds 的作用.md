```java
imageView.setAdjustViewBounds(true);
```
是否保值Image的宽高比。
setAdjustViewBounds要配合和MaxWidth、MaxHeight使用。
如果要既要保持Image宽高比又要设置图片的固定大小那么需要设置：
1.imageView.setAdjustViewBounds(true);
2.设置maxWidth、maxHeight
3.设置layoutwidh和layoutheight为wrapcontent

源码详解ImageView.java：
```java
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    .....
    if (mDrawable == null) {
            // If no drawable, its intrinsic size is 0.
            mDrawableWidth = -1;
            mDrawableHeight = -1;
            w = h = 0;
        } else {
            w = mDrawableWidth;
            h = mDrawableHeight;
            if (w <= 0) w = 1;
            if (h <= 0) h = 1;

            // We are supposed to adjust view bounds to match the aspect
            // ratio of our drawable. See if that is possible.
            if (mAdjustViewBounds) {
                //widthSpecMode和heightSpecMode不等于MeasureSpec.EXACTLY时，才重新计算宽高
                resizeWidth = widthSpecMode != MeasureSpec.EXACTLY;
                resizeHeight = heightSpecMode != MeasureSpec.EXACTLY;

                desiredAspect = (float) w / (float) h;
            }
        }

        if (resizeWidth || resizeHeight) {
                /* If we get here, it means we want to resize to match the
                    drawables aspect ratio, and we have the freedom to change at
                    least one dimension.
                */

                // Get the max possible width given our constraints
                widthSize = resolveAdjustedSize(w + pleft + pright, mMaxWidth, widthMeasureSpec);

                // Get the max possible height given our constraints
                heightSize = resolveAdjustedSize(h + ptop + pbottom, mMaxHeight, heightMeasureSpec);

                if (desiredAspect != 0.0f) {
                    // See what our actual aspect ratio is
                    float actualAspect = (float)(widthSize - pleft - pright) /
                                            (heightSize - ptop - pbottom);

                    if (Math.abs(actualAspect - desiredAspect) > 0.0000001) {

                        boolean done = false;

                        // Try adjusting width to be proportional to height
                        if (resizeWidth) {
                            int newWidth = (int)(desiredAspect * (heightSize - ptop - pbottom)) +
                                    pleft + pright;

                            // Allow the width to outgrow its original estimate if height is fixed.
                            if (!resizeHeight && !mAdjustViewBoundsCompat) {
                                widthSize = resolveAdjustedSize(newWidth, mMaxWidth, widthMeasureSpec);
                            }

                            if (newWidth <= widthSize) {
                                widthSize = newWidth;
                                done = true;
                            }
                        }

                        // Try adjusting height to be proportional to width
                        if (!done && resizeHeight) {
                            int newHeight = (int)((widthSize - pleft - pright) / desiredAspect) +
                                    ptop + pbottom;

                            // Allow the height to outgrow its original estimate if width is fixed.
                            if (!resizeWidth && !mAdjustViewBoundsCompat) {
                                heightSize = resolveAdjustedSize(newHeight, mMaxHeight,
                                        heightMeasureSpec);
                            }

                            if (newHeight <= heightSize) {
                                heightSize = newHeight;
                            }
                        }
                    }
                }
            } else {
                /* We are either don't want to preserve the drawables aspect ratio,
                   or we are not allowed to change view dimensions. Just measure in
                   the normal way.
                */
                w += pleft + pright;
                h += ptop + pbottom;

                w = Math.max(w, getSuggestedMinimumWidth());
                h = Math.max(h, getSuggestedMinimumHeight());

                widthSize = resolveSizeAndState(w, widthMeasureSpec, 0);
                heightSize = resolveSizeAndState(h, heightMeasureSpec, 0);
            }

            setMeasuredDimension(widthSize, heightSize);
}
```
