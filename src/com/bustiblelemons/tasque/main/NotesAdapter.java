package com.bustiblelemons.tasque.main;

import static com.bustiblelemons.tasque.utilities.Values.TAG;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;

import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.utilities.Utility;
import com.bustiblelemons.tasque.utilities.Values.Database.Notes;

public class NotesAdapter extends BaseAdapter {

	private Cursor data;
	private Context context;
	private SparseBooleanArray checked;

	public NotesAdapter(Context context, Cursor data) {
		this.context = context;
		this.data = data;
		Log.d(TAG, "Notes: " + data.getColumnCount() + " " + data.getCount());
		this.checked = new SparseBooleanArray();
	}

	public void toggle(int pos) {
		checked.put(pos, !checked.get(pos));
	}

	public void resetSelections() {
		this.checked = new SparseBooleanArray();
	}

	public boolean hasChecked() {
		for (int i = 0; i < checked.size(); i++) {
			if (checked.valueAt(i)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all selected data.
	 * 
	 * @return
	 */
	public ArrayList<String> getSelected() {
		ArrayList<String> r = new ArrayList<String>();
		for (int i = 0; i < checked.size(); i++) {
			if (checked.valueAt(i)) {
				data.moveToPosition(checked.keyAt(i));
				r.add(data.getString(data.getColumnIndex(Notes.ID)));
			}
		}
		return r;
	}

	public SparseBooleanArray getChecked() {
		return this.checked;
	}

	@Override
	public int getCount() {
		return data.getCount();
	}

	@Override
	public Object getItem(int position) {
		return data.moveToPosition(position) ? data : data;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	public String getNoteId(int position) {
		return data.moveToPosition(position) ? data.getString(data.getColumnIndex(Notes.ID)) : "";
	}

	public String getNoteBody(int position) {
		return data.moveToPosition(position) ? data.getString(data.getColumnIndex(Notes.TEXT)) : "";
	}

	private class ViewHolder {
		CheckedTextView note;
		EditText input;

		public void setNote(String noteText) {
			Utility.applyFontSize(note);
			Utility.applyFontSize(input);
			note.setText(noteText);
			input.setText(noteText);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.single_note_filed, null);
			holder = new ViewHolder();
			holder.note = (CheckedTextView) convertView.findViewById(R.id.fragment_notes_note);
			holder.input = (EditText) convertView.findViewById(R.id.fragment_notes_input);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		if (data.moveToPosition(position)) {
			String noteText = data.getString(data.getColumnIndex(Notes.TEXT));
			holder.note.setChecked(checked.get(position));
			holder.setNote(noteText);
		}
		return convertView;
	}
}