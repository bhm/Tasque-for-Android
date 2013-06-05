package com.bustiblelemons.tasque.main;

import static com.bustiblelemons.tasque.utilities.Values.TAG;

import java.util.ArrayList;
import java.util.HashSet;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.frontend.Alarms;
import com.bustiblelemons.tasque.main.CategoriesFragment.OnRefreshPagerAdapter;
import com.bustiblelemons.tasque.main.CategoriesFragment.OnShowCategoriesFragment;
import com.bustiblelemons.tasque.main.CategoriesFragment.OnShowInAllCategoriesChanged;
import com.bustiblelemons.tasque.main.CompletedTasksFragment.OnShowCompletedTasksFragment;
import com.bustiblelemons.tasque.main.CompletedTasksFragment.OnTaskMarkedActive;
import com.bustiblelemons.tasque.main.NotesFragment.OnNotesFragmentHidden;
import com.bustiblelemons.tasque.main.TasqueGroupFragment.OnRefreshCategory;
import com.bustiblelemons.tasque.main.TasqueGroupFragment.OnSetActionBarForInput;
import com.bustiblelemons.tasque.main.TasqueGroupFragment.OnSetDefaultCategory;
import com.bustiblelemons.tasque.main.TasqueGroupFragment.OnShowNotesFragment;
import com.bustiblelemons.tasque.rtm.RTMBackend;
import com.bustiblelemons.tasque.rtm.RTMSyncService;
import com.bustiblelemons.tasque.rtm.RTMSyncService.OnRTMRefresh;
import com.bustiblelemons.tasque.rtm.RTMConnectivityReceiver;
import com.bustiblelemons.tasque.rtm.SynchronizingFragment;
import com.bustiblelemons.tasque.utilities.Connection;
import com.bustiblelemons.tasque.utilities.Utility;
import com.bustiblelemons.tasque.utilities.Values.FragmentArguments;

public class Tasque extends SherlockFragmentActivity implements OnShowNotesFragment, OnPageChangeListener,
		OnEditorActionListener, OnSetDefaultCategory, OnShowCategoriesFragment, OnNotesFragmentHidden,
		OnRefreshPagerAdapter, OnShowInAllCategoriesChanged, OnShowCompletedTasksFragment, OnTaskMarkedActive,
		OnRefreshCategory, OnSetActionBarForInput, OnRTMRefresh {

	private static final String PAGER_POSITION = "PAGER_POSITION";
	public static boolean MORE_THAN_ONE_FILES_AVAILABLE = false;
	private NotesFragment notesFragment;
	private CategoriesFragment categoriesFragment;
	private CompletedTasksFragment completedTasksFragment;
	private FragmentManager fmanager;

	protected static ArrayList<Pair<Integer, String>> categories;
	private Context context;
	private MyPagerAdapter pagerAdapter;
	private ViewPager pager;
	private ActionBar abar;
	private RTMConnectivityReceiver wifiReceiver;
	private SynchronizationReceiver synchronizationReceiver;
	private SynchronizingFragment synchronizingFragment;
	private static IntentFilter wifiFilter;
	private static EditText customInputField;
	private static RelativeLayout customAbarInputView;
	private Intent service;

	private class SynchronizationReceiver extends BroadcastReceiver {

		@SuppressWarnings("unchecked")
		@Override
		public void onReceive(Context context, Intent intent) {
			int syncStatus = intent.getIntExtra(RTMSyncService.SYNC_STATUS, -1);
			switch (syncStatus) {
			case RTMSyncService.SYNC_BEGIN:
				Log.d(TAG, "\t\t\t\tSynchronization began");
				onShowSynchronizationFragment();
				break;
			case RTMSyncService.SYNC_CATEGORIES_CHANGED:
				Log.d(TAG, "\t\t\t\tRefreshing the adapter");
				refreshPagerAdapter();
				break;
			case RTMSyncService.SYNC_DONE:
				HashSet<String> syncedListsIDs = (HashSet<String>) intent
						.getSerializableExtra(RTMSyncService.SYNCHRONIZED_LISTS);
				for (String id : syncedListsIDs) {
					refreshCategory(id);
				}
				refreshCompletedFragment();
				if (notesFragment != null) {
					if (notesFragment.isVisible()) {
						notesFragment.refreshData();
					}
				}
				Log.d(TAG, "\t\t\t\tSynchronization done");
				break;
			case RTMSyncService.SYNC_DONE_EMPTY:
				Log.d(TAG, "\t\t\t\tEMPTY");
				break;
			case RTMSyncService.SYNC_POSTPONED:
				Log.d(TAG, "\t\t\t\tSynchronization postponed");
				break;
			case RTMSyncService.SERVICE_STOPPED:
				Log.d(TAG, "Service stoppped");
				onDetachSynchronizationFragment();
				break;
			}
		}
	}

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		Log.d(TAG, "Tasque.class onCreate");
		context = getApplicationContext();
		Alarms.cancel(context);
		fmanager = getSupportFragmentManager();
		setContentView(R.layout.activity_tasque);
		pager = (ViewPager) findViewById(R.id.pager);
		pager.setOnPageChangeListener(this);
		abar = getSupportActionBar();
		try {
			categories = Database.getCategoriesList(context);
		} catch (SQLiteException e) {
			this.finish();
		}
		this.setActionBarForInput();
		completedTasksFragment = new CompletedTasksFragment();
	}

	private void load() {
		if (SettingsUtil.isFirstRun(context)) {
			Log.d(TAG, "First Run. Setting categories and preventing further first runs");
			SettingsUtil.setDefaultValues(context, true);
			SettingsUtil.setSelectedCategoriesToAll(context);
			SettingsUtil.firstRunDone(context);
		}
		abar.show();
		categories = Database.getCategoriesList(context);
		pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
		pager.setAdapter(pagerAdapter);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (SettingsUtil.hideKeyboard(context)) {
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		}
		this.load();
		Utility.applyFontSize(customInputField);
		int defCat = SettingsUtil.getDefaultCategoryId(context);
		if (defCat > 0) {
			this.goToDefaultList(defCat);
		}		
	}

	@Override
	protected void onResume() {
		super.onResume();
		wifiReceiver = new RTMConnectivityReceiver();
		wifiFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(wifiReceiver, wifiFilter);
		synchronizationReceiver = new SynchronizationReceiver();
		IntentFilter synchronizationFilter = new IntentFilter(RTMSyncService.INTENT_FILTER);
		registerReceiver(synchronizationReceiver, synchronizationFilter);
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				startRTMRefreshService(context);
				String defaultID = RTMBackend.getDefaultListId(context);
				if (defaultID.length() > 0) {
					SettingsUtil.setDefaultCategoryId(context, Integer.valueOf(defaultID));
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(wifiReceiver);
		unregisterReceiver(synchronizationReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (SettingsUtil.getExportOnExit(context)) {
			Utility.pushDatabase(context);
		}
		if (pager != null)
			Utility.hideKeyboard(context, pager.getWindowToken());

		if (RTMBackend.useRTM(context)) {
			if (SettingsUtil.useRTMUpdateService(context)) {
				Alarms.setUp(context);
			} else {
				Log.d(TAG, "Not setting up the alarms");
			}
		}
	}

	@Override
	protected void onResumeFragments() {
		super.onResumeFragments();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (pager != null) {
			outState.putInt(PAGER_POSITION, pager.getCurrentItem());
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (pager != null) {
			pager.setCurrentItem(savedInstanceState.getInt(PAGER_POSITION));
		}
	}

	@Override
	public void onShowNotesFragment(String listId, String taskID, String taskName) {
		notesFragment = new NotesFragment();
		FragmentTransaction transaction = fmanager.beginTransaction();
		Bundle args = new Bundle();
		args.putString(FragmentArguments.ID, taskID);
		args.putString(FragmentArguments.LIST_ID, listId);
		args.putString(FragmentArguments.TASK_NAME, taskName);
		notesFragment.setArguments(args);
		transaction.add(R.id.fragment_pocket, notesFragment, NotesFragment.FRAGMENT_TAG);
		transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left,
				R.anim.slide_out_right);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	@Override
	public void onShowCategoriesFragment() {
		if (categoriesFragment == null) {
			categoriesFragment = new CategoriesFragment();
		}
		if (categoriesFragment.isHidden()) {
			fmanager.beginTransaction().show(categoriesFragment).commit();
			return;
		}
		FragmentTransaction transaction = fmanager.beginTransaction();
		transaction.add(R.id.fragment_pocket, categoriesFragment, CategoriesFragment.FRAGMENT_TAG);
		transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left,
				R.anim.slide_out_right);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	@Override
	public void onShowCompletedTasksFragment(String categoryId) {
		completedTasksFragment = new CompletedTasksFragment();
		Bundle args = new Bundle();
		args.putString(FragmentArguments.ID, categoryId);
		completedTasksFragment.setArguments(args);
		FragmentTransaction transaction = fmanager.beginTransaction();
		transaction.add(R.id.fragment_pocket, completedTasksFragment, CompletedTasksFragment.FRAGMENT_TAG);
		transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left,
				R.anim.slide_out_right);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	public void onShowSynchronizationFragment() {
		synchronizingFragment = new SynchronizingFragment();
		FragmentTransaction transaction = fmanager.beginTransaction();
		transaction.add(R.id.synchronization_fragment_pocket, synchronizingFragment, SynchronizingFragment.FRAGMENT_TAG);
		transaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down, R.anim.slide_in_up,
				R.anim.slide_out_down);
		transaction.commit();
	}

	public void onDetachSynchronizationFragment() {
		if (synchronizingFragment != null) {
			FragmentTransaction transaction = fmanager.beginTransaction();
			transaction.remove(synchronizingFragment);
			transaction.commit();
		}
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (event != null) {
			Log.d(TAG, "onEditorAction");
			if (categoriesFragment != null) {
				if (categoriesFragment.isVisible()) {
					return categoriesFragment.onEditorAction(v, actionId, event);
				}
			}
			if (completedTasksFragment.isVisible()) {
				return completedTasksFragment.onEditorAction(v, actionId, event);
			}
			return pagerAdapter.getFragment(pager.getCurrentItem()).onEditorAction(v, actionId, event);
		}
		return false;
	}

	@Override
	public void setDefaultCategory(int categoryID) {
		SettingsUtil.setDefaultCategoryId(context, categoryID);
		if (RTMBackend.useRTM(context)) {
			RTMBackend.setDefaultListId(context, String.valueOf(categoryID));
		}
	}

	private void goToDefaultList(int categoryId) {
		for (Pair<Integer, String> p : categories) {
			if (categoryId == p.first) {
				pager.setCurrentItem(categories.indexOf(p));
				customInputField.setHint(p.second);
				return;
			}
		}
	}

	@Override
	public void setActionBarForInput() {
		customAbarInputView = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.actionbar_input, null);
		customInputField = (EditText) customAbarInputView.findViewById(R.id.actionbar_input);
		customInputField.setOnEditorActionListener(this);
		customInputField.setHint(categories.get(pager.getCurrentItem()).second);
		if (SettingsUtil.autoCap(context)) {
			customInputField.setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		}
		Utility.applyFontSize(customInputField);
		abar.setDisplayShowCustomEnabled(true);
		abar.setDisplayShowTitleEnabled(false);
		abar.setTitle(R.string.app_name);
		abar.setCustomView(customAbarInputView);
	}

	public static ViewGroup getActionBarView() {
		return Tasque.customAbarInputView;
	}

	static TextView getActionBarInput() {
		return Tasque.customInputField;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event != null) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (notesFragment != null) {
					if (notesFragment.isVisible()) {
						return notesFragment.onKeyDown(keyCode, event) ? true : super.onKeyDown(keyCode, event);
					}
				}
				if (completedTasksFragment.isVisible()) {
					return completedTasksFragment.onKeyDown(keyCode, event) ? true : super.onKeyDown(keyCode, event);
				}
				if (categoriesFragment != null) {
					if (categoriesFragment.isVisible()) {
						return categoriesFragment.onKeyCode(keyCode, event) ? true : super.onKeyDown(keyCode, event);
					}
				}
				TasqueGroupFragment f = (TasqueGroupFragment) pagerAdapter.getFragment(pager.getCurrentItem());
				if (f.isVisible()) {
					return f.onKeyDown(keyCode, event) ? true : super.onKeyDown(keyCode, event);
				}
				return super.onKeyDown(keyCode, event);
			}
			return super.onKeyDown(keyCode, event);
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	public static class MyPagerAdapter extends FragmentStatePagerAdapter {
		private SparseArray<TasqueGroupFragment> fragments;

		public MyPagerAdapter(FragmentManager fm) {
			super(fm);
			fragments = new SparseArray<TasqueGroupFragment>();
		}

		@Override
		public int getCount() {
			return Tasque.categories.size();
		}

		protected TasqueGroupFragment getFragment(int key) {
			return fragments.get(key);
		}

		@Override
		public Fragment getItem(int position) {
			Log.d(TAG, "getItem(" + position + ")");
			fragments.put(position, TasqueGroupFragment.newInstance(Tasque.categories.get(position)));
			return getFragment(position);

		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			super.destroyItem(container, position, object);
			fragments.remove(position);
		}

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageSelected(int arg0) {
		Tasque.getActionBarInput().setHint(categories.get(arg0).second);
		if (completedTasksFragment.isVisible()) {
			completedTasksFragment.loadData(categories.get(arg0).first);
		}
	}

	@Override
	public void onNotesFragmentHidden(boolean taskNameChanged) {
		this.setActionBarForInput();
		if (taskNameChanged) {
			((TasqueGroupFragment) pagerAdapter.getFragment(pager.getCurrentItem())).refreshData();
		}
	}

	@Override
	public void onRefreshPagerAdapter() {
		this.refreshPagerAdapter();
		if (categoriesFragment != null) {
			if (categoriesFragment.isVisible()) {
				categoriesFragment.refreshCategories();
			}
		}
	}

	private void refreshPagerAdapter() {
		categories = Database.getCategoriesList(context);
		pagerAdapter.notifyDataSetChanged();
		pager.invalidate();
		pager.setCurrentItem(0);
		this.setActionBarForInput();
		this.goToDefaultList(SettingsUtil.getDefaultCategoryId(context));
	}

	@Override
	public void onShowInAllCategoriesChanged() {
		TasqueGroupFragment f = pagerAdapter.getFragment(0);
		if (f != null) {
			if (f.isVisible()) {
				f.refreshData();
			}
		}
	}

	@Override
	public void ontaskMarkedActive() {
		TasqueGroupFragment f = pagerAdapter.getFragment(pager.getCurrentItem());
		if (f != null) {
			if (f.isVisible()) {
				f.refreshData();
			}
		}
	}

	@Override
	public boolean onRefreshCategory() {
		return this.refreshCategory(pager.getCurrentItem());
	}

	private boolean refreshCategory(String categoryId) {
		for (Pair<Integer, String> p : categories) {
			if (p.first.equals(Integer.valueOf(categoryId))) {
				return refreshCategory(categories.indexOf(p));
			}
		}
		return false;
	}

	private boolean refreshCategory(int position) {
		this.setActionBarForInput();
		Log.d(TAG, "Refreshing category at " + position + "\n" + categories.get(position).second);
		TasqueGroupFragment f = (TasqueGroupFragment) pagerAdapter.instantiateItem(pager, position);
		f.refreshData();
		this.setActionBarForInput();
		return true;
	}

	private boolean isRTMUpdateSericeRunning(Context context) {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (RTMSyncService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void startRTMRefreshService(Context context) {
		if (isRTMUpdateSericeRunning(context)) {
			Log.d(TAG, "Service is alread running not starting a new one");
		} else {
			service = new Intent(this, RTMSyncService.class);
			context.startService(service);
		}
	}
	
	private void refreshCompletedFragment() {
		if (completedTasksFragment != null) {
			if (completedTasksFragment.isVisible()) {
				completedTasksFragment.refreshData();
			}
		}
	}

}