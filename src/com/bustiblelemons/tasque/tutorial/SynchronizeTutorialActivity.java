package com.bustiblelemons.tasque.tutorial;

import static com.bustiblelemons.tasque.utilities.Values.TAG;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.tutorial.OSChooserFragment.OSChooserFragmentListener;

/**
 * Created 10 Jun 2013
 */
public class SynchronizeTutorialActivity extends SherlockFragmentActivity implements OnTouchListener,
		OSChooserFragmentListener {
	ViewPager viewPager;
	TabsAdapter tabAdapter;
	private ActionBar abar;
	private FragmentManager fmanager;
	private OSChooserFragment chooserFragment;
	private Context context;
	private static String TAB_POSITION = "tab_position";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_synchronize_files_tutorial);
		context = getApplicationContext();
		abar = getSupportActionBar();
		abar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		abar.setDisplayShowTitleEnabled(false);
		abar.setTitle(R.string.activity_tutorial_title);

		viewPager = (ViewPager) findViewById(R.id.activity_synchronize_tutorial_pager);

		if (savedInstanceState != null) {
			abar.setSelectedNavigationItem(savedInstanceState.getInt(TAB_POSITION));
		}
		onChosenOS(OSChooserFragment.LINUX);
//		onShowOSChooser();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(context);
		inflater.inflate(R.menu.activity_tutorial, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_activity_tutorial_android:
			setOS(OSChooserFragment.ANDROID);
			return true;
		case R.id.menu_activity_tutorial_linux:
			setOS(OSChooserFragment.LINUX);
			return true;
		case R.id.menu_activity_tutorial_windows:
			setOS(OSChooserFragment.WINDOWS);
			return true;
		case R.id.menu_activity_tutorial_osx:
			setOS(OSChooserFragment.OSX);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void setOS(int os) {
		for (int i = 0; i < tabAdapter.getCount(); i++) {
			tabAdapter.instantiateItem(viewPager, i);
			((TutorialTabFragment) tabAdapter.getItem(i)).setOS(os);
		}
		viewPager.invalidate();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(TAB_POSITION, getSupportActionBar().getSelectedNavigationIndex());
	}

	/**
	 * This is a helper class that implements the management of tabs and all
	 * details of connecting a ViewPager with associated TabHost. It relies on a
	 * trick. Normally a tab host has a simple API for supplying a View or
	 * Intent that each tab will show. This is not sufficient for switching
	 * between pages. So instead we make the content part of the tab host 0dp
	 * high (it is not shown) and the TabsAdapter supplies its own dummy view to
	 * show as the tab content. It listens to changes in tabs, and takes care of
	 * switch to the correct paged in the ViewPager whenever the selected tab
	 * changes.
	 */
	public static class TabsAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener,
			ActionBar.TabListener {
		private final Context mContext;
		private final ActionBar mBar;
		private final ViewPager mViewPager;
		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
		private SparseArray<Fragment> fragments = new SparseArray<Fragment>();

		static final class TabInfo {
			private final Class<?> clss;
			private final Bundle args;

			TabInfo(Class<?> _class, Bundle _args) {
				clss = _class;
				args = _args;
			}
		}

		public TabsAdapter(FragmentActivity activity, ActionBar bar, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			mContext = activity;
			mBar = bar;
			mViewPager = pager;
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
		}

		public void addTab(ActionBar.Tab tab, Class<? extends Fragment> clss, Bundle args) {
			TabInfo info = new TabInfo(clss, args);
			tab.setTag(info);
			tab.setTabListener(this);
			mTabs.add(info);
			mBar.addTab(tab);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo info = mTabs.get(position);
			if (fragments.get(position) == null) {
				Fragment f = Fragment.instantiate(mContext, info.clss.getName(), info.args);
				fragments.put(position, f);
			}
			return fragments.get(position);
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			mBar.setSelectedNavigationItem(position);
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Object tag = tab.getTag();
			for (int i = 0; i < mTabs.size(); i++) {
				if (mTabs.get(i) == tag) {
					try {
						mViewPager.setCurrentItem(i);
					} catch (IllegalStateException e) {
						e.printStackTrace();
					}
				}
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {

		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {

		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

	@Override
	public void onChosenOS(int osType) {
		Log.d(TAG, "onChosenOs(" + osType);
		viewPager.removeAllViews();
		tabAdapter = new TabsAdapter(this, abar, viewPager);
		Bundle args = new Bundle();
		args.putInt(TutorialTabFragment.OS_TYPE, osType);
		args.putInt(TutorialTabFragment.STEP, TutorialTabFragment.STEP_ONE);
		tabAdapter.addTab(abar.newTab().setText(R.string.tutorial_step_one_tab_title), TutorialTabFragment.class, args);
		args = new Bundle();
		args.putInt(TutorialTabFragment.STEP, TutorialTabFragment.STEP_TWO);
		args.putInt(TutorialTabFragment.OS_TYPE, osType);
		tabAdapter.addTab(abar.newTab().setText(R.string.tutorial_step_two_tab_title), TutorialTabFragment.class, args);
		args = new Bundle();
		args.putInt(TutorialTabFragment.STEP, TutorialTabFragment.STEP_THREE);
		args.putInt(TutorialTabFragment.OS_TYPE, osType);
		tabAdapter.addTab(abar.newTab().setText(R.string.tutorial_step_three_tab_title), TutorialTabFragment.class,
				args);
		abar.show();
		this.detachChooser();
	}

	private void detachChooser() {
		if (chooserFragment != null) {
			FragmentTransaction t = fmanager.beginTransaction();
			t.remove(chooserFragment);
			t.commit();
		}
	}

	@Override
	public void onShowOSChooser() {
		abar.hide();
		fmanager = getSupportFragmentManager();
		chooserFragment = new OSChooserFragment();
		FragmentTransaction t = fmanager.beginTransaction().add(android.R.id.content, chooserFragment,
				OSChooserFragment.FRAGMENT_TAG);
		t.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down, R.anim.slide_in_up, R.anim.slide_out_down);
		t.commit();
	}
}
