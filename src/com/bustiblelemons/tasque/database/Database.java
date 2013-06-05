package com.bustiblelemons.tasque.database;

import static com.bustiblelemons.tasque.utilities.Values.TAG;
import it.bova.rtmapi.DeletedTask;
import it.bova.rtmapi.SynchedTasks;
import it.bova.rtmapi.Task;
import it.bova.rtmapi.TaskList;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.util.Pair;

import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.main.SettingsUtil;
import com.bustiblelemons.tasque.rtm.RTMBackend;
import com.bustiblelemons.tasque.utilities.Values.Database.Categories;
import com.bustiblelemons.tasque.utilities.Values.Database.Notes;
import com.bustiblelemons.tasque.utilities.Values.Database.Table;
import com.bustiblelemons.tasque.utilities.Values.Database.Task.TaskState;
import com.bustiblelemons.tasque.utilities.Values.Database.Tasks;
import com.bustiblelemons.tasque.utilities.Values.FragmentArguments;

public class Database {
	public static long cacheCategoriesDeleted(Context context, Collection<String> listIds) {
		long r = 0;
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			for (String listId : listIds) {
				r += a.cacheCategory(listId) > 0 ? 1 : 0;
			}
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static long cacheCateogryRenamed(Context context, String listId, String listName) {
		Database.deleteCategory(context, listId);
		long r = 0;
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			r = a.cacheCategoryRenamed(listId, listName);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static long cacheNewCategory(Context context, String categoryName) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		long r = -1;
		try {
			a.Open();
			a.newCategory(categoryName);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static long cacheNewNote(Context context, String taskId, String body) {
		long r = 0;
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			r = a.cacheNewNote(taskId, body);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static long cacheNewTask(Context context, String taskName, String categoryID) {
		long r = 0;
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			r = a.newTask(taskName, null, categoryID);
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		} finally {
			a.Close();
		}
		return r;
	}

	public static long cacheNewTask(Context context, String taskName, String taskId, String categoryID, int state) {
		long r = 0l;
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			r = a.newTask(taskName, taskId, categoryID, state);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	private static int cacheNotes(Context context, Cursor cursor) {
		int r = 0;
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			a.cacheNotes(cursor);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static void cacheNotesDeleted(Context context, Collection<String> noteIds) {
		Cursor notes = Database.mergeNotesCursor(Database.getCachedNotes(context, noteIds),
				Database.getLocalNotes(context, noteIds));
		if (notes != null) {
			Database.cacheNotes(context, notes);
		} else {
			Log.d(TAG, "Cursor of notes for caching was null");
		}
	}

	public static long cacheTaskRenamed(Context context, Cursor taskCursor, String name) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		long r = 0;
		try {
			a.Open();
			r = a.cacheTaskRenamed(taskCursor, name);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;

	}

	public static int cacheTaskRenamed(Context context, String taskId, String taskName) {
		int r = Database.markLocalTaskDeleted(context, taskId);
		if (r > 0) {
			Cursor taskCursor = Database.getTaskCursor(context, taskId);
			Database.cacheTaskRenamed(context, taskCursor, taskName);
		}
		return r;
	}

	public static void cacheTasksDeleted(Context context, Collection<String> tasksToDelete) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		Cursor cur = null;
		try {
			a.Open();
			cur = a.getTasks(tasksToDelete);
			a.Close();
			a = DatabaseAdapter.cacheDatabase(context);
			a.Open();
			a.cacheTasks(cur, TaskState.Deleted);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
	}

	public static void createCacheDatabase(Context context) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		String creationCategories = context.getString(R.string.cache_creation_string_table_categories);
		String creationTasks = context.getString(R.string.cache_creation_string_table_tasks);
		String creationNotes = context.getString(R.string.cache_creation_string_table_notes);
		try {
			a.Open();
			a.execQuery(creationCategories);
			a.execQuery(creationTasks);
			a.execQuery(creationNotes);
		} catch (SQLiteException e) {
			a.Close();
			e.printStackTrace();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		} finally {
			a.Close();
		}
	}

	public static void createFreshDatabase(Context context, boolean insertCategories) {
		Log.d(TAG, "Creating a fresh database");
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		String creationCategories = context.getString(R.string.creation_string_table_categories);
		String creationTasks = context.getString(R.string.creation_string_table_tasks);
		String creationNotes = context.getString(R.string.creation_string_table_notes);
		try {
			a.Open();
			a.execQuery(creationCategories);
			a.execQuery(creationTasks);
			a.execQuery(creationNotes);
			if (insertCategories) {
				a.execQuery(context.getString(R.string.insert_category_family));
				a.execQuery(context.getString(R.string.insert_category_personal));
				a.execQuery(context.getString(R.string.insert_category_project));
				a.execQuery(context.getString(R.string.insert_category_work));
			}
		} catch (SQLiteException e) {
			a.Close();
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			a.Close();
		}
	}

	public static void deleteCachedCategory(Context context, String categoryId) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			a.deleteCategory(categoryId);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
	}

	public static void deleteCachedNote(Context context, String noteId) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			a.deleteNotes(noteId);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
	}


	public static void deleteCachedTasks(Context context, String taskId, String categoryId) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			a.deleteTask(taskId);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
	}

	public static int deleteCategories(Context context, Collection<String> categoriesIDs) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		int r = 0;
		try {
			a.Open();
			for (String categoryID : categoriesIDs) {
				try {
					r += a.deleteCategory(categoryID);
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
			}
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	private static void deleteCategory(Context context, String listId) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			a.deleteCategory(listId);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
	}

	public static int deleteNotes(Context context, String taskId, Collection<String> noteIds) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		int r = 0;
		try {
			a.Open();
			for (String noteId : noteIds) {
				r += a.deleteNote(taskId, noteId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static void deleteTasks(Context context, List<DeletedTask> deleted) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			for (DeletedTask task : deleted) {
				a.deleteTask(task.getId());
			}
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
	}

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

	public static Cursor getCachedCategories(Context context) {
		Cursor r = null;
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			r = a.getCategoriesCursor();
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	private static Cursor getCachedCompletedTasks(Context context, String categoryID) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		Cursor r = null;
		try {
			a.Open();
			if (categoryID.equals(String.valueOf(FragmentArguments.ALL_ID))) {
				ArrayList<String> categories = SettingsUtil.getSelectedCategories(context);
				Log.d(TAG, "getAllSelectedCompletedTasks : " + categories.size());
				r = a.getCompletedTasks(categories);
			} else {
				r = a.getCompletedTasks(categoryID);
			}
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static Cursor getCachedNotes(Context context) {
		Cursor r = null;
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			r = a.getNotes();
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	private static Cursor getCachedNotes(Context context, Collection<String> noteIds) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		Cursor notes = null;
		try {
			a.Open();
			notes = a.getNotesCursor(noteIds);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return notes;
	}

	public static Cursor getCachedNotes(Context context, String taskId) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		Cursor r = null;
		try {
			a.Open();
			r = a.getCachedNotes(taskId);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}			
		return r;
	}

	public static Cursor getCachedTasks(Context context) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		Cursor r = null;
		try {
			a.Open();
			r = a.getTasks();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		} finally {
			a.Close();
		}
		return r;
	}

	public static Cursor getCachedTasks(Context context, String listId) {
		Cursor r = null;
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			r = a.getTasks(listId);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static Cursor getCategories(Context context) {
		if (RTMBackend.useRTM(context)) {
			return Database.mergeCategoriesCursor(Database.getCachedCategories(context),
					Database.getLocalCategories(context));
		} else {
			return Database.getLocalCategories(context);
		}
	}

	public static ArrayList<String> getCategoriesIds(Context context) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		ArrayList<String> IDs = new ArrayList<String>();
		try {
			a.Open();
			IDs = a.getCategoryIds();
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return IDs;
	}

	public static ArrayList<Pair<Integer, String>> getCategoriesList(Context context) {
		Cursor cur = null;
		cur = Database.getCategories(context);
		ArrayList<Pair<Integer, String>> r = new ArrayList<Pair<Integer, String>>();
		Pair<Integer, String> pair = new Pair<Integer, String>(FragmentArguments.ALL_ID, FragmentArguments.ALL);
		r.add(pair);
		while (cur.moveToNext()) {
			int id = cur.getInt(cur.getColumnIndex(Table.ID));
			String name = cur.getString(cur.getColumnIndex(Table.NAME));
			if (name.equals(RTMBackend.ALL_TASKS_CATEGORY)) {
				r.remove(0);
			}
			r.add(new Pair<Integer, String>(id, name));
		}
		return r;
	}

	public static String getCategoryId(Context context, String categoryName) throws SQLException {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		String r = "";
		try {
			a.Open();
			r = a.getCategoryId(categoryName);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static Cursor getCompletedTasks(Context context, String categoryID) {
		if (RTMBackend.useRTM(context)) {
			return Database.mergeTasksCursor(Database.getLocalTasksCompleted(context, categoryID),
					Database.getCachedCompletedTasks(context, categoryID));
		} else {
			return Database.getLocalTasksCompleted(context, categoryID);
		}
	}

	private static Cursor getLocalCategories(Context context) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		Cursor cur = null;
		try {
			a.Open();
			cur = a.getCategoriesCursor();
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return cur;
	}

	private static Cursor getLocalNotes(Context context, Collection<String> noteIds) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		Cursor notes = null;
		try {
			a.Open();
			notes = a.getNotesCursor(noteIds);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return notes;
	}

	private static Cursor getLocalNotes(Context context, String taskId) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		Cursor r = null;
		try {
			a.Open();
			r = a.getNotes(taskId);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}


	private static Cursor getLocalTasks(Context context) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		Cursor r = null;
		try {
			a.Open();
			r = a.getTasks();
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	private static Cursor getLocalTasks(Context context, String categoryId) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		Cursor r = null;
		try {
			a.Open();
			r = a.getTasks(categoryId);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	private static Cursor getLocalTasksCompleted(Context context, String categoryID) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		Cursor r = null;
		try {
			a.Open();
			if (categoryID.equals(String.valueOf(FragmentArguments.ALL_ID))) {
				ArrayList<String> categories = SettingsUtil.getSelectedCategories(context);
				Log.d(TAG, "getAllSelectedCompletedTasks : " + categories.size());
				r = a.getCompletedTasks(categories);
			} else {
				r = a.getCompletedTasks(categoryID);
			}
			a.Close();
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		}
		return r;
	}

	public static Cursor getNotes(Context context, String taskId) {
		return RTMBackend.useRTM(context) ? Database.mergeNotesCursor(Database.getCachedNotes(context, taskId),
				Database.getLocalNotes(context, taskId)) : Database.getLocalNotes(context, taskId);
	}

	public static Cursor getTaskCursor(Context context, String taskId) {
		Cursor r = null;
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			r = a.getTaskCursor(taskId);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static Cursor getTasks(Context context) {
		if (RTMBackend.useRTM(context)) {
			return Database.mergeTasksCursor(Database.getCachedTasks(context), Database.getLocalTasks(context));
		} else {
			return Database.getLocalTasks(context);
		}
	}

	public static Cursor getTasks(Context context, String categoryId) {
		if (RTMBackend.useRTM(context)) {
			return Database.mergeTasksCursor(Database.getCachedTasks(context, categoryId),
					Database.getLocalTasks(context, categoryId));
		} else {
			return Database.getLocalTasks(context, categoryId);
		}
	}
	/**
	 * 
	 * @param context
	 * @param lists
	 * @return number of new lists inserted
	 */
	public static int importCategories(Context context, List<TaskList> lists) {
		int r = 0;
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			for (TaskList list : lists) {
				r += a.insertList(list);
			}
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static int importSyncedTasks(Context context, SynchedTasks synchedTasks) {
		int r = 0;
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			List<Task> tasks = synchedTasks.getTasks();
			for (Task task : tasks) {
				r += a.newOrUpdateTask(task);
			}
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static void importTask(Context context, Task task, String listId) {
		List<Task> _task = new LinkedList<Task>();
		_task.add(task);
		Database.importTasks(context, _task, listId);
	}

	public static int importTasks(Context context, List<Task> taskList, String listID) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		int r = 0;
		try {
			a.Open();
			r = a.importTaskList(taskList, listID);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static void insertDefaultCategories(Context context) {
		Log.d(TAG, "Inserting default categories");
		String insertWork = context.getString(R.string.insert_category_work);
		String insertFamily = context.getString(R.string.insert_category_family);
		String insertPersonal = context.getString(R.string.insert_category_personal);
		String insertProject = context.getString(R.string.insert_category_project);
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			a.execQuery(insertWork);
			a.execQuery(insertFamily);
			a.execQuery(insertPersonal);
			a.execQuery(insertProject);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
	}

	private static int markCachedTasksDeleted(Context context, Collection<String> taskIDs) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		int r = 0;
		try {
			a.Open();
			for (String taskId : taskIDs) {
				try {
					Log.d(TAG, "Marking ID " + taskId + " as deleted");
					r += a.setTaskDeleted(taskId);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			a.Close();
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		}
		return r;
	}

	private static int markLocalTaskActive(Context context, String taskId) {
		int r = 0;
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			r = a.setTaskActive(taskId);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	/**
	 * @param context
	 * @param id
	 * @return AMount of rows updated via this statement.
	 */
	private static int markLocalTaskCompleted(Context context, String id) {
		int r = 0;
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			r = a.setTaskCompleted(id);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	private static int markLocalTaskDeleted(Context context, String taskId) {
		int r = 0;
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			r = a.setTaskDeleted(taskId);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	private static Collection<String> markLocalTasksDeleted(Context context, Collection<String> taskIDs) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		Collection<String> t = new ArrayList<String>(taskIDs);
		try {
			a.Open();
			for (String taskId : taskIDs) {
				try {
					Log.d(TAG, "Marking ID " + taskId + " as deleted");
					int aff = a.setTaskDeleted(taskId);
					if (aff > 0) {
						t.remove(taskId);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			a.Close();
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		}
		return t;
	}

	public static int markTaskActive(Context context, String taskId) {
		int r = Database.markLocalTaskActive(context, taskId);
		if (r == 0) {
			return Database.setTaskStatus(context, TaskState.Active, taskId);
		}
		return r;
	}

	public static int markTaskDone(Context context, String id) {
		int r = Database.markLocalTaskCompleted(context, id);
		if (r == 0) {
			return Database.setTaskStatus(context, TaskState.Completed, id);
		}
		return r;
	}

	public static int markTasksDeleted(Context context, Collection<String> taskIDs) {
		Collection<String> left = Database.markLocalTasksDeleted(context, taskIDs);
		return left.size() > 0 ? Database.markCachedTasksDeleted(context, left) : 0;
	}

	private static Cursor mergeCategoriesCursor(Cursor... taskCursors) {
		return Database.mergeCursor(new String[] { Categories.ID, Categories.NAME, Categories.EXTERNALID, Tasks.ADDED,
				Tasks.DELETED }, taskCursors);
	}

	private static Cursor mergeCursor(String[] columns, Cursor... taskCursors) {
		MatrixCursor r = new MatrixCursor(columns);
		for (Cursor cursor : taskCursors) {
			if (cursor != null) {
				while (cursor.moveToNext()) {
					ArrayList<Object> columnValues = new ArrayList<Object>();
					for (int i = 0; i < r.getColumnCount(); i++) {
						try {
							columnValues.add(cursor.getString(cursor.getColumnIndexOrThrow(r.getColumnName(i))));
						} catch (IndexOutOfBoundsException e) {
							e.printStackTrace();
						} catch (IllegalArgumentException e) {
							// Log.d(TAG, "Missing collumn, adding empty");
							columnValues.add("");
						}
					}
					r.addRow(columnValues);
				}
			}
		}
		return r;
	}

	private static Cursor mergeNotesCursor(Cursor... taskCursors) {
		return Database.mergeCursor(new String[] { Notes.ID, Notes.TASK, Notes.TEXT, Notes.EXTERNALID, Tasks.ADDED,
				Tasks.DELETED }, taskCursors);
	}

	private static Cursor mergeTasksCursor(Cursor... taskCursors) {
		return Database.mergeCursor(new String[] { Table.ID, Tasks.CATEGORY, Table.NAME, Tasks.DUE_DATE,
				Tasks.COMPLETION_DATE, Tasks.PRIORITY, Tasks.STATE, Table.EXTERNALID, Tasks.ADDED, Tasks.DELETED },
				taskCursors);
	}

	public static long newCategory(Context context, String categoryName) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		long r = -1;
		try {
			a.Open();
			r = a.newCategory(categoryName);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static long newCategory(Context context, String categoryName, String listId) {
		long r = 0;
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			r = a.newCategory(categoryName, listId);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static long newNote(Context context, String taskId, String body) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		long r = -1;
		try {
			a.Open();
			r = a.newNote(taskId, body);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static long newNote(Context context, String taskId, String noteId, String text) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		long r = -1;
		try {
			a.Open();
			r = a.newNote(taskId, noteId, text);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	/**
	 * 
	 * @param context
	 * @param taskName
	 * @param categoryId
	 * @return
	 */
	public static long newTask(Context context, String taskName, String categoryId) {
		return Database.newTask(context, taskName, null, categoryId);
	}

	/**
	 * 
	 * @param context
	 * @param taskId
	 * @param taskName
	 * @param categoryId
	 * @return
	 */
	public static long newTask(Context context, String taskName, String taskId, String categoryId) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		long r = 0;
		try {
			a.Open();
			if (categoryId.equals(String.valueOf(FragmentArguments.ALL_ID))) {
				int defCategory = SettingsUtil.getDefaultCategoryId(context);
				categoryId = defCategory > 0 ? String.valueOf(defCategory) : String.valueOf(1);
			}
			Log.d(TAG, "Adding " + taskName + " in category " + categoryId);
			r = a.newTask(taskName, taskId, categoryId);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static long newTask(Context context, String taskName, String taskId, String listId, int state) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		long r = 0;
		try {
			a.Open();
			if (listId.equals(String.valueOf(FragmentArguments.ALL_ID))) {
				int defCategory = SettingsUtil.getDefaultCategoryId(context);
				listId = defCategory > 0 ? String.valueOf(defCategory) : String.valueOf(1);
			}
			Log.d(TAG, "Adding " + taskName + " in category " + listId);
			r = a.newTask(taskName, taskId, listId, state);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static void setCategoryId(Context context, String categoryName, String oldListId, String listId) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			a.updateCategoryId(categoryName, oldListId, listId);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
	}

	public static int setCategoryName(Context context, String listId, String categoryName) {
		int r = 0;
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			r = a.setCategoryName(listId, categoryName);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static int setTaskStatus(Context context, int state, String taskId) {
		int r = 0;
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			r = a.setTaskStatus(taskId, state);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
		return r;
	}

	public static void updateCachedNote(Context context, String noteId, String body) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		try {
			a.Open();
			a.updateNote(noteId, body);
			a.Close();
		} catch (SQLException e) {
			a.Close();
			e.printStackTrace();
		}
	}

	public static int updateCachedTask(Context context, String id, String name) {
		DatabaseAdapter a = DatabaseAdapter.cacheDatabase(context);
		int r = 0;
		try {
			a.Open();
			r = a.updateTaskName(id, name);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static int updateLocalTask(Context context, String id, String name) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		int r = 0;
		try {
			a.Open();
			r = a.updateTaskName(id, name);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static void updateNote(Context context, String noteId, String body) {
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			a.updateNote(noteId, body);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
	}

	public static int updateNoteId(Context context, String newNoteId, String taskId, String body) {
		int r = 0;
		DatabaseAdapter a = DatabaseAdapter.localDatabase(context);
		try {
			a.Open();
			r = a.updateNoteId(taskId, newNoteId, body);
		} catch (SQLException e) {
			e.printStackTrace();
			a.Close();
		} finally {
			a.Close();
		}
		return r;
	}

	public static int updateTask(Context context, String id, String name) {
		int r = Database.updateLocalTask(context, id, name);
		if (r == 0) {
			r = Database.updateCachedTask(context, id, name);
		}
		return r;
	}
}