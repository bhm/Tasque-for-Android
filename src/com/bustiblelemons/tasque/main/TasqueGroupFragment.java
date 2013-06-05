package com.bustiblelemons.tasque.main;

import static com.bustiblelemons.tasque.utilities.Values.TAG;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.frontend.Task;
import com.bustiblelemons.tasque.main.CategoriesFragment.OnShowCategoriesFragment;
import com.bustiblelemons.tasque.main.CompletedTasksFragment.OnShowCompletedTasksFragment;
import com.bustiblelemons.tasque.rtm.RTMBackend;
import com.bustiblelemons.tasque.rtm.RTMSyncService.OnRTMRefresh;
import com.bustiblelemons.tasque.utilities.Values.FragmentArguments;

public class TasqueGroupFragment extends SherlockFragment implements OnItemLongClickListener, OnItemClickListener,
		OnTouchListener {

	private boolean DELETING_IN_PROGRESS;
	private View view;
	private ListView listView;
	private TasqueAdapter adapter;
	private Bundle args;
	private int categoryID;
	private String listId;
	private Cursor data;
	private Context context;
	private ActionBar abar;

	public interface OnShowNotesFragment {
		/**
		 * 
		 * @param listId
		 * @param taskID
		 * @param taskName
		 */
		public void onShowNotesFragment(String listId, String taskID, String taskName);
	}

	private OnShowNotesFragment showNotesFragment;

	public interface OnSetDefaultCategory {
		public void setDefaultCategory(int categoryID);
	}

	public interface OnSetActionBarForInput {
		public void setActionBarForInput();
	}

	private OnSetActionBarForInput setActionBarForInput;

	private OnSetDefaultCategory setDefaultCategoryCallback;

	private OnShowCategoriesFragment onShowCategoriesFragment;
	private OnShowCompletedTasksFragment showCompletedTasksFragment;

	public interface OnRefreshCategory {
		public boolean onRefreshCategory();
	}
	
	private OnRTMRefresh rtmRefresh;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		setActionBarForInput = (OnSetActionBarForInput) activity;
		showNotesFragment = (OnShowNotesFragment) activity;
		setDefaultCategoryCallback = (OnSetDefaultCategory) activity;
		onShowCategoriesFragment = (OnShowCategoriesFragment) activity;
		showCompletedTasksFragment = (OnShowCompletedTasksFragment) activity;
		rtmRefresh = (OnRTMRefresh) activity;
	}

	public static TasqueGroupFragment newInstance(Pair<Integer, String> category) {
		TasqueGroupFragment f = new TasqueGroupFragment();
		Bundle args = new Bundle();
		int id = category.first;
		String name = category.second;
		Log.d(TAG, "newInstance for " + id + " " + name);
		args.putString(FragmentArguments.CATEGORY, name);
		args.putInt(FragmentArguments.ID, id);
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
			listId = String.valueOf(categoryID);
			// categoryName = args.getString(FragmentArguments.CATEGORY);
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
	}

	public void loadData() {
		try {
			switch (categoryID) {
			case FragmentArguments.ALL_ID:
				this.data = Database.getTasks(context);
				break;
			default:
				this.data = Database.getTasks(context, listId);
				break;
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	public void refreshData() {
		this.loadData();
		this.adapter.notifyDataSetChanged();
		adapter = new TasqueAdapter(context, data);
		listView.setAdapter(adapter);
	}

	/**
	 * FIXME Going back from the completed fragment prevents adding
	 * 
	 * @param v
	 */
	public boolean addTask(TextView v) {
		String taskName = v.getText().toString();
		if (taskName.length() > 0) {
			Task.add(context, listId, taskName);
			v.setText("");
			this.refreshData();
			return true;
		}
		return false;
	}

	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (event != null) {
			return this.addTask(v);
		}
		return false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		/**
		 * Needs reworking and loading on the fly. May not be understood by
		 * people.
		 */
		// if (Tasque.MORE_THAN_ONE_FILES_AVAILABLE) {
		// inflater.inflate(R.menu.tasque_group_change_database_file, menu);
		// }
		if (SettingsUtil.isDefaultCategory(context, categoryID)) {
			inflater.inflate(R.menu.fragment_tasque_group_default, menu);
		} else {
			inflater.inflate(R.menu.fragment_tasque_group, menu);
		}
		if (RTMBackend.useRTM(context)) {
			inflater.inflate(R.menu.rtm_refresh_option, menu);
		}
		if (DELETING_IN_PROGRESS) {
			menu.clear();
			inflater.inflate(R.menu.fragment_tasque_group_delete_tasks, menu);
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (DELETING_IN_PROGRESS) {
			this.disableDeleting();
			return true;
		}
		return false;
	}

	private void startDeleting() {
		abar.setDisplayShowCustomEnabled(false);
		abar.setDisplayShowTitleEnabled(true);
		abar.setTitle(R.string.fragment_task_group_deleting_title);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_new_task:
			this.addTask(Tasque.getActionBarInput());
			this.refreshData();
			return true;
		case R.id.menu_manage_cateogies:
			onShowCategoriesFragment.onShowCategoriesFragment();
			return true;
		case R.id.menu_show_completed:
			showCompletedTasksFragment.onShowCompletedTasksFragment(listId);
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
		case R.id.menu_rtm_refresh:
			rtmRefresh.startRTMRefreshService(context);
			return true;
		case R.id.menu_change_database_file:
			// multipleFilesDetected.onShowMultipleFilesDetected(Utility.getSyncedDatabasePaths(context),
			// false);
			return true;
		case R.id.menu_start_deleting:
			this.startDeleting();
			DELETING_IN_PROGRESS = true;
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_delete_tasks_ok:
			ArrayList<String> tasksToDelete = adapter.getIDsToDelete();
			Task.delete(context, listId, tasksToDelete);
		case R.id.menu_delete_tasks_cancel:
			this.disableDeleting();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void disableDeleting() {
		this.refreshData();
		setActionBarForInput.setActionBarForInput();
		DELETING_IN_PROGRESS = false;
		getActivity().supportInvalidateOptionsMenu();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if (DELETING_IN_PROGRESS) {
			adapter.markForDeletion(arg2);
		} else {
			adapter.toggle(arg2);
			String taskId = String.valueOf(adapter.getItemId(arg2));
			Task.markDone(context, listId, taskId, adapter.getTaskName(arg2));
			this.refreshData();
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long arg3) {
		showNotesFragment.onShowNotesFragment(listId, adapter.getItemStringId(position), adapter.getTaskName(position));
		return true;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

	public void resetInputField() {
		setActionBarForInput.setActionBarForInput();
	}
}
