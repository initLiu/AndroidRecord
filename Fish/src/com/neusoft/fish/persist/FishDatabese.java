package com.neusoft.fish.persist;

import com.neusoft.fish.note.NoteItem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class FishDatabese {
	private SQLiteDatabase wdb;
	private SQLiteDatabase rdb;
	private FishSQLiteOpenHelper dbHelper;

	public FishDatabese(Context context) {
		dbHelper = new FishSQLiteOpenHelper(context, "fish.db", null, 1);
	}

	protected synchronized SQLiteDatabase getWriteableDatebase() {
		if (wdb == null) {
			wdb = dbHelper.getWritableDatabase();
		}
		return wdb;
	}

	protected synchronized SQLiteDatabase getReadableDatebase() {
		if (rdb == null) {
			rdb = dbHelper.getWritableDatabase();
		}
		return rdb;
	}

	public void close() {
		if (wdb != null) {
			wdb.close();
		}
		if (rdb != null) {
			rdb.close();
		}
		if (dbHelper != null) {
			dbHelper.close();
		}
	}

	public void insert(String table, ContentValues values, CallBack callBack) {
		getWriteableDatebase().insert(table, null, values);
		callBack.onFinish(values);
	}

	public Cursor query(String table, String[] columns, String selection,
			String[] selectionArgs, String groupBy, String having,
			String orderBy) {
		return getReadableDatebase().query(table, columns, selection,
				selectionArgs, groupBy, having, orderBy);
	}
}
