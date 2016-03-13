package com.neusoft.fish.activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.ContentValues;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.neusoft.fish.R;
import com.neusoft.fish.app.BaseApplication;
import com.neusoft.fish.note.NoteItem;
import com.neusoft.fish.persist.CallBack;
import com.neusoft.fish.persist.DBConstants;
import com.neusoft.fish.persist.FishDatabese;

public class EditeNoteActivity extends BaseActivity implements OnClickListener,
		CallBack, Callback {

	public static final String KEY_ADDNOTE = "addnote";
	public static final String KEY_TITLE = "title";
	public static final String KEY_TIME = "time";
	public static final String KEY_CONTENT = "content";

	private EditText mTitleView;
	private EditText mContentView;
	private TextView mEditeBtn;
	private TextView mSaveBtn;
	private TextView mTimeView;

	private boolean mAddNote = true;
	private HandlerThread mHandlerThread;
	private Handler mSubHandler;
	private Handler mUIHandler;
	private String mCreatTime;

	public static final int UPDATE_UI = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_editenote);
		mHandlerThread = new HandlerThread("SubThread");
		mHandlerThread.start();
		mSubHandler = new Handler(mHandlerThread.getLooper());
		mUIHandler = new Handler(getMainLooper(), this);

		mAddNote = getIntent().getBooleanExtra(KEY_ADDNOTE, true);
		initUI();
		initDate();
	}

	private void initUI() {
		mTitleView = (EditText) findViewById(R.id.editetitle);
		mContentView = (EditText) findViewById(R.id.editetcontent);
		mEditeBtn = (TextView) findViewById(R.id.editeedite);
		mSaveBtn = (TextView) findViewById(R.id.editesave);
		mTimeView = (TextView) findViewById(R.id.editetime);

		if (mAddNote) {
			mTitleView.setFocusableInTouchMode(true);
			mContentView.setFocusableInTouchMode(true);
			mSaveBtn.setVisibility(View.VISIBLE);
			mEditeBtn.setVisibility(View.GONE);
		} else {
			mTitleView.setFocusable(false);
			mContentView.setFocusable(false);
			mSaveBtn.setVisibility(View.GONE);
			mEditeBtn.setVisibility(View.VISIBLE);
		}
		mEditeBtn.setOnClickListener(this);
		mSaveBtn.setOnClickListener(this);
	}

	private void initDate() {
		if (!mAddNote) {
			String title = getIntent().getStringExtra(KEY_TITLE);
			String content = getIntent().getStringExtra(KEY_CONTENT);
			String time = getIntent().getStringExtra(KEY_TIME);
			mCreatTime = time;
			mTimeView.setText("创建于"+time);
			mTitleView.setText(title);
			mContentView.setText(content);
		}else {
			setCreateTime();
		}
	}

	private void setCreateTime() {
		Calendar now = Calendar.getInstance(Locale.CHINA);

		String format = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.CHINA);
		mCreatTime = sdf.format(new Date());
		mTimeView.setText("创建于" + mCreatTime);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.editeedite:
			mSaveBtn.setVisibility(View.VISIBLE);
			mEditeBtn.setVisibility(View.GONE);
			mTitleView.setFocusableInTouchMode(true);
			mContentView.setFocusableInTouchMode(true);
			break;
		case R.id.editesave:
			mSaveBtn.setVisibility(View.GONE);
			mEditeBtn.setVisibility(View.VISIBLE);
			mTitleView.setFocusable(false);
			mContentView.setFocusable(false);
			doSave();
			break;

		default:
			break;
		}
	}

	private void doSave() {
		mSubHandler.post(new Runnable() {

			@Override
			public void run() {
				FishDatabese fdb = BaseApplication.mApplication
						.getFishDatabese();
				ContentValues values = new ContentValues();
				values.put(DBConstants.Note.title, mTitleView.getText()
						.toString());
				values.put(DBConstants.Note.content, mContentView.getText()
						.toString());
				values.put(DBConstants.Note.time, mCreatTime);
				if (mAddNote) {
					fdb.insert(DBConstants.TABALENAME_NOTE, values,
							EditeNoteActivity.this);
				}else {
					
				}
			}
		});

	}

	@Override
	public void onFinish(Object date) {
		ContentValues values = (ContentValues) date;
		String title = values.getAsString(DBConstants.Note.title);
		String content = values.getAsString(DBConstants.Note.content);
		String time = values.getAsString(DBConstants.Note.time);
		NoteItem item = new NoteItem.Builder().setTitle(title)
				.setContent(content).setTime(time).build();
		Message msg = mUIHandler.obtainMessage(UPDATE_UI);
		msg.obj = item;
		mUIHandler.sendMessage(msg);
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case UPDATE_UI:
			NoteItem item = (NoteItem) msg.obj;
			BaseApplication.mApplication.notifyObservers(item);
			break;

		default:
			break;
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
