package com.bustiblelemons.tasque;

import static com.bustiblelemons.tasque.Values.TAG;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

public class Utility {

	private static Animation slide_in_left;
	private static Animation slide_out_right;

	// private static Animation slide_in_up;
	// private static Animation slide_out_down;

	public static void loadAnimations(Context context) {
		slide_in_left = AnimationUtils.loadAnimation(context, R.anim.slide_in_left);
		slide_out_right = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
		// slide_in_up = AnimationUtils.loadAnimation(context,
		// R.anim.slide_in_up);
		// slide_out_down = AnimationUtils.loadAnimation(context,
		// R.anim.slide_out_down);
	}

	public static void hideKeyboard(Context context, IBinder token) {
		InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		manager.hideSoftInputFromInputMethod(token, InputMethodManager.SHOW_FORCED);
	}

	public static void hideKeyboard(View view) {
		InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(
				Context.INPUT_METHOD_SERVICE);
		manager.hideSoftInputFromInputMethod(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}

	public static boolean isExtenalAvailable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Searches for the database in the external memory;
	 * 
	 * @param context
	 * @return Absolute path if found or 0-length String if not;
	 */
	public static String getSyncedDatabasePath(Context context) {
		File externalMemory = Environment.getExternalStorageDirectory();
		String[] paths = context.getResources().getStringArray(R.array.fragment_importer_paths);
		for (String path : paths) {
			File t = new File(externalMemory.getAbsolutePath() + path, DatabaseAdapter.DATABASE_NAME);
			if (!t.isDirectory()) {
				if (t.exists()) {
					return t.getAbsolutePath();
				}
			}
		}
		return "";

	}

	/**
	 * Sets font size for settings;
	 * 
	 * @param title
	 */
	public static void applyFontSize(View view) {
		if (view.getClass() == TextView.class) {
			((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsUtil.getListFontSize(view.getContext()));
		} else if (view.getClass() == EditText.class) {
			((EditText) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsUtil.getInputFontSize(view.getContext()));
		}
	}

	public static void applyCompletionDateFontSize(TextView v) {
		v.setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsUtil.getCompletionDateFontSize(v.getContext()));
	}

	public static void applyDueDateFontSize(TextView v) {
		v.setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsUtil.getDueDateFontSize(v.getContext()));
	}

	public static boolean isDatabaseSynced(Context context) {
		String[] paths = context.getResources().getStringArray(R.array.fragment_importer_paths);
		if (Utility.isExtenalAvailable()) {
			File external = Environment.getExternalStorageDirectory();
			for (String path : paths) {
				if (new File(external, path).exists()) {
					return true;
				}
			}
		} else {
			return false;
		}
		return false;
	}

	/**
	 * Finds if in known paths are files and returns list of them
	 * 
	 * @param context
	 * @return
	 */
	public static ArrayList<File> getSyncedDatabasePaths(Context context) {
		File externalMemory = Environment.getExternalStorageDirectory();
		ArrayList<File> files = new ArrayList<File>();
		String[] paths = context.getResources().getStringArray(R.array.fragment_importer_paths);
		for (String path : paths) {
			File t = new File(externalMemory.getAbsolutePath() + path, DatabaseAdapter.DATABASE_NAME);
			if (!t.isDirectory()) {
				if (t.exists()) {
					files.add(t);
				}
			}
		}
		return files;
	}

	public static void toggleVisibiity(View view, int visibility) {
		if (visibility == View.VISIBLE) {
			view.startAnimation(slide_in_left);
		} else if (visibility == View.GONE) {
			view.startAnimation(slide_out_right);
		}
		view.setVisibility(visibility);
	}

	/**
	 * Backs up the database file before pushing the new version out for syncing
	 * via a service.
	 * 
	 * @param context
	 * @param syncedDatabase
	 *            the full absolute file path saved in settings.
	 * @return true on successful push of a new file. Will fail if external
	 *         memory is not available.
	 */
	public static boolean backupSynced(Context context, String syncedDatabase) {
		return backupSynced(context, new File(syncedDatabase));
	}

	public static boolean backupSynced(Context context, File syncedDatabase) {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			if (syncedDatabase.exists()) {
				return syncedDatabase.renameTo(new File(syncedDatabase + ".backup"));
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Pushes out the database on closing
	 * 
	 * @param context
	 * @return if successfully pushed local database in place of a synced one
	 */
	public static boolean pushDatabase(Context context) {
		File appDatabase = Utility.getAppDatabaseFile(context);
		String syncedPath = SettingsUtil.getSyncedDataasePath(context);
		if (syncedPath.length() > 0) {
			File syncedDatabase = new File(syncedPath);
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state)) {
				if (syncedDatabase.exists()) {
					return backupSynced(context, syncedDatabase) ? copy(appDatabase, syncedDatabase) : false;
				}
				return true;
			}
		}
		return false;
	}

	public static File getAppDatabaseFile(Context context) {
		return new File(context.getApplicationInfo().dataDir + "/databases", DatabaseAdapter.DATABASE_NAME);
	}

	public static boolean isExternalNewer(Context context) {
		File localDB = Utility.getAppDatabaseFile(context);
		File externalDB = new File(Utility.getSyncedDatabasePath(context));
		return (localDB.lastModified() < externalDB.lastModified()) ? true : false;
	}

	/**
	 * Copies the database for local use after checking if it was modified.
	 * 
	 * @param context
	 * @param path
	 *            to the synced database backend file on external memory
	 */
	public static boolean copyDatabase(Context context, String path) {
		File appDatabaseDir = new File(new File(context.getApplicationInfo().dataDir), "databases");
		File appDatabase = new File(appDatabaseDir, DatabaseAdapter.DATABASE_NAME);
		File syncedDatabase = new File(path);
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss yyyy/MM/dd");
		Log.d(TAG,
				appDatabaseDir.getAbsolutePath() + "\n" + appDatabase.getAbsolutePath() + "\n"
						+ syncedDatabase.getAbsolutePath());
		Log.d(TAG, "Synced timestamp: " + format.format(new Date(syncedDatabase.lastModified())));
		Log.d(TAG, "App Database timestamp: " + format.format(new Date(appDatabase.lastModified())));
		if (!appDatabaseDir.exists()) {
			appDatabaseDir.mkdir();
		}
		if (!appDatabase.exists()) {
			Log.d(TAG, "No previous copy.");
			if (syncedDatabase.exists()) {
				return Utility.copy(syncedDatabase, appDatabase);
			} else {
				Log.d(TAG, "Did not find " + syncedDatabase.getAbsolutePath());
			}
		} else {
			Log.d(TAG, "Found previous copy");
			if (appDatabase.lastModified() < syncedDatabase.lastModified()) {
				Log.d(TAG, "Local file is older. Copy");
				return Utility.copy(syncedDatabase, appDatabase);
			}
		}
		return false;
	}

	public static boolean copyDatabase(Context context) {
		return copyDatabase(context, Utility.getSyncedDatabasePath(context));
	}

	@SuppressWarnings("unused")
	private static boolean copy(String source, String destination) {
		return copy(new File(source), new File(destination));
	}

	private static boolean copy(File source, File destination) {
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(source));
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destination));
			byte[] buffer = new byte[10];
			while (in.read(buffer) != -1) {
				out.write(buffer);
			}
			out.flush();
			in.close();
			out.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean exportFileExists(Context context) {
		String externalPath = SettingsUtil.getSyncedDataasePath(context);
		if (externalPath.length() > 0) {
			return new File(externalPath).exists();
		} else {
			return false;
		}
	}

	/**
	 * DATES
	 */
	public static String getSimpleDate(long date, Context context) {
		String dateFormat = SettingsUtil.getDateFromat(context);
		return dateFormat.length() > 0 ? ((new SimpleDateFormat(dateFormat)).format(new Date(date * 1000))) : Utility
				.getSimpleDate(date);
	}

	public static String getSimpleDate(long date) {
		return ((new SimpleDateFormat("d/M - E").format(new Date(date * 1000))));
	}

	public static boolean isToday(long date) {
		Calendar t = Calendar.getInstance();
		Calendar d = Calendar.getInstance();
		t.setTime(new Date());
		d.setTime(new Date(date * 1000));
		return (t.get(Calendar.ERA) == d.get(Calendar.ERA) && t.get(Calendar.YEAR) == d.get(Calendar.YEAR) && t
				.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR));
	}

	public static boolean isOverDue(long date) {
		Calendar t = Calendar.getInstance();
		Calendar d = Calendar.getInstance();
		t.setTime(new Date());
		d.setTime(new Date(date * 1000));
		return t.compareTo(d) > 0 ? true : false;
	}

}