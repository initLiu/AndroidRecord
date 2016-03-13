package com.neusoft.fish.activity;

import android.app.Activity;
import android.content.Context;
import android.view.View;

public abstract class Frame {
	protected View mContentView;
	protected Activity mActivity;

	public abstract View onCreate(Activity context);

	public void onStart() {
	};

	public void onResume() {
	};

	public void onPause() {
	};

	public void onDestroy() {
	};

	public View findViewById(int id) {
		if (mContentView == null) {
			return null;
		}
		return mContentView.findViewById(id);
	}

}
