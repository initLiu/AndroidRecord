package com.neusoft.fish.app;

import java.util.ArrayList;
import java.util.List;

import com.neusoft.fish.observer.BusinessObserver;
import com.neusoft.fish.persist.FishDatabese;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class BaseApplication extends Application {

	private static final String SPNAME = "splash";
	private static final String SPKEY = "first";

	public static BaseApplication mApplication;
	private FishDatabese fishDb;
	private List<BusinessObserver> uiObservers = new ArrayList<BusinessObserver>();

	@Override
	public void onCreate() {
		super.onCreate();
		mApplication = this;

	}

	public FishDatabese getFishDatabese() {
		if (fishDb == null) {
			fishDb = new FishDatabese(getApplicationContext());
		}
		return fishDb;
	}

	public void closeFishDb() {
		if (fishDb != null) {
			fishDb.close();
		}
	}

	public boolean shouldShowSplash() {
		SharedPreferences sp = getSharedPreferences(SPNAME,
				Context.MODE_PRIVATE);
		return sp.getBoolean(SPKEY, false);
	}

	public boolean setShowSplash(boolean show) {
		SharedPreferences sp = getSharedPreferences(SPNAME,
				Context.MODE_PRIVATE);
		return sp.edit().putBoolean(SPKEY, show).commit();
	}

	public void addObserver(BusinessObserver observer) {
		uiObservers.add(observer);
	}

	public void removeObserver(BusinessObserver observer) {
		uiObservers.remove(observer);
	}

	public void notifyObservers(Object data) {
		for (BusinessObserver observer : uiObservers) {
			observer.update(data);
		}
	}
}
