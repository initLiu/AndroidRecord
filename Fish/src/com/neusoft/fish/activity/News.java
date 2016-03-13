package com.neusoft.fish.activity;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.neusoft.fish.R;

public class News extends Frame {

	private WebView mWebView;

	@Override
	public View onCreate(Activity context) {
		mActivity = context;
		mContentView = LayoutInflater.from(context).inflate(
				R.layout.frame_news, null);
		initUI();
		return mContentView;
	}

	private void initUI() {
		mWebView = (WebView) findViewById(R.id.newswebview);
		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				if (mActivity instanceof MainActivity) {
					mActivity.showDialog(MainActivity.DIALOG_ID);
				}
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				if (mActivity instanceof MainActivity) {
					mActivity.dismissDialog(MainActivity.DIALOG_ID);
				}
			}

		});
		mWebView.loadUrl("http://news.so.com/ns?q=%E8%A7%82%E8%B5%8F%E9%B1%BC%20%E6%96%B0%E9%97%BB&src=onebox");
		mWebView.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (mWebView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK
						&& event.getAction() == KeyEvent.ACTION_DOWN) {
					mWebView.goBack();
					return true;
				}
				return false;
			}
		});
	}
}
