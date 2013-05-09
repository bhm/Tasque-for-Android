package com.bustiblelemons.tasque;

import static com.bustiblelemons.tasque.Values.TAG;

import java.io.File;
import java.sql.SQLException;
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
			PreferenceManager.setDefaultValues(context, R.xml.pref_general, true);
		}
		if (SettingsUtil.getExportOnExit(context)) {
			Log.d(TAG, "Exporting ENABLED");
			if (SettingsUtil.getStartedFresh(context)) {
				Log.d(TAG, "Started Fresh");
				this.startNormaly();
				return;
			}
			String externalDBPath = SettingsUtil.getSyncedDataasePath(context);
			if (externalDBPath.length() > 0) {
				Utility.copyDatabase(context, externalDBPath);
				Log.d(TAG, "External Path : " + externalDBPath);
				if (!new File(externalDBPath).exists()) {
					SHOW_DATABASE_CHOOSER = true;
					LOST_DATABASE = true;
				}
				this.startNormaly();
			} else {
				ArrayList<File> externalDatabases = Utility.getSyncedDatabasePaths(context);
				switch (externalDatabases.size()) {
				default:
					SHOW_DATABASE_CHOOSER = true;
					LOST_DATABASE = false;
					Log.d(TAG, "Multiple external DBs");
					this.startImporter();
					break;
				case 0:
					SHOW_DATABASE_CHOOSER = false;
					LOST_DATABASE = false;
					Log.d(TAG, "No external DBs");
					this.startImporter();
					break;
				case 1:
					Log.d(TAG, "Only one external DB");
					Utility.copyDatabase(context, externalDatabases.get(0).getAbsolutePath());
					this.startNormaly();
					break;
				}
			}
		} else {
			Log.d(TAG, "Exporting disabled");
			if (SettingsUtil.isFirstRun(context)) {
				Log.d(TAG, "First Run");
				this.startImporter();
			} else {
				Log.d(TAG, "Next Run");
				if (!Database.exists(context)) {
					try {
						Database.creatFreshDatabase(context);
					} catch (SQLException e) {
						e.printStackTrace();
					} finally {

					}
				}
				this.startNormaly();
			}
		}
	}

	private void startImporter() {
		Intent importer = new Intent(this, ImporterActivity.class);
		startActivityForResult(importer, Values.IMPORTER_REQUEST_CODE);
	}

	private void startNormaly() {
		Log.d(TAG, "Lost: " + LOST_DATABASE + "\nShow database: " + SHOW_DATABASE_CHOOSER);
		Intent tasque = new Intent(this, Tasque.class);
		tasque.putExtra(TasqueArguments.LOST_DATABASE, LOST_DATABASE);
		tasque.putExtra(TasqueArguments.SHOW_DATABASE_CHOOSER, SHOW_DATABASE_CHOOSER);
		startActivity(tasque);
		this.finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		SHOW_DATABASE_CHOOSER = false;
		Log.d(TAG, "onResutl");
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == Values.IMPORTER_REQUEST_CODE) {
				ArrayList<File> detectedDatabases = Utility.getSyncedDatabasePaths(context);
				switch (detectedDatabases.size()) {
				case 1:
					Utility.copyDatabase(context, detectedDatabases.get(0).getAbsolutePath());
					SHOW_DATABASE_CHOOSER = false;
					LOST_DATABASE = false;
					break;
				case 0:
					SHOW_DATABASE_CHOOSER = false;
					LOST_DATABASE = false;
					break;
				default:
					SHOW_DATABASE_CHOOSER = true;
					LOST_DATABASE = false;
					break;
				}
				Log.d(TAG, "on Result, now Launching with\n Lost: " + LOST_DATABASE + "\nSHOW: "
						+ SHOW_DATABASE_CHOOSER);
				this.startNormaly();
			}
		} else if (resultCode == RESULT_CANCELED) {
			this.finish();
		}
	}

}
