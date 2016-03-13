package com.neusoft.fish.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.neusoft.fish.R;
import com.neusoft.fish.note.NoteItem;

public class NoteListAdapter extends BaseAdapter {

	private Context mContext;
	private List<NoteItem> noteItems = new ArrayList<NoteItem>();

	public NoteListAdapter(Context context) {
		mContext = context;
	}

	public void setNoteList(ArrayList<NoteItem> items) {
		if (items != null && !items.isEmpty()) {
			noteItems.addAll(items);
			notifyDataSetChanged();
		}
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return noteItems.size();
	}

	@Override
	public Object getItem(int position) {
		return noteItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = LayoutInflater.from(mContext).inflate(
					R.layout.note_list_item, null);
			NoteItmeHolder holder = new NoteItmeHolder();
			view.setTag(holder);
		}
		NoteItmeHolder holder = (NoteItmeHolder) view.getTag();

		holder.timeView = (TextView) view.findViewById(R.id.item_time);
		holder.titleView = (TextView) view.findViewById(R.id.item_title);
		holder.contentView = (TextView) view.findViewById(R.id.item_content);

		NoteItem item = (NoteItem) getItem(position);
		holder.timeView.setText(item.getTime());
		holder.titleView.setText(item.getTitle());
		holder.contentView.setText(item.getContent());

		return view;
	}

	public static class NoteItmeHolder {
		public TextView timeView;
		public TextView titleView;
		public TextView contentView;
	}
}
