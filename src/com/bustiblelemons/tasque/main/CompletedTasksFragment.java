package com.bustiblelemons.tasque.main;

import static com.bustiblelemons.tasque.utilities.Values.TAG;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.main.TasqueGroupFragment.OnRefreshCategory;
import com.bustiblelemons.tasque.main.TasqueGroupFragment.OnShowNotesFragment;
import com.bustiblelemons.tasque.utilities.Database;
import com.bustiblelemons.tasque.utilities.Values.FragmentArguments;

public class CompletedTasksFragment extends SherlockFragment implements OnItemClickListener, OnItemLongClickListener,
		OnTouchListener {

	private View view;
	private ListView listView;
	private TasqueAdapter adapter;
	private Context context;
	private Bundle args;
	private Cursor data;
	private int categoryID;
	private boolean DELETING_ENABLED;
	private ActionBar abar;

	public interface OnShowCompletedTasksFragment {
		public void showCompletedTasksFragment(int categoryID);
	}

	public interface OnTaskMarkedActive {
		public void ontaskMarkedActive();
	}

	private OnShowNotesFragment showNotesFragment;

	private OnRefreshCategory refreshCategory;
	private EditText input;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		showNotesFragment = (OnShowNotesFragment) activity;
		refreshCategory = (OnRefreshCategory) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context = getActivity().getApplicationContext();
		this.args = getArguments();
		this.categoryID = args.getInt(FragmentArguments.ID);
		this.data = Database.getCompletedTasks(context, String.valueOf(categoryID));
		abar = getSherlockActivity().getSupportActionBar();
		abar.setDisplayShowCustomEnabled(false);
		abar.setDisplayShowTitleEnabled(true);
		abar.setTitle(context.getString(R.string.fragment_completed_title));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_completed_tasks, null);
		listView = (ListView) view.findViewById(R.id.fragment_completed_tasks_list);
		adapter = new TasqueAdapter(context, data);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		setHasOptionsMenu(true);
		return view;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		refreshCategory.onRefreshCategory();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		if (DELETING_ENABLED) {
			inflater.inflate(R.menu.fragment_tasque_completed_delete, menu);
		} else {
			inflater.inflate(R.menu.fragment_tasque_completed, menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_completed_add_new_task:
			this.addTask();
			refreshCategory.onRefreshCategory();
		case R.id.menu_completed_delete_start:
			abar.setTitle(R.string.fragment_task_group_deleting_title);
			DELETING_ENABLED = true;
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_completed_delete_ok:
			Database.markDeleted(context, adapter.getIDsToDelete());
			data = Database.getCompletedTasks(context, String.valueOf(categoryID));
		case R.id.menu_completed_delete_cancel:
			this.disableDeleting();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void addTask() {
		input = (EditText) abar.getCustomView().findViewById(R.id.actionbar_input);
		String task = input.getText().toString();
		if (task.length() > 0) {
			Database.insertNewTask(context, String.valueOf(categoryID), task);
		}
	}

	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		this.addTask();
		return true;
	}

	private void disableDeleting() {
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		abar.setTitle(context.getString(R.string.fragment_completed_title));
		DELETING_ENABLED = false;
		adapter.resetDeletionSelection();
		this.refreshData();
		getActivity().supportInvalidateOptionsMenu();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (DELETING_ENABLED) {
			this.disableDeleting();
			return true;
		}
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if (DELETING_ENABLED) {
			this.adapter.markForDeletion(arg2);
		} else {
			this.adapter.toggle(arg2);
			Database.markActive(context, String.valueOf(adapter.getItemId(arg2)));
			refreshCategory.onRefreshCategory();
			this.loadData(categoryID);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		showNotesFragment.onShowNotesFragment((int) adapter.getItemId(arg2), adapter.getTaskName(arg2));
		return true;
	}

	public void refreshData() {
		this.data = Database.getCompletedTasks(context, String.valueOf(this.categoryID));
		adapter = new TasqueAdapter(context, data);
		listView.setAdapter(adapter);
	}

	public void loadData(Integer categoryID) {
		data = Database.getCompletedTasks(context, String.valueOf(categoryID));
		adapter = new TasqueAdapter(context, data);
		listView.setAdapter(adapter);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}
}
