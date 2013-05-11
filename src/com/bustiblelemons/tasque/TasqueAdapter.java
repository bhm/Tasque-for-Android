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
	private boolean useColours;
	private SparseBooleanArray forDeletion;
	private int selected_color;
	private int deselected_Normal;
	private int overdue_color;
	private int today_color;

	public TasqueAdapter(Context context, Cursor data) {
		this.data = data;
		Log.d(TAG, "TasqueAdapter. Size: " + data.getCount());
		this.context = context;
		this.checked = new SparseBooleanArray();
		this.forDeletion = new SparseBooleanArray();
		this.showDate = SettingsUtil.showDate(context);
		this.useColours = SettingsUtil.useColours(context);
		this.deselected_Normal = context.getResources().getColor(R.color.abs__background_holo_light);
		this.selected_color = context.getResources().getColor(R.color.list_selected_color);
		this.overdue_color = SettingsUtil.getOverdueColor(context);
		this.today_color = SettingsUtil.getTodayColor(context);
		this.setChecked();
	}

	private void setChecked() {
		while (data.moveToNext()) {
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
		Cursor item = (Cursor) this.getItem(position);
		return item.getString(item.getColumnIndex(Tasks.NAME));
	}

	private class ViewHolder {
		CheckedTextView titleView;
		ProgressBar bar;
		TextView dateView;
		public TextView dueDateView;

		public void setDate(long date) {
			Log.d(TAG, "Date: " + date);
			if (date > Tasks.INDEFINED_DATE) {
				String dateString = Utility.getSimpleDate(date, context);
				this.dateView.setVisibility(View.VISIBLE);
				this.dateView.setText(dateString);
				Utility.applyCompletionDateFontSize(dateView);
			}
		}

		public void setDueDate(long date, long completionDate) {
			if (date > Tasks.INDEFINED_DATE && completionDate > date) {
				String dateString = Utility.getSimpleDate(date, context);
				this.dueDateView.setVisibility(View.VISIBLE);
				this.dueDateView.setText(dateString);
				Utility.applyDueDateFontSize(dueDateView);
				if (useColours) {
					this.setColors(this.dueDateView, date);
				}
			}
		}

		public void setTitle(String title, boolean checked) {
			this.titleView.setText(title);
			this.titleView.setChecked(checked);
		}

		private void setColors(TextView v, long date) {
			if (Utility.isToday(date)) {
				v.setTextColor(today_color);
			} else if (Utility.isOverDue(date)) {
				v.setTextColor(overdue_color);
			}
		}

		public void setTag(String tag) {
			titleView.setTag(tag);
		}

		public void setBackgroundColor(int color) {
			titleView.setBackgroundColor(color);
		}

	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.single_tasque_row, null);
			holder = new ViewHolder();
			holder.titleView = (CheckedTextView) convertView.findViewById(R.id.single_tasque_row_checked_title);
			holder.dateView = (TextView) convertView.findViewById(R.id.single_tasque_row_date);
			holder.dueDateView = (TextView) convertView.findViewById(R.id.single_tasque_row_due_date);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		if (data.moveToPosition(position)) {
			holder.setTitle(data.getString(data.getColumnIndex(Tasks.NAME)), checked.get(position));
			holder.setTag(data.getString(data.getColumnIndex(Tasks.ID)));
			if (showDate) {
				holder.setDate(data.getLong(data.getColumnIndex(Tasks.COMPLETION_DATE)));
				holder.setDueDate(data.getLong(data.getColumnIndex(Tasks.DUE_DATE)),
						data.getInt(data.getColumnIndex(Tasks.COMPLETION_DATE)));
			}
			Utility.applyFontSize(holder.titleView);
		}
		if (forDeletion.get(position)) {
			holder.setBackgroundColor(selected_color);
		} else {
			holder.setBackgroundColor(deselected_Normal);
		}
		return convertView;
	}

}
