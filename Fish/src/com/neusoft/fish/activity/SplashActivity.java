package com.neusoft.fish.activity;

import com.neusoft.fish.R;
import com.neusoft.fish.app.BaseApplication;

import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(null);
		if (BaseApplication.mApplication.shouldShowSplash()) {
			setContentView(R.layout.activity_splash);
			BaseApplication.mApplication.setShowSplash(false);
			startMain();
		} else {
			startMain();
		}
	}

	private void startMain() {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
		finish();
	}
}
