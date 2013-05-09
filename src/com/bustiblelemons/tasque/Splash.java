package com.bustiblelemons.tasque;

import static com.bustiblelemons.tasque.Values.TAG;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockActivity;
import com.bustiblelemons.tasque.Values.TasqueArguments;

public class Splash extends SherlockActivity {

	private Context context;
	private boolean SHOW_DATABASE_CHOOSER = false;
	private boolean LOST_DATABASE = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		context = getApplicationContext();
		Utility.loadAnimations(context);
		if (SettingsUtil.isFirstRun(context)) {
			Log.d(TAG, "First run");
			PreferenceManager.setDefaultValues(context, R.xml.pref_general, true);		
			ArrayList<File> externalDatabases = Utility.getSyncedDatabasePaths(context);
			switch (externalDatabases.size()) {
			default:
				SHOW_DATABASE_CHOOSER = true;
				LOST_DATABASE = false;
				Log.d(TAG, "Multiple external DBs");
				break;
			case 0:
				SHOW_DATABASE_CHOOSER = false;
				LOST_DATABASE = false;
				Log.d(TAG, "No external DBs");
				break;
			case 1:
				Log.d(TAG, "Only one external DB");
				Utility.copyDatabase(context, externalDatabases.get(0).getAbsolutePath());
				this.startNormaly();
				return;
			}
			this.startImporter();
		} else {
			if (SettingsUtil.getStartedFresh(context)) {
				this.startNormaly();
				return;
			}
			if (SettingsUtil.getExportOnExit(context)) {
				if (Utility.exportFileExists(context)) {
					try {
						Utility.copyDatabase(context);
					} finally {
						Log.d(TAG, "Copied the database, now starting normally");
						this.startNormaly();
					}					
				} else {
					SHOW_DATABASE_CHOOSER = true;
					LOST_DATABASE = true;
					this.startImporter();
				}
			}
		}
	}

	private void startImporter() {
		Intent importer = new Intent(this, ImporterActivity.class);
		importer.putExtra(TasqueArguments.LOST_DATABASE, LOST_DATABASE);
		importer.putExtra(TasqueArguments.SHOW_DATABASE_CHOOSER, SHOW_DATABASE_CHOOSER);
		startActivityForResult(importer, Values.IMPORTER_REQUEST_CODE);
	}

	private void startNormaly() {
		Intent tasque = new Intent(this, Tasque.class);
		startActivity(tasque);
		this.finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onResult");
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == Values.IMPORTER_REQUEST_CODE) {
				this.startNormaly();
			}
		} else if (resultCode == RESULT_CANCELED) {
			this.finish();
		}
	}

}
