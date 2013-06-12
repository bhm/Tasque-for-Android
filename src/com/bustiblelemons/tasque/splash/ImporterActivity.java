package com.bustiblelemons.tasque.splash;

import static com.bustiblelemons.tasque.utilities.Values.TAG;
import it.bova.rtmapi.Token;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.rtm.RTMAuthFragment;
import com.bustiblelemons.tasque.rtm.RTMAuthFragment.OnCompleteAuthentication;
import com.bustiblelemons.tasque.rtm.RTMAuthFragment.OnDetachRTMAuthFragment;
import com.bustiblelemons.tasque.rtm.RTMAuthFragment.OnShowRTMAuthFragment;
import com.bustiblelemons.tasque.settings.SettingsUtil;
import com.bustiblelemons.tasque.splash.ExportToExternalFragment.OnExportOptionsChosen;
import com.bustiblelemons.tasque.splash.ExportToExternalFragment.OnShowExportOptions;
import com.bustiblelemons.tasque.splash.ExternalProblemsFragment.OnDatabaseSearchFinished;
import com.bustiblelemons.tasque.tutorial.SynchronizeTutorialActivity;
import com.bustiblelemons.tasque.tutorial.SynchronizedFilesAdapter;
import com.bustiblelemons.tasque.utilities.Connection;
import com.bustiblelemons.tasque.utilities.Utility;
import com.bustiblelemons.tasque.utilities.Values.ImporterArguments;

public class ImporterActivity extends SherlockFragmentActivity implements OnClickListener, OnDatabaseSearchFinished,
		OnExportOptionsChosen, OnShowExportOptions, OnCompleteAuthentication, OnShowRTMAuthFragment,
		OnItemClickListener, OnDetachRTMAuthFragment {

	private Context context;
	private FragmentManager fmanager;
	private ExternalProblemsFragment externalProblemsFragment;
	private ExportToExternalFragment exportToExternal;
	private RTMAuthFragment rtmAuthFragment;

	private ListView synchronizedFilesList;

	private boolean startFresh;
	private boolean useRememberTheMilk;

	private SynchronizedFilesAdapter synchronizedFilesAdapter;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		context = getApplicationContext();
		fmanager = getSupportFragmentManager();
		getSupportActionBar().hide();
		setContentView(R.layout.activity_importer);
		findViewById(R.id.importer_rtm).setOnClickListener(this);
		findViewById(R.id.importer_start_fresh_here).setOnClickListener(this);
		findViewById(R.id.importer_synchronize_with_other).setOnClickListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "Importer Activity:onStart()");
		ArrayList<File> synchronizedFiles = Utility.getSyncedDatabasePaths(context);
		if (synchronizedFiles.size() > 0) {
			synchronizedFilesAdapter = new SynchronizedFilesAdapter(context, synchronizedFiles);
			synchronizedFilesList.setAdapter(synchronizedFilesAdapter);
			synchronizedFilesList.setOnClickListener(this);
			Utility.toggleVisibiity(synchronizedFilesList, View.VISIBLE);
			synchronizedFilesList.setAdapter(new SynchronizedFilesAdapter(context, Utility
					.getSyncedDatabasePaths(context)));
			synchronizedFilesList.setOnItemClickListener(this);
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
			break;
		case R.id.importer_synchronize_with_other:
			Intent tutorial = new Intent(context, SynchronizeTutorialActivity.class);
			startActivity(tutorial);
			break;
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
	public void databaseSearchFinished(int result) {
		if (result == ExternalProblemsFragment.DATABASE_NOT_FOUND) {
			fmanager.beginTransaction().detach(externalProblemsFragment).commit();
		} else if (result == ExternalProblemsFragment.DATABASE_COPIED) {
			this.finish();
		}
	}

	@Override
	public void onExportOptionsChosen() {
		Utility.toggleVisibiity(findViewById(R.id.activity_importer_progress_bar), View.VISIBLE);
		Intent importerIntent = new Intent();
		importerIntent.putExtra(ImporterArguments.START_FRESH, startFresh);
		importerIntent.putExtra(ImporterArguments.USE_REMEMBER_THE_MILK, useRememberTheMilk);
		setResult(RESULT_OK, importerIntent);
		this.finish();
	}

	public void syncedFileChosen(String filePath) {
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
		String path = ((SynchronizedFilesAdapter) arg0.getAdapter()).getPath(arg2);
		this.syncedFileChosen(path);
	}

	@Override
	public void onDetachRTMAuthFragment() {
		fmanager.beginTransaction().detach(rtmAuthFragment).commit();
	}
}
