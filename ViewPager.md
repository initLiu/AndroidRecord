ViewPager中存在一个mItem集合，默认会缓存当前页的上一个和下一个item。当ViewPager左右拖动时，ViewPager会把超出缓存范围的view从mItem集合中移除，然后进入缓存范围的view加入mItem集合。

在更新ViewPager中item的显示内容后，调用PagerAdapter.notifyDataSetChanged()可以发现，ViewPager的item并没有更新。这是为什么呢？我们从源码来分析
```java
private class PagerObserver extends DataSetObserver {
    PagerObserver() {
    }

    @Override
    public void onChanged() {
        dataSetChanged();
    }
    @Override
    public void onInvalidated() {
        dataSetChanged();
    }
}

void dataSetChanged() {
    // This method only gets called if our observer is attached, so mAdapter is non-null.

    final int adapterCount = mAdapter.getCount();
    mExpectedAdapterCount = adapterCount;
    boolean needPopulate = mItems.size() < mOffscreenPageLimit * 2 + 1
            && mItems.size() < adapterCount;
    int newCurrItem = mCurItem;

    boolean isUpdating = false;
    for (int i = 0; i < mItems.size(); i++) {
        final ItemInfo ii = mItems.get(i);
        //调用PagerAdapter的getItemPosition，根据返回值判断是否需要更新
        final int newPos = mAdapter.getItemPosition(ii.object);

        //如果POSITION_UNCHANGED，不更新
        if (newPos == PagerAdapter.POSITION_UNCHANGED) {
            continue;
        }

        if (newPos == PagerAdapter.POSITION_NONE) {
            mItems.remove(i);
            i--;

            if (!isUpdating) {
                mAdapter.startUpdate(this);
                isUpdating = true;
            }

            //调用PagerAdapter.destroyItem方法
            mAdapter.destroyItem(this, ii.position, ii.object);
            //设置ViewPager更新标志
            needPopulate = true;

            if (mCurItem == ii.position) {
                // Keep the current item in the valid range
                newCurrItem = Math.max(0, Math.min(mCurItem, adapterCount - 1));
                needPopulate = true;
            }
            continue;
        }

        if (ii.position != newPos) {
            if (ii.position == mCurItem) {
                // Our current item changed position. Follow it.
                newCurrItem = newPos;
            }

            ii.position = newPos;
            needPopulate = true;
        }
    }

    if (isUpdating) {
        mAdapter.finishUpdate(this);
    }

    Collections.sort(mItems, COMPARATOR);

    //需要更新，设置当前页的item，调用requestLayout重新布局
    if (needPopulate) {
        // Reset our known page widths; populate will recompute them.
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (!lp.isDecor) {
                lp.widthFactor = 0.f;
            }
        }

        setCurrentItemInternal(newCurrItem, false, true);
        requestLayout();
    }
}
```
从上面可以看到，只有当PagerAdapter.getItemPosition返回值为POSITION_NONE或者返回值和原来的item的position不相等时才会更新ViewPager

解决方案demo
```java
public class MyAdapter extends PagerAdapter {
    private List<String> mData = new ArrayList<>();
    private ArrayMap<Integer, Integer> mDataHashKeyMap = new ArrayMap<>();
    private List<Integer> mChangeList = new ArrayList<>();

    public void updateData(int index, String data) {
        mData.set(index, data);
        mChangeList.add(index);
        notifyDataSetChanged();
    }

    public void upddatData(List<String> datas) {
        mData = datas;
        int len = mData.size();
        for (int i = 0; i < len; i++) {
            mChangeList.add(i);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        TextView textView = new TextView(container.getContext());
        textView.setText(mData.get(position));
        container.addView(textView);
        mDataHashKeyMap.put(textView.hashCode(), position);
        return textView;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public int getItemPosition(Object object) {
        Integer pos = mDataHashKeyMap.get(object.hashCode());
        if (pos != null) {
            if (mChangeList.contains(pos)) {
                return POSITION_NONE;
            }
        }
        return POSITION_UNCHANGED;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        mDataHashKeyMap.remove(object.hashCode());
        container.removeView((TextView) object);
    }
}
```