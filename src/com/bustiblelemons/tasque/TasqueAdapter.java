package com.bustiblelemons.tasque;

import static com.bustiblelemons.tasque.Values.TAG;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bustiblelemons.tasque.Values.Database.Task;
import com.bustiblelemons.tasque.Values.Database.Tasks;

public class TasqueAdapter extends android.widget.BaseAdapter {

	private Cursor data;
	private SparseBooleanArray checked;
	private Context context;
	private boolean showDate;
	private SparseBooleanArray forDeletion;
	private int selected_color;
	private int deselected_Normal;

	public TasqueAdapter(Context context, Cursor data) {
		this.data = data;
		Log.d(TAG, "TasqueAdapter. Size: " + data.getCount());
		this.context = context;
		this.checked = new SparseBooleanArray();
		this.forDeletion = new SparseBooleanArray();
		this.showDate = SettingsUtil.showDate(context);
		this.deselected_Normal = context.getResources().getColor(R.color.abs__background_holo_light);
		this.selected_color = context.getResources().getColor(R.color.list_selected_color);
		this.setChecked();
	}
	
	private void setChecked() {
		while(data.moveToNext()) {
			int state = data.getInt(data.getColumnIndex(Tasks.STATE));
			if (state == Task.State.Completed) {
				checked.put(data.getPosition(), true);
			}
		}
	}

	public void toggle(int position) {
		checked.put(position, !checked.get(position));
	}

	public SparseBooleanArray getChecked() {
		return this.checked;
	}

	public void resetSelections() {
		this.checked = new SparseBooleanArray();
	}

	public void markForDeletion(int position) {
		forDeletion.put(position, !forDeletion.get(position));
	}

	public void resetDeletionSelection() {
		this.forDeletion = new SparseBooleanArray();
	}

	public SparseBooleanArray getSelectedForDeletion() {
		return this.forDeletion;
	}

	public ArrayList<String> getIDsToDelete() {
		ArrayList<String> r = new ArrayList<String>();
		for (int i = 0; i < forDeletion.size(); i++) {
			if (forDeletion.valueAt(i)) {
				data.moveToPosition(forDeletion.keyAt(i));
				String ID = data.getString(data.getColumnIndex(Tasks.ID));
				r.add(ID);
			}
		}
		return r;
	}

	@Override
	public int getCount() {
		return this.data.getCount();
	}

	@Override
	public Object getItem(int arg0) {
		return data.moveToPosition(arg0) ? data : data;
	}

	@Override
	public long getItemId(int position) {
		Cursor item = (Cursor) this.getItem(position);		
		return (long) item.getLong(item.getColumnIndex(Tasks.ID));
	}
	

	public String getTaskName(int position) {
		Cursor item  = (Cursor) this.getItem(position);
		return item.getString(item.getColumnIndex(Tasks.NAME));
	}

	private class ViewHolder {
		CheckedTextView title;
		ProgressBar bar;
		TextView date;

		public void setDate(int d) {
			if (d > 0 && showDate) {
				String dateString = Utility.getSimpleDate(d, context);
				this.date.setVisibility(View.VISIBLE);
				this.date.setText(dateString);
			}
		}

		public void setTitle(String title, boolean checked) {
			this.title.setText(title);
			this.title.setChecked(checked);
		}

		public void setTag(String tag) {
			title.setTag(tag);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.single_tasque_row, null);
			holder = new ViewHolder();
			holder.title = (CheckedTextView) convertView.findViewById(R.id.single_tasque_row_checked_title);
			holder.date = (TextView) convertView.findViewById(R.id.single_tasque_row_date);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		if (data.moveToPosition(position)) {
			holder.setTitle(data.getString(data.getColumnIndex(Tasks.NAME)), checked.get(position));
			holder.setTag(data.getString(data.getColumnIndex(Tasks.ID)));
			holder.setDate(data.getInt(data.getColumnIndex(Tasks.COMPLETION_DATE)));
			Utility.applyFontSize(holder.title);
		}
		if (forDeletion.get(position)) {
			convertView.setBackgroundColor(selected_color);
		} else {
			convertView.setBackgroundColor(deselected_Normal);
		}
		return convertView;
	}

}
