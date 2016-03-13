package com.neusoft.fish.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

import com.neusoft.fish.R;

public class MainActivity extends BaseActivity implements TabContentFactory,
		OnTabChangeListener {

	public static final int DIALOG_ID = 111;

	private TabHost mTabHost;
	private View[] mTabViews;
	private Frame mFrame;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mainpage);

		initTabViews();

		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTabHost.addTab(mTabHost.newTabSpec(News.class.getName())
				.setIndicator(mTabViews[0]).setContent(this));
		mTabHost.addTab(mTabHost.newTabSpec(Discuss.class.getName())
				.setIndicator(mTabViews[1]).setContent(this));
		mTabHost.addTab(mTabHost.newTabSpec(Note.class.getName())
				.setIndicator(mTabViews[2]).setContent(this));

		mTabHost.setOnTabChangedListener(this);
		mTabHost.setCurrentTab(0);
	}

	private void initTabViews() {
		if (mTabViews == null) {
			mTabViews = new View[3];
			mTabViews[0] = LayoutInflater.from(this).inflate(R.layout.tab_news,
					null);
			mTabViews[1] = LayoutInflater.from(this).inflate(
					R.layout.tab_discuss, null);
			mTabViews[2] = LayoutInflater.from(this).inflate(R.layout.tab_note,
					null);
		}
	}

	@Override
	public View createTabContent(String tag) {
		View view = null;
		try {
			mFrame = ((Frame) Class.forName(tag).newInstance());
			view = mFrame.onCreate(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return view;
	}

	@Override
	public void onTabChanged(String tabId) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		if (id == DIALOG_ID) {
			dialog = new Dialog(this, R.style.commondialog);
			dialog.setContentView(R.layout.common_dialog_loading);
			dialog.setCanceledOnTouchOutside(false);
			Window window = dialog.getWindow();
			LayoutParams params = window.getAttributes();
			params.width = LayoutParams.MATCH_PARENT;
			params.height = LayoutParams.MATCH_PARENT;
			window.setAttributes(params);
		}
		return dialog;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mFrame != null) {
			mFrame.onDestroy();
		}
	}
}
