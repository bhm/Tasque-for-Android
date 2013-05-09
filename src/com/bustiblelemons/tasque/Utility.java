package com.bustiblelemons.tasque;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.os.Environment;
import android.os.IBinder;
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
//	private static Animation slide_in_up;
//	private static Animation slide_out_down;

	public static void loadAnimations(Context context) {
		slide_in_left = AnimationUtils.loadAnimation(context, R.anim.slide_in_left);
		slide_out_right = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
//		slide_in_up = AnimationUtils.loadAnimation(context, R.anim.slide_in_up);
//		slide_out_down = AnimationUtils.loadAnimation(context, R.anim.slide_out_down);
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

	public static String getSimpleDate(int date, Context context) {
		String dateFormat = SettingsUtil.getDateFromat(context);
		return dateFormat.length() > 0 ? ((new SimpleDateFormat(dateFormat)).format(new Date(date * 1000))) : Utility
				.getSimpleDate(date);
	}

	public static String getSimpleDate(int date) {
		return ((new SimpleDateFormat("d/M - E").format(new Date(date * 1000))));
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
		File appDatabase = new File(context.getApplicationInfo().dataDir + "/databases", DatabaseAdapter.DATABASE_NAME);
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
		if (syncedDatabase.length() > 0 && syncedDatabase.lastModified() > appDatabase.lastModified()) {
			if (!appDatabaseDir.exists()) {
				appDatabaseDir.mkdir();
			}
			return copy(syncedDatabase, appDatabase);
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
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

}
