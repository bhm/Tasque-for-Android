package com.bustiblelemons.tasque;

import static com.bustiblelemons.tasque.Values.TAG;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.bustiblelemons.tasque.CategoriesFragment.OnShowCategoriesFragment;
import com.bustiblelemons.tasque.CompletedTasksFragment.OnShowCompletedTasksFragment;
import com.bustiblelemons.tasque.MultipleFilesChooserFragment.OnMultipleFilesDetected;
import com.bustiblelemons.tasque.Values.FragmentArguments;

public class TasqueGroupFragment extends SherlockFragment implements OnItemLongClickListener, OnItemClickListener,
		OnTouchListener {

	private boolean DELETING_IN_PROGRESS;
	private View view;
	private ListView listView;
	private TasqueAdapter adapter;
	private Bundle args;
	private int categoryID;
	private Cursor data;
	private Context context;
	private EditText inputField;
	private ActionBar abar;

	public interface OnShowNotesFragment {
		public void onShowNotesFragment(Integer taskID, String taskName);
	}

	private OnShowNotesFragment showNotesFragment;

	public interface OnSetDefaultCategory {
		public void setDefaultCategory(int categoryID);
	}

	private OnSetDefaultCategory setDefaultCategoryCallback;

	private OnShowCategoriesFragment onShowCategoriesFragment;
	private OnMultipleFilesDetected multipleFilesDetected;
	private OnShowCompletedTasksFragment showCompletedTasksFragment;
	
	public interface OnRefreshCategory {
		public boolean onRefreshCategory();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		showNotesFragment = (OnShowNotesFragment) activity;
		setDefaultCategoryCallback = (OnSetDefaultCategory) activity;
		onShowCategoriesFragment = (OnShowCategoriesFragment) activity;
		multipleFilesDetected = (OnMultipleFilesDetected) activity;
		showCompletedTasksFragment = (OnShowCompletedTasksFragment) activity;
	}

	public static TasqueGroupFragment newInstance(Entry<Integer, String> category) {
		TasqueGroupFragment f = new TasqueGroupFragment();
		Bundle args = new Bundle();
		String categoryName = "";
		int categoryID = -1;
		categoryName = category.getValue();
		categoryID = category.getKey();
		Log.d(TAG, "newInstance for " + categoryID + " " + categoryName);
		args.putString(FragmentArguments.CATEGORY, categoryName);
		args.putInt(FragmentArguments.ID, categoryID);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity().getApplicationContext();
		setHasOptionsMenu(true);
		abar = getSherlockActivity().getSupportActionBar();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_task_group, null);
		listView = (ListView) view.findViewById(R.id.fragment_task_group_listview);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		args = getArguments();
		if (args != null) {
			categoryID = args.getInt(FragmentArguments.ID);
		}
		this.loadData();
		context = getActivity().getApplicationContext();
		adapter = new TasqueAdapter(context, data);
		listView.setAdapter(adapter);
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		inputField = (EditText) abar.getCustomView().findViewById(R.id.actionbar_input);
	}

	public void loadData() {
		try {
			switch (categoryID) {
			case FragmentArguments.ALL_ID:
				this.data = Database.getAllSelectedTasks(context);
				break;
			default:
				this.data = Database.getTasks(context, categoryID);
				break;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	public void refreshData() {
		this.loadData();
		adapter = new TasqueAdapter(context, data);
		listView.setAdapter(adapter);
	}

	public void addTask(TextView v) {
		String taskName = inputField.getText().toString();
		if (taskName.length() > 0) {
			Database.insertNewTask(context, String.valueOf(categoryID), taskName);
			inputField.setText("");
			this.refreshData();
		}
	}
	
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (event != null) {
			this.addTask(v);
			return true;
		}
		return false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		/**
		 * Needs reworking and loading on the fly. May not be understood by people.
		 */
//		if (Tasque.MORE_THAN_ONE_FILES_AVAILABLE) {
//			inflater.inflate(R.menu.tasque_group_change_database_file, menu);
//		}
		if (SettingsUtil.isDefaultCategory(context, categoryID)) {
			inflater.inflate(R.menu.tasque_group_fragment_default, menu);
		} else {
			inflater.inflate(R.menu.tasque_group_fragment, menu);
		}
		if (DELETING_IN_PROGRESS) {
			menu.clear();
			inflater.inflate(R.menu.fragment_tasque_group_delete_tasks, menu);
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (DELETING_IN_PROGRESS) {
			DELETING_IN_PROGRESS = false;
			getActivity().supportInvalidateOptionsMenu();
			return true;
		}
		return true;
	}
	
	private void startDeleting() {
		abar.setDisplayShowCustomEnabled(false);
		abar.setDisplayShowTitleEnabled(true);
		abar.setTitle(R.string.fragment_task_group_deleting_title);
	}
	
	public void setActionBarForInput() {
		abar = getSherlockActivity().getSupportActionBar();
		inputField = (EditText) abar.getCustomView().findViewById(R.id.actionbar_input);
		abar.setDisplayShowCustomEnabled(true);
		abar.setDisplayShowTitleEnabled(false);
		abar.setTitle(R.string.app_name);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_new_task:
			this.addTask(null);
			this.refreshData();
			return true;
		case R.id.menu_manage_cateogies:
			onShowCategoriesFragment.onShowCategoriesFragment();
			return true;
		case R.id.menu_show_completed:
			showCompletedTasksFragment.showCompletedTasksFragment(categoryID);
			return true;
		case R.id.menu_settings:
			Intent settings = new Intent(context, SettingsActivity.class);
			startActivity(settings);
			return true;
		case R.id.menu_set_default:
			setDefaultCategoryCallback.setDefaultCategory(categoryID);
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_un_set_default:
			setDefaultCategoryCallback.setDefaultCategory(0);
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_change_database_file:
			multipleFilesDetected.onShowMultipleFilesDetected(Utility.getSyncedDatabasePaths(context), false);
			return true;
		case R.id.menu_start_deleting:
			this.startDeleting();
			DELETING_IN_PROGRESS = true;
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_delete_tasks_ok:
			ArrayList<String> tasksToDelete = new ArrayList<String>();
			tasksToDelete = adapter.getIDsToDelete();
			Database.markDeleted(context, tasksToDelete);
		case R.id.menu_delete_tasks_cancel:
			this.refreshData();
			this.setActionBarForInput();
			DELETING_IN_PROGRESS = false;
			getActivity().supportInvalidateOptionsMenu();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if (DELETING_IN_PROGRESS) {
			adapter.markForDeletion(arg2);
		} else {
			adapter.toggle(arg2);
			Database.setMarkTaskDone(context, String.valueOf(adapter.getItemId(arg2)));
			this.refreshData();
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		showNotesFragment.onShowNotesFragment((int) adapter.getItemId(arg2), adapter.getTaskName(arg2));
		return true;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

}
