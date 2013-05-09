package com.bustiblelemons.tasque;

import static com.bustiblelemons.tasque.Values.TAG;

import java.io.File;
import java.util.ArrayList;
import java.util.Map.Entry;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.InputType;
import android.util.Log;
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
import com.bustiblelemons.tasque.CategoriesFragment.OnRefreshCategoriesInPager;
import com.bustiblelemons.tasque.CategoriesFragment.OnShowCategoriesFragment;
import com.bustiblelemons.tasque.CategoriesFragment.OnShowInAllCategoriesChanged;
import com.bustiblelemons.tasque.CompletedTasksFragment.OnShowCompletedTasksFragment;
import com.bustiblelemons.tasque.CompletedTasksFragment.OnTaskMarkedActive;
import com.bustiblelemons.tasque.MultipleFilesChooserFragment.OnMultipleFilesDetected;
import com.bustiblelemons.tasque.MultipleFilesChooserFragment.OnSyncedFileChosen;
import com.bustiblelemons.tasque.NotesFragment.OnNotesFragmentHidden;
import com.bustiblelemons.tasque.TasqueGroupFragment.OnRefreshCategory;
import com.bustiblelemons.tasque.TasqueGroupFragment.OnSetDefaultCategory;
import com.bustiblelemons.tasque.TasqueGroupFragment.OnShowNotesFragment;
import com.bustiblelemons.tasque.Values.FragmentArguments;
import com.bustiblelemons.tasque.Values.FragmentFlags;

public class Tasque extends SherlockFragmentActivity implements OnShowNotesFragment, OnPageChangeListener,
		OnEditorActionListener, OnSetDefaultCategory, OnShowCategoriesFragment, OnSyncedFileChosen,
		OnNotesFragmentHidden, OnRefreshCategoriesInPager, OnShowInAllCategoriesChanged, OnShowCompletedTasksFragment,
		OnTaskMarkedActive, OnRefreshCategory, OnMultipleFilesDetected {

	private static final String PAGER_POSITION = "PAGER_POSITION";
	public static boolean MORE_THAN_ONE_FILES_AVAILABLE = false;
	private NotesFragment notesFragment;
	private CategoriesFragment categoriesFragment;
	private MultipleFilesChooserFragment filesChooserFragment;
	private CompletedTasksFragment completedTasksFragment;
	private FragmentManager fmanager;

	protected static ArrayList<Entry<Integer, String>> categories;
	private Context context;
	private MyPagerAdapter pagerAdapter;
	private ViewPager pager;
	private EditText customInputField;
	private ActionBar abar;
	private RelativeLayout customAbarInputView;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		Log.d(TAG, "Tasque.class onCreate");
		context = getApplicationContext();
		fmanager = getSupportFragmentManager();
		setContentView(R.layout.activity_tasque);
		pager = (ViewPager) findViewById(R.id.pager);
		pager.setOnPageChangeListener(this);
		abar = getSupportActionBar();
		categories = Database.getCategories(context);
		this.setActionBarForInput();
		completedTasksFragment = new CompletedTasksFragment();
	}

	private void load() {
		if (SettingsUtil.isFirstRun(context)) {
			Log.d(TAG, "First Run. Setting categories and preventing further first runs");
			SettingsUtil.setSelectedCategoriesToAll(context);
			SettingsUtil.firstRunDone(context);
		}
		abar.show();
		categories = Database.getCategories(context);
		pagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), categories);
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
	protected void onDestroy() {
		super.onDestroy();
		if (SettingsUtil.getExportOnExit(context)) {
			Utility.pushDatabase(context);
		}
		if (pager != null)
			Utility.hideKeyboard(context, pager.getWindowToken());
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
	public void onShowNotesFragment(Integer taskID, String taskName) {
		notesFragment = new NotesFragment();
		FragmentTransaction transaction = fmanager.beginTransaction();
		Bundle args = new Bundle();
		args.putInt(FragmentArguments.ID, taskID);
		args.putString(FragmentArguments.TASK_NAME, taskName);
		notesFragment.setArguments(args);
		transaction.add(R.id.fragment_pocket, notesFragment, FragmentFlags.NOTES_FRAGMENT);
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
		transaction.add(R.id.fragment_pocket, categoriesFragment, FragmentFlags.CATEGORIES_FRAGMENT);
		transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left,
				R.anim.slide_out_right);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	@Override
	public void showCompletedTasksFragment(int categoryID) {
		completedTasksFragment = new CompletedTasksFragment();
		Bundle args = new Bundle();
		args.putInt(FragmentArguments.ID, categoryID);
		completedTasksFragment.setArguments(args);
		FragmentTransaction transaction = fmanager.beginTransaction();
		transaction.add(R.id.fragment_pocket, completedTasksFragment, FragmentFlags.COMPLETED_TASKS_FRAGMENT);
		transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left,
				R.anim.slide_out_right);
		transaction.addToBackStack(null);
		transaction.commit();

	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (event != null) {
			if (categoriesFragment != null) {
				if (categoriesFragment.isVisible()) {
					return categoriesFragment.onKeyCode(event);
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
	}

	public void goToDefaultList(int categoryId) {
		for (Entry<Integer, String> c : categories) {
			if (c.getKey().equals(categoryId)) {
				int index = categories.indexOf(c);
				pager.setCurrentItem(index);
				customInputField.setHint(c.getValue());
			}
		}
	}

	private void setActionBarForInput() {
		customAbarInputView = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.actionbar_input, null);
		customInputField = (EditText) customAbarInputView.findViewById(R.id.actionbar_input);
		customInputField.setOnEditorActionListener(this);
		customInputField.setHint(categories.get(pager.getCurrentItem()).getValue());
		if (SettingsUtil.autoCap(context)) {
			customInputField.setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		}
		abar.setDisplayShowCustomEnabled(true);
		abar.setDisplayShowTitleEnabled(false);
		abar.setTitle(R.string.app_name);
		abar.setCustomView(customAbarInputView);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (filesChooserFragment != null) {
			if (filesChooserFragment.isVisible()) {
				Intent goHome = new Intent(Intent.ACTION_MAIN);
				goHome.addCategory(Intent.CATEGORY_HOME);
				startActivity(goHome);
				return false;
			}
			return false;
		}
		if (notesFragment != null) {
			if (notesFragment.isVisible()) {
				return notesFragment.onKeyDown(keyCode, event);
			}
			return super.onKeyDown(keyCode, event);
		}
		TasqueGroupFragment f = (TasqueGroupFragment) pagerAdapter.getItem(pager.getCurrentItem());
		if (f.isVisible()) {
			return f.onKeyDown(keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void syncedFileChosen(String filePath) {
		SettingsUtil.setSyncedDatabsePath(context, filePath);
		fmanager.beginTransaction().detach(filesChooserFragment).commit();
		try {
			Utility.copyDatabase(context, filePath);
		} finally {
			this.load();
		}
	}

	public static class MyPagerAdapter extends FragmentStatePagerAdapter {
		private SparseArray<TasqueGroupFragment> fragments;

		public MyPagerAdapter(FragmentManager fm, ArrayList<Entry<Integer, String>> categories) {
			super(fm);
			fragments = new SparseArray<TasqueGroupFragment>();
		}

		@Override
		public int getCount() {
			return Tasque.categories.size();
		}

		public TasqueGroupFragment getFragment(int key) {
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
		String hint = context.getString(R.string.fragment_task_group_intput_field_hint);
		customInputField.setHint(String.format(hint, categories.get(arg0).getValue()));
		if (completedTasksFragment.isVisible()) {
			completedTasksFragment.loadData(categories.get(arg0).getKey());
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
		categories = Database.getCategories(context);
		pagerAdapter.notifyDataSetChanged();
		pager.invalidate();
		pager.setCurrentItem(0);
	}

	@Override
	public void onShowInAllCategoriesChanged() {
		((TasqueGroupFragment) pagerAdapter.getItem(0)).refreshData();
	}

	@Override
	public void ontaskMarkedActive() {
		TasqueGroupFragment f = pagerAdapter.getFragment(pager.getCurrentItem());
		if (f.isVisible()) {
			f.refreshData();
		}
	}

	@Override
	public boolean onRefreshCategory() {
		this.setActionBarForInput();
		TasqueGroupFragment f = (TasqueGroupFragment) pagerAdapter.instantiateItem(pager, pager.getCurrentItem());
		f.refreshData();
		this.setActionBarForInput();
		return true;
	}

	@Override
	public void onShowMultipleFilesDetected(ArrayList<File> files, boolean lostPrevious) {
		if (filesChooserFragment == null) {
			filesChooserFragment = new MultipleFilesChooserFragment();
		}
		Bundle args = new Bundle();
		args.putSerializable(FragmentArguments.MultipleFiles.FILES_FOUND, files);
		args.putBoolean(FragmentArguments.MultipleFiles.LOST_PREVIOUS, lostPrevious);
		filesChooserFragment.setArguments(args);
		FragmentTransaction transaction = fmanager.beginTransaction();
		transaction.add(android.R.id.content, filesChooserFragment, FragmentFlags.MULTIPLEFILES_FRAGMENT);
		transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left,
				R.anim.slide_out_right);
		transaction.addToBackStack(null);
		transaction.commit();
	}
}