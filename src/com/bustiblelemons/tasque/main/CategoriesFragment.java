package com.bustiblelemons.tasque.main;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.frontend.Category;
import com.bustiblelemons.tasque.main.TasqueGroupFragment.OnSetActionBarForInput;
import com.bustiblelemons.tasque.rtm.RTMBackend;
import com.bustiblelemons.tasque.rtm.RTMSyncService.OnRTMRefresh;

public class CategoriesFragment extends SherlockFragment implements OnItemClickListener, OnTouchListener,
		OnItemLongClickListener {

	public static final String FRAGMENT_TAG = "categories";
	private boolean DELETING_ENABLED = false;
	private View view;
	private ListView listView;
	private TasqueCategoryAdapter adapter;
	private Context context;
	private Cursor data;
	private ActionBar abar;
	private EditText inputField;

	public interface OnShowCategoriesFragment {
		public void onShowCategoriesFragment();
	}

	public interface OnRefreshPagerAdapter {
		public void onRefreshPagerAdapter();
	}

	public interface OnShowInAllCategoriesChanged {
		public void onShowInAllCategoriesChanged();
	}

	private OnShowInAllCategoriesChanged showInAllCategoriesChanged;
	private OnRefreshPagerAdapter refreshPagerAdapter;
	private OnSetActionBarForInput setActionBarForInput;
	private boolean useRTM;
	private TextView listHint;
	private boolean EDITING_NAME;
	private OnRTMRefresh rtmRefresh;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		rtmRefresh = (OnRTMRefresh) activity;
		setActionBarForInput = (OnSetActionBarForInput) activity;
		refreshPagerAdapter = (OnRefreshPagerAdapter) activity;
		showInAllCategoriesChanged = (OnShowInAllCategoriesChanged) activity;
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
		data = Database.getCategories(context);
		if (useRTM) {
			this.adapter = new TasqueRTMCategoryAdapter(context, data);
		} else {
			this.adapter = new TasqueCategoryAdapter(context, data);
		}
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		abar = getSherlockActivity().getSupportActionBar();
		inputField = (EditText) abar.getCustomView().findViewById(R.id.actionbar_input);
		inputField.setHint(R.string.fragment_categories_input_hint);
		abar.setTitle(R.string.fragment_categories_title);
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
		refreshPagerAdapter.onRefreshPagerAdapter();
		setActionBarForInput.setActionBarForInput();
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
			rtmRefresh.startRTMRefreshService(context);
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
			refreshPagerAdapter.onRefreshPagerAdapter();
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
			showInAllCategoriesChanged.onShowInAllCategoriesChanged();
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
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		return this.addCategory(v);
	}
}
