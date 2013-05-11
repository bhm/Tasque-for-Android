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

	private Cursor notes;
	private Context context;
	private SparseBooleanArray checked;

	public NotesAdapter(Context context, Cursor notes) {
		this.context = context;
		this.notes = notes;
		Log.d(TAG, "Notes: " + notes.getCount() + " " + notes.getColumnCount());
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
	 * @return
	 */
	public ArrayList<String> getSelected() {
		ArrayList<String> r = new ArrayList<String>();
		for (int i=0; i<checked.size(); i++) {
			if (checked.valueAt(i)) {
				notes.moveToPosition(checked.keyAt(i));
				r.add(notes.getString(notes.getColumnIndex(Notes.TEXT)));
			}
		}
		return r;
	}

	public SparseBooleanArray getChecked() {
		return this.checked;
	}

	@Override
	public int getCount() {
		return notes.getCount();
	}

	@Override
	public Object getItem(int position) {
		return notes.moveToPosition(position) ? notes : notes;
	}

	@Override
	public long getItemId(int position) {
		return 0;
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
		if (notes.moveToPosition(position)) {
			String noteText = notes.getString(notes.getColumnIndex(Notes.TEXT));
			holder.note.setChecked(checked.get(position));
			holder.setNote(noteText);
		}
		return convertView;
	}

}