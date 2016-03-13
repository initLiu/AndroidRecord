package com.neusoft.fish.activity;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.neusoft.fish.R;
import com.neusoft.fish.adapter.NoteListAdapter;
import com.neusoft.fish.adapter.NoteListAdapter.NoteItmeHolder;
import com.neusoft.fish.app.BaseApplication;
import com.neusoft.fish.note.NoteItem;
import com.neusoft.fish.observer.BusinessObserver;
import com.neusoft.fish.persist.DBConstants;

public class Note extends Frame implements OnClickListener, Callback {
	private ListView mNoteList;
	private NoteListAdapter mAdapter;
	private TextView mAddNode;
	private Handler mUiHandler;
	public static final int UPDATE_UI = 0;

	@Override
	public View onCreate(Activity context) {
		mActivity = context;
		mUiHandler = new Handler(mActivity.getMainLooper(), this);

		mContentView = LayoutInflater.from(context).inflate(
				R.layout.frame_note, null);
		initUI();
		initData();
		BaseApplication.mApplication.addObserver(noteObserver);
		new QueryDBTask().execute();

		return mContentView;
	}

	private class QueryDBTask extends AsyncTask<Void, Void, Cursor> {

		@Override
		protected Cursor doInBackground(Void... params) {
			return BaseApplication.mApplication.getFishDatabese().query(
					DBConstants.TABALENAME_NOTE, null, null, null, null, null,
					null);
		}

		@Override
		protected void onPostExecute(Cursor result) {
			if (result != null && result.getCount() > 0) {
				ArrayList<NoteItem> items = new ArrayList<NoteItem>();
				result.moveToFirst();
				do {
					String title = result.getString(result
							.getColumnIndex(DBConstants.Note.title));
					String content = result.getString(result
							.getColumnIndex(DBConstants.Note.content));
					String time = result.getString(result
							.getColumnIndex(DBConstants.Note.time));
					items.add(new NoteItem.Builder().setTitle(title)
							.setContent(content).setTime(time).build());
				} while (result.moveToNext());
				Message msg = mUiHandler.obtainMessage(Note.UPDATE_UI);
				msg.obj = items;
				mUiHandler.sendMessage(msg);
			}
		}
	}

	private BusinessObserver noteObserver = new BusinessObserver() {

		@Override
		public void update(Object data) {
			if (data instanceof NoteItem) {
				NoteItem item = (NoteItem) data;
				ArrayList<NoteItem> items = new ArrayList<NoteItem>();
				items.add(item);
				updateNoteList(items);
			}
		}
	};

	private void initUI() {
		mNoteList = (ListView) findViewById(R.id.notelist);
		mAddNode = (TextView) findViewById(R.id.noteadd);
		mAddNode.setOnClickListener(this);
		mNoteList.setOnItemClickListener(itemOncliceListener);
	}

	private void initData() {
		mAdapter = new NoteListAdapter(mActivity);
		mNoteList.setAdapter(mAdapter);
	}

	private void updateNoteList(ArrayList<NoteItem> items) {
		mAdapter.setNoteList(items);
	}

	private OnItemClickListener itemOncliceListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (view.getTag() instanceof NoteItmeHolder) {
				NoteItmeHolder holder = (NoteItmeHolder) view.getTag();
				String title = holder.titleView.getText().toString();
				String content = holder.contentView.getText().toString();
				String time = holder.timeView.getText().toString();

				Intent intent = new Intent(mActivity, EditeNoteActivity.class);
				intent.putExtra(EditeNoteActivity.KEY_ADDNOTE, false);
				intent.putExtra(EditeNoteActivity.KEY_TITLE, title);
				intent.putExtra(EditeNoteActivity.KEY_TIME, time);
				intent.putExtra(EditeNoteActivity.KEY_CONTENT, content);
				mActivity.startActivity(intent);
			}
		}
	};

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.noteadd) {// 添加note
			Intent intent = new Intent(mActivity, EditeNoteActivity.class);
			intent.putExtra(EditeNoteActivity.KEY_ADDNOTE, true);
			mActivity.startActivity(intent);
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		if (msg.what == UPDATE_UI) {
			updateNoteList((ArrayList<NoteItem>) msg.obj);
		}
		return false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		BaseApplication.mApplication.removeObserver(noteObserver);
	}
}
