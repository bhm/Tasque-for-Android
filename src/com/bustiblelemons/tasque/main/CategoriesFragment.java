package com.bustiblelemons.tasque.main;

import static com.bustiblelemons.tasque.utilities.Values.TAG;

import java.util.ArrayList;

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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.frontend.Category;
import com.bustiblelemons.tasque.main.TasqueGroupFragment.TasqueGroupFragmentListener;
import com.bustiblelemons.tasque.rtm.RTMBackend;
import com.bustiblelemons.tasque.rtm.RTMSyncService.OnRTMRefresh;
import com.bustiblelemons.tasque.settings.SettingsUtil;

public class CategoriesFragment extends SherlockFragment implements OnItemClickListener, OnTouchListener,
		OnItemLongClickListener {

	public static final String FRAGMENT_TAG = "categories";
	private boolean DELETING_ENABLED = false;
	private View view;
	private ListView listView;
	private TasqueCategoryAdapter adapter;
	private Context context;
	private Cursor data;

	public interface CategoriesFragmentListener {
		public void onShowCategoriesFragment();

		public void onShowInAllCategoriesChanged();

		public void onRefreshAllCategories();
	}

	private CategoriesFragmentListener categoriesFragmentListener;
	private TasqueGroupFragmentListener tasqueGroupFragmentListener;
	private RightSideFragmentPocketListener rightSideFragmentChange;
	private boolean useRTM;
	private TextView listHint;
	private boolean EDITING_NAME;
	private OnRTMRefresh rtmRefresh;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		rtmRefresh = (OnRTMRefresh) activity;
		rightSideFragmentChange = (RightSideFragmentPocketListener) activity;
		tasqueGroupFragmentListener = (TasqueGroupFragmentListener) activity;
		categoriesFragmentListener = (CategoriesFragmentListener) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		context = getActivity().getApplicationContext();
		view = inflater.inflate(R.layout.fragment_categories, null);
		listHint = (TextView) view.findViewById(R.id.fragment_categories_list_hint);
		this.useRTM = RTMBackend.useRTM(context);
		listView = (ListView) view.findViewById(R.id.fragment_categories_list);
		listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
		setHasOptionsMenu(true);
		setRetainInstance(true);
		data = Database.getCategories(context);
		if (useRTM) {
			this.adapter = new TasqueRTMCategoryAdapter(context, data);
		} else {
			this.adapter = new TasqueCategoryAdapter(context, data);
		}
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		setActionBar();
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (useRTM) {
			listHint.setText(R.string.fragment_categories_list_rtm_hint);
		} else {
			listHint.setText(R.string.fragment_categories_list_hint);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		SettingsUtil.setSelectedCategories(context, adapter.getCheckedIDs());
	}

	@Override
	public void onDetach() {
		super.onDetach();
		categoriesFragmentListener.onRefreshAllCategories();
		tasqueGroupFragmentListener.setActionBarForInput();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		if (DELETING_ENABLED) {
			inflater.inflate(R.menu.fragment_categories_delete, menu);
		} else if (EDITING_NAME) {
			inflater.inflate(R.menu.fragment_categories_edit_name, menu);
		} else {
			inflater.inflate(R.menu.fragment_categories, menu);
			if (RTMBackend.useRTM(context)) {
				inflater.inflate(R.menu.rtm_refresh_option, menu);
				menu.findItem(R.id.menu_rtm_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_rtm_refresh:
			rtmRefresh.startRTMRefreshService(context, true);
			return true;
		case R.id.menu_categories_add:
			this.addCategory();
			return true;
		case R.id.menu_fragment_categories_edit_name_ok:
			this.addCategory();
		case R.id.menu_fragment_categories_edit_name_cancel:
			DELETING_ENABLED = false;
			EDITING_NAME = false;
			Tasque.getActionBarInput().setText("");
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_delete_categories_start:
			DELETING_ENABLED = true;
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_delete_categories_ok:
			ArrayList<String> categoriesToDelete = adapter.getCheckedToDelete();
			Category.delete(context, categoriesToDelete);
			categoriesFragmentListener.onRefreshAllCategories();
		case R.id.menu_delete_categories_cancel:
			this.disableDeleting();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		TasqueCategoryAdapter a = (TasqueCategoryAdapter) arg0.getAdapter();
		if (DELETING_ENABLED) {
			a.markForDeletion(arg2);
		} else {
			a.toggle(arg2);
			categoriesFragmentListener.onShowInAllCategoriesChanged();
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if (EDITING_NAME) {
			EDITING_NAME = false;
			Tasque.getActionBarInput().setText("");
		} else {
			EDITING_NAME = true;
			Tasque.getActionBarInput().setText(adapter.getName(arg2));
			Tasque.getActionBarInput().setTag(adapter.getItemStringId(arg2));
			getActivity().supportInvalidateOptionsMenu();
		}
		return true;
	}

	private boolean addCategory() {
		return this.addCategory(Tasque.getActionBarInput());
	}

	private boolean addCategory(TextView v) {
		String categoryName = Tasque.getActionBarInput().getText().toString();
		if (categoryName.length() > 0) {
			if (EDITING_NAME) {
				String categoryId = v.getTag().toString();
				Category.rename(context, categoryId, categoryName);

			} else {
				Category.insert(context, categoryName);
			}
			this.refreshCategories();
			v.setText("");
			v.setHint(R.string.fragment_categories_input_hint);
			return true;
		}
		return false;
	}

	public void refreshCategories() {
		Log.d(TAG, "Refreshing Categories in Categories Fragment");
		data = Database.getCategories(context);
		if (useRTM) {
			this.adapter = new TasqueRTMCategoryAdapter(context, data);
		} else {
			this.adapter = new TasqueCategoryAdapter(context, data);
		}
		listView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}

	private void disableDeleting() {
		DELETING_ENABLED = false;
		EDITING_NAME = false;
		getActivity().supportInvalidateOptionsMenu();
		this.refreshCategories();
		adapter.resetForDeletion();
	}

	public boolean onKeyCode(int keyCode, KeyEvent event) {
		if (DELETING_ENABLED) {
			this.disableDeleting();
			return true;
		}
		rightSideFragmentChange.onRemoveRightSideFragment();
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		return this.addCategory(v);
	}

	public void setActionBar() {
		Tasque.getActionBarInput().setHint(R.string.fragment_categories_input_hint);
	}
}
