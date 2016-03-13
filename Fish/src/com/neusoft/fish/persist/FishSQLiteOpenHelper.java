package com.neusoft.fish.persist;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class FishSQLiteOpenHelper extends SQLiteOpenHelper {

	public FishSQLiteOpenHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "create table " + DBConstants.TABALENAME_USER
				+ "(uid TEXT PRIMARY KEY,password TEXT NOT NULL)";
		db.execSQL(sql);
		sql = "create table "
				+ DBConstants.TABALENAME_NOTE
				+ "(title TEXT NOT NULL,content TEXT NOT NULL,time TEXT NOT NULL)";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS user");
		db.execSQL("DROP TABLE IF EXISTS note");
		onCreate(db);
	}
}
