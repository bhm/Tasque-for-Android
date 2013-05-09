package com.bustiblelemons.tasque;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import com.bustiblelemons.tasque.Values.Database.Categories;

public class TasqueCategoryAdapter extends BaseAdapter {

	private Context context;
	private Cursor data;
	private SparseBooleanArray checked;
	private SparseBooleanArray forDelete;
	private ArrayList<String> selected;
	private int deselected_color;
	private int selected_color;

	public TasqueCategoryAdapter(Context context, Cursor data) {
		this.data = data;
		this.context = context;
		this.checked = new SparseBooleanArray();
		this.forDelete = new SparseBooleanArray();
		this.selected = SettingsUtil.getSelectedCategories(context);
		this.selected_color = context.getResources().getColor(R.color.selected_color_list);
		this.deselected_color = context.getResources().getColor(R.color.abs__background_holo_light);
		while (data.moveToNext()) {
			String id = data.getString(data.getColumnIndex(Categories.ID));
			if (selected.contains(id)) {
				this.checked.put(data.getPosition(), true);
			}
		}
	}

	public ArrayList<String> getCheckedToDelete() {
		ArrayList<String> r = new ArrayList<String>();
		for (int i = 0; i < forDelete.size(); i++) {
			if (forDelete.valueAt(i)) {
				data.moveToPosition(forDelete.keyAt(i));
				r.add(data.getString(data.getColumnIndex(Categories.ID)));
			}
		}
		return r;
	}
	
	public void markForDeletion(int position) {
		forDelete.put(position, !forDelete.get(position));
	}
	
	public void resetForDeletion() {
		this.forDelete = new SparseBooleanArray();
	}

	public void toggle(int pos) {
		this.checked.put(pos, !this.checked.get(pos));
	}

	public void resetSelections() {
		this.checked = new SparseBooleanArray();
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

	@Override
	public int getCount() {
		return data.getCount();
	}

	@Override
	public Object getItem(int position) {
		return (data.moveToPosition(position)) ? data : data;
	}

	@Override
	public long getItemId(int position) {
		return 0;
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
