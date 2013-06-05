package com.bustiblelemons.tasque.main;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.rtm.RTMBackend;
import com.bustiblelemons.tasque.utilities.Connection;
import com.bustiblelemons.tasque.utilities.Utility;
import com.bustiblelemons.tasque.utilities.Values.Database.Categories;

public class TasqueRTMCategoryAdapter extends TasqueCategoryAdapter {

	private Context context;
	private Cursor data;
	private SparseBooleanArray checked;
	private SparseBooleanArray forDelete;
	private int deselected_color;
	private int selected_color;
	private String defaultCategory;

	public TasqueRTMCategoryAdapter(Context context, Cursor data) {
		super(context, data);
		this.data = data;
		this.context = context;
		this.checked = new SparseBooleanArray();
		this.forDelete = new SparseBooleanArray();
		this.selected_color = context.getResources().getColor(R.color.selected_color_list);
		this.deselected_color = context.getResources().getColor(R.color.abs__background_holo_light);
		this.defaultCategory = SettingsUtil.getDefaultCategoryStringId(context);
		this.seelectDefaultCategory();
	}

	private void seelectDefaultCategory() {
		this.data.moveToPosition(-1);
		while (this.data.moveToNext()) {
			String id = data.getString(data.getColumnIndex(Categories.ID));
			if (id.equals(defaultCategory)) {
				this.checked.put(data.getPosition(), true);
			}
		}
	}

	@Override
	public void toggle(int position) {
		resetSelections();
		this.checked.put(position, !this.checked.get(position));
		SettingsUtil.setDefaultCategoryId(context, (int) this.getItemId(position));
		if (Connection.isUp(context)) {
			RTMBackend.setDefaultListId(context, this.getItemStringId(position));
		}
	}

	public void markForDeletion(int position) {
		forDelete.put(position, !forDelete.get(position));
	}

	public void resetForDeletion() {
		this.forDelete = new SparseBooleanArray();
	}

	public void resetSelections() {
		this.checked = new SparseBooleanArray();
	}

	@Override
	public int getCount() {
		return data.getCount();
	}

	public SparseBooleanArray getChecked() {
		return this.checked;
	}

	public ArrayList<String> getCheckedIDs() {
		ArrayList<String> r = new ArrayList<String>();
		for (int i = 0; i < checked.size(); i++) {
			if (checked.valueAt(i)) {
				data.moveToPosition(checked.keyAt(i));
				String ID = data.getString(data.getColumnIndex(Categories.ID));
				r.add(ID);
			}
		}
		return r;
	}

	public String getName(int position) {
		return data.moveToPosition(position) ? data.getString(data.getColumnIndex(Categories.NAME)) : "";
	}


	@Override
	public Object getItem(int position) {
		return (data.moveToPosition(position)) ? data : data;
	}

	@Override
	public long getItemId(int position) {
		return data.moveToPosition(position) ? data.getLong(data.getColumnIndex(Categories.ID)) : data.getLong(data
				.getColumnIndex(Categories.ID));
	}

	public String getItemStringId(int position) {
		return String.valueOf(this.getItemId(position));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = LayoutInflater.from(context).inflate(R.layout.single_tasque_row, null);
		} else {
			view = convertView;
		}
		if (data.moveToPosition(position)) {
			CheckedTextView title = (CheckedTextView) view.findViewById(R.id.single_tasque_row_checked_title);
			if (forDelete.get(position)) {
				title.setBackgroundColor(selected_color);
			} else {
				title.setBackgroundColor(deselected_color);
			}
			Utility.applyFontSize(title);
			title.setText(data.getString(data.getColumnIndex(Categories.NAME)));
			title.setTag(data.getString(data.getColumnIndex(Categories.ID)));
			title.setChecked(checked.get(position));
		}
		return view;
	}

}
