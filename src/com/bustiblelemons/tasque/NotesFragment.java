package com.bustiblelemons.tasque;

import static com.bustiblelemons.tasque.Values.TAG;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bustiblelemons.tasque.Values.Database.Notes;
import com.bustiblelemons.tasque.Values.FragmentArguments;

public class NotesFragment extends SherlockFragment implements OnItemClickListener, OnItemLongClickListener {

	private static boolean INSERT_NOTE = false;
	private static boolean EDITING_NOTE = false;
	private static boolean DELETING_ENABLED;
	private View view;
	private Bundle args;
	private int taskID;
	private Context context;
	private NotesAdapter adapter;
	private ListView listView;
	private Cursor notes;
	private EditText noteInputField;
	private ActionBar abar;
	private EditText taskNameInputField;

	public interface OnNotesFragmentHidden {
		public void onNotesFragmentHidden(boolean taskNameChanged);
	}

	private OnNotesFragmentHidden notesFragmentHidden;
	private String taskName;
	private String oldNote;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnNotesFragmentHidden) {
			notesFragmentHidden = (OnNotesFragmentHidden) activity;
		} else {
			throw new ClassCastException(activity.getClass().getSimpleName() + " should implement "
					+ OnNotesFragmentHidden.class.getSimpleName());
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		context = getActivity().getApplicationContext();
		abar = getSherlockActivity().getSupportActionBar();
		abar.setDisplayShowCustomEnabled(false);
		abar.setDisplayShowTitleEnabled(true);
		abar.setTitle(R.string.fragment_notes_title);
		view = inflater.inflate(R.layout.fragment_notes, null);
		taskNameInputField = (EditText) view.findViewById(R.id.fragment_notes_task_name_input_field);
		Utility.applyFontSize(taskNameInputField);
		noteInputField = (EditText) view.findViewById(R.id.fragment_notes_note_input_field);
		Utility.applyFontSize(noteInputField);
		args = getArguments();
		taskName = args.getString(FragmentArguments.TASK_NAME);
		taskNameInputField.setText(taskName);
		taskID = args.getInt(FragmentArguments.ID);
		listView = (ListView) view.findViewById(R.id.fragment_notes_list);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		notes = Database.getNotes(context, String.valueOf(String.valueOf(taskID)));
		adapter = new NotesAdapter(context, notes);
		listView.setAdapter(adapter);
		setHasOptionsMenu(true);
		return view;
	}

	@Override
	public void onPause() {
		super.onPause();
		String currentTaskName = taskNameInputField.getText().toString();
		boolean taskNameChanged = false;
		if (!taskName.equals(currentTaskName)) {
			taskName = currentTaskName;
			Database.updateTask(context, String.valueOf(taskID), taskName);
			taskNameChanged = true;
		}
		notesFragmentHidden.onNotesFragmentHidden(taskNameChanged);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		if (INSERT_NOTE || EDITING_NOTE) {
			inflater.inflate(R.menu.fragment_notes_new_note, menu);
		} else if (DELETING_ENABLED) {
			inflater.inflate(R.menu.fragment_notes_delete, menu);
		} else {
			inflater.inflate(R.menu.fragment_notes, menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_notes_add:
			noteInputField.setText("");
			Utility.toggleVisibiity(noteInputField, View.VISIBLE);
			Utility.toggleVisibiity(listView, View.GONE);
			DELETING_ENABLED = false;
			INSERT_NOTE = true;
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_note_new_ok:
			String body = noteInputField.getText().toString();
			Log.d(TAG, "Note\nID: " + String.valueOf(taskID) + "\nBody: " + body);
			if (EDITING_NOTE) {
				Database.updateTaskNote(context, String.valueOf(taskID), oldNote, body);
			} else if (INSERT_NOTE) {
				Database.insertNote(context, String.valueOf(taskID), body);
			}
			this.refreshData();
		case R.id.menu_note_new_cancel:
			adapter.resetSelections();
			adapter.notifyDataSetChanged();
			Utility.toggleVisibiity(noteInputField, View.GONE);
			Utility.toggleVisibiity(listView, View.VISIBLE);
			noteInputField.clearFocus();
			taskNameInputField.clearFocus();
			Utility.hideKeyboard(noteInputField);
			INSERT_NOTE = false;
			EDITING_NOTE = false;
			DELETING_ENABLED = false;
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_notes_delete_ok:
			ArrayList<String> forDeletion = ((NotesAdapter) listView.getAdapter()).getSelected();
			Database.deleteNotes(context, String.valueOf(taskID), forDeletion);
			this.refreshData();
		case R.id.menu_notes_delete_cancel:
			INSERT_NOTE = false;
			DELETING_ENABLED = false;
			adapter.resetSelections();
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_notes_ok:
			getSherlockActivity().getSupportFragmentManager().popBackStack();
			return true;
		case R.id.menu_settings:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		NotesAdapter adapter = ((NotesAdapter) arg0.getAdapter());
		adapter.toggle(arg2);
		INSERT_NOTE = false;
		if (adapter.hasChecked()) {
			DELETING_ENABLED = true;
		} else {
			DELETING_ENABLED = false;
		}
		getActivity().supportInvalidateOptionsMenu();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Cursor data = (Cursor) arg0.getAdapter().getItem(arg2);
		Utility.toggleVisibiity(noteInputField, View.VISIBLE);
		Utility.toggleVisibiity(listView, View.GONE);
		oldNote = data.getString(data.getColumnIndex(Notes.TEXT));
		noteInputField.setText(oldNote);
		EDITING_NOTE = true;
		INSERT_NOTE = false;
		getActivity().supportInvalidateOptionsMenu();
		return true;
	}

	/**
	 * 
	 * @return true to propagate further, false to stop it.
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (EDITING_NOTE || INSERT_NOTE) {
			EDITING_NOTE = false;
			INSERT_NOTE = false;
			noteInputField.setText("");
			Utility.toggleVisibiity(noteInputField, View.GONE);
			Utility.toggleVisibiity(listView, View.VISIBLE);
			getActivity().supportInvalidateOptionsMenu();
			return true;
		} else if (DELETING_ENABLED) {
			this.refreshData();
			DELETING_ENABLED = false;
			getActivity().supportInvalidateOptionsMenu();
			return true;
		}
		return false;
	}

	private void refreshData() {
		notes = Database.getNotes(context, String.valueOf(taskID));
		adapter = new NotesAdapter(context, notes);
		listView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}
}
