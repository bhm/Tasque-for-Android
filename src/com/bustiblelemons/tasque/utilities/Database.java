package com.bustiblelemons.tasque.utilities;

import static com.bustiblelemons.tasque.utilities.Values.TAG;

import java.io.File;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map.Entry;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.main.SettingsUtil;
import com.bustiblelemons.tasque.utilities.Values.Database.Categories;
import com.bustiblelemons.tasque.utilities.Values.FragmentArguments;

public class Database {
	/**
	 * Checks if the database is present in the apps directory;
	 * 
	 * @param context
	 * @return
	 */
	public static boolean exists(Context context) {
		Log.d(TAG, "Checking for database");
		File appDir = new File(context.getApplicationInfo().dataDir);
		File dbDir = new File(appDir, "databases");
		if (dbDir.exists()) {
			if (dbDir.isDirectory()) {
				Log.d(TAG, new File(dbDir, DatabaseAdapter.DATABASE_NAME).getAbsolutePath());
				if ((new File(dbDir, DatabaseAdapter.DATABASE_NAME)).exists()) {
					Log.d(TAG, "Found local database");
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * @param context
	 * @param taskID
	 *            notes are referenced through ID of a task
	 * @return
	 * @throws SQLException
	 */
	public static String getNote(Context context, String taskID) throws SQLException {
		String note = "";
		DatabaseAdapter a = new DatabaseAdapter(context);
		try {
			a.Open();
			note = a.getNote(taskID);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return note;
	}

	public static String getNote(Context context, int taskID) {
		return Database.getNote(context, Integer.valueOf(taskID));
	}

	public static int getNumberOfCategories(Context context) throws SQLException {
		DatabaseAdapter a = new DatabaseAdapter(context);
		int ret = -1;
		try {
			a.Open();
			ret = a.getNumberOfCategories();
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return ret;
	}

	public static Cursor getCategoriesCursor(Context context) throws SQLException {
		DatabaseAdapter a = new DatabaseAdapter(context);
		Cursor cur = null;
		try {
			a.Open();
			cur = a.getCategories();
			Log.d(TAG, cur.getCount() + " x " + cur.getColumnCount());
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return cur;
	}

	public static ArrayList<Entry<Integer, String>> getCategories(Context context) {
		Cursor cur = null;
		ArrayList<Entry<Integer, String>> ret = new ArrayList<Entry<Integer, String>>();
		try {
			cur = Database.getCategoriesCursor(context);
			Entry<Integer, String> e = new AbstractMap.SimpleEntry<Integer, String>(FragmentArguments.ALL_ID,
					FragmentArguments.ALL);
			ret.add(e);
			Log.d(TAG, cur.getCount() + " x " + cur.getColumnCount());
			while (cur.moveToNext()) {
				int id = cur.getInt(cur.getColumnIndex(Categories.ID));
				String name = cur.getString(cur.getColumnIndex(Categories.NAME));
				e = new AbstractMap.SimpleEntry<Integer, String>(id, name);
				ret.add(e);
			}
			Log.d(TAG, cur.getCount() + " x " + cur.getColumnCount() + "\n" + ret.size());
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return ret;
	}

	public static int getCategoryIdentifier(Context context, String categoryName) throws SQLException {
		DatabaseAdapter a = new DatabaseAdapter(context);
		int r = -1;
		try {
			a.Open();
			r = a.getCategoryIdentifier(categoryName);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static Cursor getTasks(Context context, int categoryID) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		Cursor r = null;
		try {
			a.Open();
			r = a.getTasks(categoryID);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static Cursor getTasks(Context context, String categoryName) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		Cursor r = null;
		try {
			a.Open();
			r = a.getTasks(Database.getCategoryIdentifier(context, categoryName));
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static Cursor getAllTasks(Context context) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		Cursor r = null;
		try {
			a.Open();
			r = a.getAllTasks();
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static String getCategoryName(Context context, int categoryId) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		String r = "";
		try {
			a.Open();
			r = a.getCategoryName(categoryId);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static long createNewCategory(Context context, String nCategoryName) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		long r = -1;
		try {
			a.Open();
			r = a.createNewCategory(nCategoryName);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static Cursor getAllSelectedTasks(Context context) throws SQLException {
		DatabaseAdapter a = new DatabaseAdapter(context);
		ArrayList<String> categories = SettingsUtil.getSelectedCategories(context);
		Cursor cur = null;
		try {
			a.Open();
			cur = a.getTasks(categories);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} catch (SQLiteException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return cur;
	}

	public static ArrayList<String> getAllCategoryIDS(Context context) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		ArrayList<String> IDs = new ArrayList<String>();
		try {
			a.Open();
			IDs = a.getAllCategoriesIDs();
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return IDs;
	}

	public static void setMarkTaskDone(Context context, String id) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		try {
			a.Open();
			a.markTaskDone(id);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
	}

	public static void creatFreshDatabase(Context context) throws SQLException {
		DatabaseAdapter a = new DatabaseAdapter(context);
		String creationCategories = context.getString(R.string.creation_string_table_categories);
		String creationTasks = context.getString(R.string.creation_string_table_tasks);
		String creationNotes = context.getString(R.string.creation_string_table_notes);
		String insertWork = context.getString(R.string.insert_category_work);
		String insertFamily = context.getString(R.string.insert_category_family);
		String insertPersonal = context.getString(R.string.insert_category_personal);
		String insertProject = context.getString(R.string.insert_category_project);
		try {
			a.Open();
			a.execQuery(creationCategories);
			a.execQuery(insertWork);
			a.execQuery(insertFamily);
			a.execQuery(insertPersonal);
			a.execQuery(insertProject);
			a.execQuery(creationTasks);
			a.execQuery(creationNotes);
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			a.Close();
		}
	}

	/**
	 * 
	 * @param context
	 * @param id
	 * @param name
	 *            new value for a name.
	 */
	public static void updateTask(Context context, String id, String name) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		try {
			a.Open();
			a.updateTask(id, name);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
	}

	public static void updateTaskNote(Context context, String id, String oldNote, String note) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		try {
			a.Open();
			a.updateTaskNote(id, oldNote, note);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
	}

	public static Cursor getNotes(Context context, String taskID) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		Cursor r = null;
		try {
			a.Open();
			r = a.getNotes(taskID);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static long insertNote(Context context, String taskID, String body) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		long r = -1;
		try {
			a.Open();
			r = a.insertNote(taskID, body);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static int deleteNotes(Context context, String taskID, Iterable<String> notes) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		int r = 0;
		try {
			a.Open();
			for (String note : notes) {
				r += a.deleteNote(taskID, note);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static long insertNewTask(Context context, String categoryID, String task) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		long r = 0;
		try {
			a.Open();
			if (categoryID.equals(String.valueOf(FragmentArguments.ALL_ID))) {
				int defCategory = SettingsUtil.getDefaultCategoryId(context);
				categoryID = defCategory > 0 ? String.valueOf(defCategory) : String.valueOf(1);
			}
			Log.d(TAG, "Adding " + task + " in category " + categoryID);
			r = a.insertTask(categoryID, task);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static long markDeleted(Context context, Iterable<String> taskIDs) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		long r = -1;
		try {
			a.Open();
			for (String taskID : taskIDs) {
				try {
					Log.d(TAG, "Marking ID " + taskID + "deleted");
					a.markDeleted(taskID);
				} catch (SQLException e) {
					e.printStackTrace();
					a.Close();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static int deleteCategories(Context context, Iterable<String> categoriesIDs) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		int r = 0;
		try {
			a.Open();
			try {
				for (String categoryID : categoriesIDs) {
					r += a.deleteCategory(categoryID);
				}
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			a.Close();
		}
		return r;
	}

	public static Cursor getCompletedTasks(Context context, String categoryID) {
		if (categoryID.equals(String.valueOf(FragmentArguments.ALL_ID))) {
			return Database.getAllSelectedCompltedTasks(context);
		} else {
			return Database.getCompletedTasksOfCategory(context, categoryID);
		}
	}

	public static Cursor getCompletedTasksOfCategory(Context context, String categoryID) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		Cursor r = null;
		try {
			a.Open();
			r = a.getCompletedTasks(categoryID);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static Cursor getAllSelectedCompltedTasks(Context context) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		ArrayList<String> categories = SettingsUtil.getSelectedCategories(context);
		Log.d(TAG, "getAllSelectedCompletedTasks : " + categories.size());
		Cursor r = null;
		try {
			a.Open();
			r = a.getCompletedTasks(categories);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static void markActive(Context context, String taskID) {
		DatabaseAdapter a = new DatabaseAdapter(context);
		try {
			a.Open();
			a.markTaskActive(taskID);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
	}
}