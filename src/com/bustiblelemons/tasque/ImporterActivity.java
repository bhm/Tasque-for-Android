package com.bustiblelemons.tasque;

import java.sql.SQLException;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bustiblelemons.tasque.ExportToExternalFragment.OnExportOptionsChosen;
import com.bustiblelemons.tasque.ExternalProblemsFragment.OnDatabaseSearchFinished;
import com.bustiblelemons.tasque.Values.FragmentArguments;
import com.bustiblelemons.tasque.Values.FragmentArguments.OsInfoFragment;
import com.bustiblelemons.tasque.Values.FragmentFlags;
import com.bustiblelemons.tasque.Values.ImporterArguments;

public class ImporterActivity extends SherlockFragmentActivity implements OnClickListener, OnPageChangeListener,
		OnCheckedChangeListener, OnDatabaseSearchFinished, OnExportOptionsChosen {

	private static final String PAGER_ITEM = "pagerItem";
	
	private ViewPager pager;
	private SystemPagerAdapter adapter;
	private TextView rtmButton;
	private TextView startFreshButton;
	private Context context;
	private RadioGroup radioPips;
	private FragmentManager fmanager;
	private ExternalProblemsFragment externalProblemsFragment;
	private ExportToExternalFragment exportToDropboxFragment;

	private int currentPagerItem = 0;

	private boolean startFresh;
	private boolean useRememberTheMilk;
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		context = getApplicationContext();
		fmanager = getSupportFragmentManager();
		getSupportActionBar().hide();
		setContentView(R.layout.activity_importer);
		radioPips = (RadioGroup) findViewById(R.id.importer_pips);
		radioPips.setOnCheckedChangeListener(this);
		this.populateRadioGroup(radioPips);
		pager = (ViewPager) findViewById(R.id.pager);
		adapter = new SystemPagerAdapter(getSupportFragmentManager());
		pager.setAdapter(adapter);
		pager.setOnPageChangeListener(this);
		pager.setCurrentItem(0);
		rtmButton = (TextView) findViewById(R.id.importer_rtm);
		rtmButton.setOnClickListener(this);
		startFreshButton = (TextView) findViewById(R.id.importer_start_fresh_here);
		startFreshButton.setOnClickListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		((RadioButton) radioPips.getChildAt(currentPagerItem)).setChecked(true);
		if (Utility.isExtenalAvailable()) {
		
		} else {
			this.showExternalProblemsFragment();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (pager != null) {
			currentPagerItem = pager.getCurrentItem();
			outState.putInt(PAGER_ITEM, currentPagerItem);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (pager != null) {
			pager.setCurrentItem(savedInstanceState.getInt(PAGER_ITEM));
		}
	}

	private void showExternalProblemsFragment() {
		if (externalProblemsFragment == null) {
			externalProblemsFragment = new ExternalProblemsFragment();
		}
		if (!externalProblemsFragment.isVisible()) {
			fmanager.beginTransaction().show(externalProblemsFragment).commit();
		} else {
			FragmentTransaction transaction = fmanager.beginTransaction();
			transaction.add(android.R.id.content, externalProblemsFragment, FragmentFlags.NOTES_FRAGMENT);
			transaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down, R.anim.slide_in_up,
					R.anim.slide_out_down);
			transaction.addToBackStack(null);
			transaction.commit();
		}
	}

	private void populateRadioGroup(RadioGroup radioPips2) {
		for (int i = 0; i < OsInfoFragment.OS_COUNT; i++) {
			RadioButton b = (RadioButton) LayoutInflater.from(context).inflate(R.layout.single_radio_pip, null);
			b.setId(i);
			radioPips2.addView(b);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			setResult(RESULT_CANCELED);
			this.finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public class SystemPagerAdapter extends FragmentPagerAdapter {
		private int PAGER_COUNT = 3;

		public SystemPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int arg0) {
			OSInfoFragment f = new OSInfoFragment();
			Bundle args = new Bundle();
			args.putInt(FragmentArguments.OsInfoFragment.OS_TYPE, arg0);
			f.setArguments(args);
			return f;
		}

		@Override
		public int getCount() {
			return this.PAGER_COUNT;
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.importer_rtm:
			try {
				Database.creatFreshDatabase(context);
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				// Pop RTM fragment Here
			}
			useRememberTheMilk = true;
			break;
		case R.id.importer_start_fresh_here:
			try {
				Database.creatFreshDatabase(context);
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				SettingsUtil.setStartedFresh(context, true);
				if (exportToDropboxFragment == null) {
					exportToDropboxFragment = new ExportToExternalFragment();
				}
				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				transaction.addToBackStack(null);
				transaction.add(android.R.id.content, exportToDropboxFragment, FragmentFlags.NOTES_FRAGMENT);
				transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left,
						R.anim.slide_out_right);
				transaction.commit();
			}
		default:
			break;
		}
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {

	}

	@Override
	public void onPageSelected(int arg0) {
		((RadioButton) radioPips.getChildAt(arg0)).setChecked(true);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		pager.setCurrentItem(checkedId);
	}

	@Override
	public void databaseSearchFinished(int result) {
		if (result == ExternalProblemsFragment.DATABASE_NOT_FOUND) {
			fmanager.beginTransaction().detach(externalProblemsFragment).commit();
		} else if (result == ExternalProblemsFragment.DATABASE_COPIED) {
			this.finish();
		}
	}

	@Override
	public void onExportOptionsChosen() {
		Utility.toggleVisibiity(findViewById(R.id.activity_importer_main), View.GONE);
		Utility.toggleVisibiity(findViewById(R.id.activity_importer_progress_bar), View.VISIBLE);
		Intent importerIntent = new Intent();
		importerIntent.putExtra(ImporterArguments.START_FRESH, startFresh);
		importerIntent.putExtra(ImporterArguments.USE_REMEMBER_THE_MILK, useRememberTheMilk);
		setIntent(importerIntent);
		setResult(RESULT_OK);
		this.finish();		
	}
}
