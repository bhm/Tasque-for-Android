package com.bustiblelemons.tasque;

import static com.bustiblelemons.tasque.Values.TAG;

import java.sql.SQLException;
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
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class CategoriesFragment extends SherlockFragment implements OnItemClickListener, OnTouchListener {

	private static boolean DELETING_ENABLED = false;
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

	public interface OnRefreshCategoriesInPager {
		public void onRefreshPagerAdapter();
	}

	public interface OnShowInAllCategoriesChanged {
		public void onShowInAllCategoriesChanged();
	}

	private OnShowInAllCategoriesChanged showInAllCategoriesChanged;

	private OnRefreshCategoriesInPager onRefreshPagerAdapter;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		onRefreshPagerAdapter = (OnRefreshCategoriesInPager) activity;
		showInAllCategoriesChanged = (OnShowInAllCategoriesChanged) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_categories, null);
		listView = (ListView) view.findViewById(R.id.fragment_categories_list);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		setHasOptionsMenu(true);
		context = getActivity().getApplicationContext();
		try {
			data = Database.getCategoriesCursor(context);
			adapter = new TasqueCategoryAdapter(context, data);
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(this);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		abar = getSherlockActivity().getSupportActionBar();
		inputField = (EditText) abar.getCustomView().findViewById(R.id.actionbar_input);
		inputField.setHint(R.string.fragment_categories_input_hint);
		abar.setTitle(R.string.fragment_categories_title);
		return view;
	}

	@Override
	public void onPause() {
		super.onPause();
		SettingsUtil.setSelectedCategories(context, adapter.getCheckedIDs());
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		onRefreshPagerAdapter.onRefreshPagerAdapter();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		if (DELETING_ENABLED) {
			inflater.inflate(R.menu.fragment_categories_delete, menu);
		} else {
			inflater.inflate(R.menu.fragment_categories, menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_categories_add:
			this.addCategory();
			return true;
		case R.id.menu_ok:
			getActivity().getSupportFragmentManager().popBackStack();
			return true;
		case R.id.menu_delete_categories_start:
			DELETING_ENABLED = true;
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_delete_categories_ok:
			ArrayList<String> categoriesToDelete = adapter.getCheckedToDelete();
			Database.deleteCategories(context, categoriesToDelete);
			onRefreshPagerAdapter.onRefreshPagerAdapter();
			this.refreshCategories();
		case R.id.menu_delete_categories_cancel:
			DELETING_ENABLED = false;
			getActivity().supportInvalidateOptionsMenu();
			adapter.resetForDeletion();
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

	private void addCategory() {
		String nCategoryName = inputField.getText().toString();
		if (nCategoryName.length() > 0) {
			int r = (int) Database.createNewCategory(context, nCategoryName);
			switch (r) {
			case 0:
				inputField.setHint(R.string.fragment_categories_insert_failed_hint);
				break;
			default:
				inputField.setText("");
				inputField.setHint(R.string.fragment_categories_input_hint);
				break;
			}
			this.refreshCategories();
		}
	}

	private void refreshCategories() {
		try {
			data = Database.getCategoriesCursor(context);
			this.adapter = new TasqueCategoryAdapter(context, data);
			listView.setAdapter(adapter);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean onKeyCode(KeyEvent event) {
		if (event != null) {
			addCategory();
			return true;
		}
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}
}
