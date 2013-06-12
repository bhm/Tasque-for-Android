package com.bustiblelemons.tasque.main;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
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
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.frontend.Note;
import com.bustiblelemons.tasque.frontend.Task;
import com.bustiblelemons.tasque.main.TasqueGroupFragment.TasqueGroupFragmentListener;
import com.bustiblelemons.tasque.settings.SettingsUtil;
import com.bustiblelemons.tasque.utilities.Utility;
import com.bustiblelemons.tasque.utilities.Values.FragmentArguments;

public class NotesFragment extends SherlockFragment implements OnItemClickListener, OnItemLongClickListener {

	public static final String FRAGMENT_TAG = "notes";
	private boolean INSERT_NOTE = false;
	private boolean EDITING_NOTE = false;
	private boolean DELETING_ENABLED = false;
	private View view;
	private Bundle args;
	private String taskId;
	private String listId;
	private Context context;
	private NotesAdapter adapter;
	private ListView listView;
	private Cursor data;
	private EditText noteInputField;
	private EditText taskNameInputField;

	public interface NotesFragmentListener {
		/**
		 * 
		 * @param listId
		 * @param taskID
		 * @param taskName
		 */
		public void onShowNotesFragment(String listId, String taskID, String taskName);
		public void onNotesFragmentHidden(boolean taskNameChanged);
	}	

	private TasqueGroupFragmentListener tasqueGroupFragmentListener;
	private NotesFragmentListener notesFragmentListener;
	private RightSideFragmentPocketListener rightSideFragmentChange;
	private String taskName;
	private ActionBar abar;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		rightSideFragmentChange = (RightSideFragmentPocketListener) activity;
		notesFragmentListener = (NotesFragmentListener) activity;
		tasqueGroupFragmentListener = (TasqueGroupFragmentListener) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		context = getActivity().getApplicationContext();
		view = inflater.inflate(R.layout.fragment_notes, null);
		taskNameInputField = (EditText) view.findViewById(R.id.fragment_notes_task_name_input_field);
		Utility.applyFontSize(taskNameInputField);
		noteInputField = (EditText) view.findViewById(R.id.fragment_notes_note_input_field);
		Utility.applyFontSize(noteInputField);
		args = getArguments();
		taskName = args.getString(FragmentArguments.TASK_NAME);
		taskNameInputField.setText(taskName);
		listId = args.getString(FragmentArguments.LIST_ID);
		taskId = args.getString(FragmentArguments.ID);
		listView = (ListView) view.findViewById(R.id.fragment_notes_list);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		data = Database.getNotes(context, taskId);
		adapter = new NotesAdapter(context, data);
		listView.setAdapter(adapter);
		setHasOptionsMenu(true);
		setRetainInstance(true);
		Utility.applyFontSize(noteInputField);
		Utility.applyFontSize(taskNameInputField);
		setActionBar();
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onPause() {
		super.onPause();
		String currentTaskName = taskNameInputField.getText().toString();
		boolean taskNameChanged = false;
		if (!taskName.equals(currentTaskName)) {
			taskName = currentTaskName;
			Task.rename(context, listId, taskId, taskName);
			taskNameChanged = true;
		}
		notesFragmentListener.onNotesFragmentHidden(taskNameChanged);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		rightSideFragmentChange.onRemoveRightSideFragment();
		tasqueGroupFragmentListener.setActionBarForInput();
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
			if (SettingsUtil.autoCap(context)) {
				taskNameInputField.setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
				noteInputField.setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
			}
			Utility.toggleVisibiity(noteInputField, View.VISIBLE);
			Utility.toggleVisibiity(listView, View.GONE);
			DELETING_ENABLED = false;
			INSERT_NOTE = true;
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_note_new_ok:
			this.addNote();
			this.refreshData();
		case R.id.menu_note_new_cancel:
			adapter.resetSelections();
			adapter.notifyDataSetChanged();
			Utility.hideKeyboard(noteInputField);
			Utility.toggleVisibiity(noteInputField, View.GONE);
			Utility.toggleVisibiity(listView, View.VISIBLE);
			INSERT_NOTE = false;
			EDITING_NOTE = false;
			DELETING_ENABLED = false;
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_notes_delete_ok:
			ArrayList<String> forDeletion = adapter.getSelected();
			Note.delete(context, taskId, forDeletion);
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

	private void addNote() {
		String body = noteInputField.getText().toString();
		if (EDITING_NOTE) {
			String noteId = noteInputField.getTag(R.id.notes_fragment_note_id).toString();
			String oldBody = noteInputField.getTag(R.id.notes_fragment_note_old_body).toString();
			Note.update(context, noteId, listId, taskId, oldBody, body);
		} else {
			Note.add(context, listId, taskId, body);
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
		Utility.toggleVisibiity(noteInputField, View.VISIBLE);
		Utility.toggleVisibiity(listView, View.GONE);
		String noteBody = adapter.getNoteBody(arg2);
		noteInputField.setText(noteBody);
		String noteId = adapter.getNoteId(arg2);
		noteInputField.setTag(R.id.notes_fragment_note_id, noteId);
		noteInputField.setTag(R.id.notes_fragment_note_old_body, noteBody);
		EDITING_NOTE = true;
		INSERT_NOTE = false;
		getActivity().supportInvalidateOptionsMenu();
		return true;
	}

	/**
	 * 
	 * @return false to propagate further, true to stop it here.
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
		rightSideFragmentChange.onRemoveRightSideFragment();
		return false;
	}

	public void refreshData() {
		data = Database.getNotes(context, taskId);
		adapter = new NotesAdapter(context, data);
		listView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}

	public void setActionBar() {
		abar = getSherlockActivity().getSupportActionBar();
		abar.setDisplayShowCustomEnabled(false);
		abar.setDisplayShowTitleEnabled(true);
		abar.setTitle(R.string.fragment_notes_title);
	}

	public void saveData() {
		if (EDITING_NOTE || INSERT_NOTE) {
			addNote();
		}
	}
}
