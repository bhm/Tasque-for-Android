package com.bustiblelemons.tasque.splash;

import static com.bustiblelemons.tasque.utilities.Values.TAG;
import it.bova.rtmapi.Token;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.main.NotesFragment;
import com.bustiblelemons.tasque.main.SettingsUtil;
import com.bustiblelemons.tasque.rtm.RTMAuthFragment;
import com.bustiblelemons.tasque.rtm.RTMAuthFragment.OnCompleteAuthentication;
import com.bustiblelemons.tasque.rtm.RTMAuthFragment.OnDetachRTMAuthFragment;
import com.bustiblelemons.tasque.rtm.RTMAuthFragment.OnShowRTMAuthFragment;
import com.bustiblelemons.tasque.splash.ExportToExternalFragment.OnExportOptionsChosen;
import com.bustiblelemons.tasque.splash.ExportToExternalFragment.OnShowExportOptions;
import com.bustiblelemons.tasque.splash.ExternalProblemsFragment.OnDatabaseSearchFinished;
import com.bustiblelemons.tasque.utilities.Connection;
import com.bustiblelemons.tasque.utilities.Utility;
import com.bustiblelemons.tasque.utilities.Values.FragmentArguments;
import com.bustiblelemons.tasque.utilities.Values.FragmentArguments.OsInfoFragment;
import com.bustiblelemons.tasque.utilities.Values.ImporterArguments;
import com.bustiblelemons.tasque.utilities.Values.TasqueArguments;

public class ImporterActivity extends SherlockFragmentActivity implements OnClickListener, OnPageChangeListener,
		OnCheckedChangeListener, OnDatabaseSearchFinished, OnExportOptionsChosen, OnShowExportOptions,
		OnCompleteAuthentication, OnShowRTMAuthFragment, OnItemClickListener, OnDetachRTMAuthFragment {

	private static final String PAGER_ITEM = "pagerItem";

	private ViewPager pager;
	private SystemPagerAdapter adapter;
	private TextView rtmButton;
	private TextView startFreshButton;
	private Context context;
	private RadioGroup radioPips;
	private FragmentManager fmanager;
	private ExternalProblemsFragment externalProblemsFragment;
	private ExportToExternalFragment exportToExternal;
	private RTMAuthFragment rtmAuthFragment;

	private int currentPagerItem = 0;

	private boolean startFresh;
	private boolean useRememberTheMilk;
	private boolean SHOW_CHOOSER;

	private LinearLayout chooserView;
	private ListView chooserListView;
	private LinearLayout importerView;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		context = getApplicationContext();
		fmanager = getSupportFragmentManager();
		getSupportActionBar().hide();
		setContentView(R.layout.activity_importer);
		radioPips = (RadioGroup) findViewById(R.id.importer_pips);
		importerView = (LinearLayout) findViewById(R.id.activity_importer_main);
		chooserView = (LinearLayout) findViewById(R.id.activity_importer_chooser);
		chooserListView = (ListView) chooserView.findViewById(R.id.activity_importer_list);
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
		Log.d(TAG, "Importer Activity:onStart()");
		((RadioButton) radioPips.getChildAt(currentPagerItem)).setChecked(true);
		if (Utility.isExtenalAvailable()) {
			Intent startIntent = getIntent();
			if (startIntent != null) {
				SHOW_CHOOSER = startIntent.getBooleanExtra(TasqueArguments.SHOW_DATABASE_CHOOSER, false);
				if (SHOW_CHOOSER) {
					Utility.toggleVisibiity(importerView, View.GONE);
					Utility.toggleVisibiity(chooserView, View.VISIBLE);
					chooserListView.setAdapter(new MultipleFilesAdapter(context, Utility
							.getSyncedDatabasePaths(context)));
					chooserListView.setOnItemClickListener(this);
				}
			}
		} else {
//			this.showExternalProblemsFragment();
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
			transaction.add(android.R.id.content, externalProblemsFragment, NotesFragment.FRAGMENT_TAG);
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
			if (rtmAuthFragment != null) {
				if (rtmAuthFragment.isAdded()) {
					return rtmAuthFragment.onKeyDown(keyCode, event);
				}
			}
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
			if (Connection.isUp(context)) {
				this.onShowRTMAuthFragment();
				useRememberTheMilk = true;
			} else {
				Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
				startActivity(intent);
			}
			break;
		case R.id.importer_start_fresh_here:
			try {
				Database.createFreshDatabase(context, true);
			} finally {
				startFresh = true;
				SettingsUtil.setStartedFresh(context, true);
				this.showExportOptionsFragment();
			}
		default:
			break;
		}
	}

	private void showExportOptionsFragment() {
		exportToExternal = new ExportToExternalFragment();
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.addToBackStack(null);
		transaction.add(android.R.id.content, exportToExternal, ExportToExternalFragment.FRAGMENT_TAG);
		transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left,
				R.anim.slide_out_right);
		transaction.commit();
	}

	@Override
	public void onShowExportOptions() {
		this.showExportOptionsFragment();
	}

	@Override
	public void onShowRTMAuthFragment() {
		rtmAuthFragment = new RTMAuthFragment();
		FragmentTransaction transaction = fmanager.beginTransaction();
		transaction.add(android.R.id.content, rtmAuthFragment, RTMAuthFragment.FRAGMENT_TAG);
		transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left,
				R.anim.slide_out_right);
		transaction.commit();
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
		setResult(RESULT_OK, importerIntent);
		this.finish();
	}

	public void syncedFileChosen(String filePath) {
		Utility.toggleVisibiity(findViewById(R.id.activity_importer_main), View.GONE);
		Utility.toggleVisibiity(findViewById(R.id.activity_importer_progress_bar), View.VISIBLE);
		Log.d(TAG, "syncedFileChosen(" + filePath + ")");
		SettingsUtil.setSyncedDatabsePath(context, filePath);
		try {
			if (Utility.isExternalNewer(context)) {
				Utility.copyDatabase(context);
			}
		} finally {
			Intent importerIntent = new Intent();
			importerIntent.putExtra(ImporterArguments.START_FRESH, startFresh);
			importerIntent.putExtra(ImporterArguments.USE_REMEMBER_THE_MILK, useRememberTheMilk);
			importerIntent.putExtra(ImporterArguments.SYNCED_DATABASE_PATH, filePath);
			setResult(RESULT_OK, importerIntent);
			this.finish();
		}
	}

	@Override
	public void onCompleteAuthentication(Object token) {
		try {
			SettingsUtil.saveRTMToken(context, (Token) token);
			SettingsUtil.setUseRTMBackend(context, true);
			fmanager.beginTransaction().detach(rtmAuthFragment).commit();
			this.showExportOptionsFragment();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		useRememberTheMilk = false;
		Log.d(TAG, "onItemClick\nRTM " + useRememberTheMilk + "\nStart fresh " + startFresh);
		String path = ((MultipleFilesAdapter) arg0.getAdapter()).getPath(arg2);
		this.syncedFileChosen(path);
	}

	@Override
	public void onDetachRTMAuthFragment() {
		fmanager.beginTransaction().detach(rtmAuthFragment).commit();
	}
}
