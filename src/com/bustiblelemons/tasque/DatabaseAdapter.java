package com.bustiblelemons.tasque;

import static com.bustiblelemons.tasque.Values.TAG;

import java.sql.SQLException;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.bustiblelemons.tasque.Values.Database.Categories;
import com.bustiblelemons.tasque.Values.Database.Notes;
import com.bustiblelemons.tasque.Values.Database.Task;
import com.bustiblelemons.tasque.Values.Database.Tasks;

public class DatabaseAdapter {
	@SuppressWarnings("unused")
	private Context context;
	final static String DATABASE_NAME = "sqlitebackend.db";

	private final static String TABLE_NOTES = "Notes";
	private final static String TABLE_TASKS = "Tasks";
	private final static String TABLE_CATEGORIES = "Categories";

	private final int DATABASE_VERSION = 1;

	private SQLiteDatabase database;
	private DatabaseHelper databaseHelper;

	public DatabaseAdapter(Context context) {
		this.context = context;
		databaseHelper = new DatabaseHelper(context);
	}

	private class DatabaseHelper extends SQLiteOpenHelper {
		private String mDATABASE_NAME = null;

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.v(TAG, "Created a database named " + mDATABASE_NAME);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onCreate(db);
		}
	}

	public DatabaseAdapter Open() throws SQLException {
		database = databaseHelper.getWritableDatabase();
		return this;
	}

	public void Close() {
		databaseHelper.close();
	}

	public void execQuery(String sql) {
		database.execSQL(sql);
	}

	protected int getNumberOfCategories() {
		return database.query(TABLE_CATEGORIES, null, null, null, null, null, null).getCount();
	}

	protected Cursor getCategories() {
		Cursor ret = database.query(TABLE_CATEGORIES, new String[] { Categories.ID, Categories.NAME }, null, null,
				null, null, Categories.NAME + " ASC");
		Log.d(TAG, "Categories: " + ret.getCount() + " x " + ret.getColumnCount());
		return ret;
	}

	protected int getCategoryIdentifier(String categoryName) {
		Cursor cur = database.query(TABLE_CATEGORIES, new String[] { Categories.ID }, Categories.NAME + "=?",
				new String[] { categoryName }, null, null, null);
		Log.d(TAG, "Getting task identifier for category " + categoryName);
		Log.d(TAG, "Cursor: " + cur.getCount() + " X " + cur.getColumnCount());
		return 0;
	}

	protected Cursor getTasks(int cateogryIdentifier) {
		Cursor cursor = database.query(TABLE_TASKS, null, Tasks.CATEGORY + "=? AND " + Tasks.STATE + "=?",
				new String[] { String.valueOf(cateogryIdentifier), String.valueOf(Task.State.Active) }, null, null,
				Tasks.NAME + " ASC");
		Log.d(TAG, "\nGetting tasks. Category " + cateogryIdentifier + "\nSize: " + cursor.getCount());
		return cursor;
	}

	protected Cursor getTasks(Iterable<String> categories) {
		MatrixCursor ret = new MatrixCursor(new String[] { Tasks.ID, Tasks.CATEGORY, Tasks.NAME, Tasks.DUE_DATE,
				Tasks.COMPLETION_DATE, Tasks.PRIORITY, Tasks.STATE, Tasks.EXTERNALID });
		for (String cat : categories) {
			Cursor cursor = this.getTasks(Integer.valueOf(cat));
			while (cursor.moveToNext()) {
				ArrayList<Object> columnValues = new ArrayList<Object>();
				try {
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.ID)));
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.CATEGORY)));
					columnValues.add(cursor.getString(cursor.getColumnIndex(Tasks.NAME)));
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.DUE_DATE)));
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.COMPLETION_DATE)));
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.PRIORITY)));
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.STATE)));
					columnValues.add(cursor.getString(cursor.getColumnIndex(Tasks.EXTERNALID)));
				} catch (IndexOutOfBoundsException e) {
					e.printStackTrace();
				}
				ret.addRow(columnValues);
			}
		}
		return ret;
	}

	protected Cursor getCompletedTasks(Iterable<String> categories) {
		MatrixCursor ret = new MatrixCursor(new String[] { Tasks.ID, Tasks.CATEGORY, Tasks.NAME, Tasks.DUE_DATE,
				Tasks.COMPLETION_DATE, Tasks.PRIORITY, Tasks.STATE, Tasks.EXTERNALID });
		for (String cat : categories) {
			Cursor cursor = this.getCompletedTasks(cat);
			while (cursor.moveToNext()) {
				ArrayList<Object> columnValues = new ArrayList<Object>();
				try {
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.ID)));
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.CATEGORY)));
					columnValues.add(cursor.getString(cursor.getColumnIndex(Tasks.NAME)));
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.DUE_DATE)));
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.COMPLETION_DATE)));
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.PRIORITY)));
					columnValues.add(cursor.getInt(cursor.getColumnIndex(Tasks.STATE)));
					columnValues.add(cursor.getString(cursor.getColumnIndex(Tasks.EXTERNALID)));
				} catch (IndexOutOfBoundsException e) {
					e.printStackTrace();
				}
				ret.addRow(columnValues);
			}
		}
		return ret;
	}

	public Cursor getCompletedTasks(String categoryID) {
		Cursor r = database.query(TABLE_TASKS, null, Tasks.STATE + "=? AND " + Tasks.CATEGORY + "=?", new String[] {
				String.valueOf(Task.State.Completed), categoryID }, null, null, Tasks.COMPLETION_DATE + " ASC");
		Log.d(TAG, "Completed tasks: " + r.getCount());
		return r;
	}

	protected Cursor getAllTasks() {
		Cursor cur = database.query(TABLE_TASKS, null, Tasks.STATE + "<>?",
				new String[] { String.valueOf(Task.State.Deleted) }, null, null, null);
		Log.d(TAG, "Cursor for all " + cur.getCount() + " X " + cur.getColumnCount());
		return cur;

	}

	public String getCategoryName(int categoryId) {
		Cursor cur = database.query(TABLE_CATEGORIES, null, Categories.ID + "=?",
				new String[] { String.valueOf(categoryId) }, null, null, null);
		Log.d(TAG, cur.getCount() + " " + cur.getColumnCount());
		return cur.getString(0);
	}

	public long createNewCategory(String categoryName) {
		Cursor check = database.query(TABLE_CATEGORIES, null, Categories.NAME + "=?", new String[] { categoryName },
				null, null, null, null);
		if (check.getCount() > 0) {
			return -1;
		}
		ContentValues values = new ContentValues();
		values.put(Categories.NAME, categoryName);
		return database.insert(TABLE_CATEGORIES, null, values);
	}

	public ArrayList<String> getAllCategoriesIDs() {
		Cursor cur = database.query(TABLE_CATEGORIES, new String[] { Categories.ID }, null, null, null, null, null);
		Log.d(TAG, "Recovered number of categories: " + cur.getCount());
		ArrayList<String> r = new ArrayList<String>();
		while (cur.moveToNext()) {
			r.add(cur.getString(cur.getColumnIndex(Categories.ID)));
		}
		return r;
	}

	public int deleteCategory(String categoryID) {
		database.delete(TABLE_TASKS, Tasks.CATEGORY + "=?", new String[] { categoryID });
		return database.delete(TABLE_CATEGORIES, Categories.ID + "=?", new String[] { categoryID });
	}

	public int setTaskStatus(String id, int status, long date) {
		ContentValues values = new ContentValues();
		values.put(Tasks.COMPLETION_DATE, date / 1000);
		values.put(Tasks.STATE, status);
		return database.update(TABLE_TASKS, values, Tasks.ID + "=?", new String[] { id });
	}

	public int setTaskStatus(String id, int status) {
		ContentValues values = new ContentValues();
		values.put(Tasks.STATE, status);
		return database.update(TABLE_TASKS, values, Tasks.ID + "=?", new String[] { id });
	}

	public int markTaskDiscarded(String id) {
		return setTaskStatus(id, Task.State.Discarded);
	}

	public int markTaskDone(String id) {
		Log.d(TAG, "Marking ID " + id + " as done");
		return setTaskStatus(id, Task.State.Completed, System.currentTimeMillis());
	}

	public int markTaskActive(String id) {
		ContentValues values = new ContentValues();
		Log.d(TAG, "Marking ID: " + id + " as active");
		values.put(Tasks.STATE, Task.State.Active);
		values.put(Tasks.COMPLETION_DATE, Tasks.INDEFINED_DATE);
		return database.update(TABLE_TASKS, values, Tasks.ID + "=?", new String[] { id });
	}

	public int setPriority(String id, int priority) {
		ContentValues values = new ContentValues();
		values.put(Tasks.PRIORITY, priority);
		return database.update(TABLE_TASKS, values, Tasks.ID + "=?", new String[] { id });
	}

	/**
	 * This should mark as 3 in State column. Never delete.
	 * 
	 * @param task
	 * @return
	 */
	public int markDeleted(String taskID) throws SQLException {
		return setTaskStatus(taskID, Task.State.Deleted);
	}

	/**
	 * 
	 * @param id
	 * @param name
	 *            new value for a name;
	 * @return
	 */
	public int updateTask(String id, String name) {
		ContentValues values = new ContentValues();
		values.put(Tasks.NAME, name);
		return database.update(TABLE_TASKS, values, Tasks.ID + "=?", new String[] { id });
	}

	public int updateTaskNote(String id, String oldNote, String note) {
		ContentValues values = new ContentValues();
		values.put(Notes.TEXT, note);
		return database.update(TABLE_NOTES, values, Notes.TASK + "=? AND " + Notes.TEXT + "=?", new String[] { id,
				oldNote });
	}

	public Cursor getNotes(String taskID) {
		Cursor cur = database.query(TABLE_NOTES, null, Notes.TASK + "=?", new String[] { taskID }, null, null, null);
		Log.d(TAG, "Number of notes: " + cur.getCount());
		return cur;
	}

	public String getNote(String taskID) {
		return database.query(TABLE_NOTES, new String[] { Notes.TEXT }, Notes.ID + "=?", new String[] { taskID }, null,
				null, null).getString(0);
	}

	public long insertNote(String taskID, String body) {
		ContentValues values = new ContentValues();
		values.put(Notes.TEXT, body);
		values.put(Notes.TASK, taskID);
		return database.insert(TABLE_NOTES, null, values);
	}

	public int deleteNote(String taskID, String note) {
		String whereClause = Notes.TASK + "=? AND " + Notes.TEXT + "=?";
		return database.delete(TABLE_NOTES, whereClause, new String[] { taskID, note });
	}

	public long insertTask(String categoryID, String task) {
		ContentValues values = new ContentValues();
		values.put(Tasks.CATEGORY, categoryID);
		values.put(Tasks.NAME, task);
		values.put(Tasks.DUE_DATE, Tasks.INDEFINED_DATE);
		values.put(Tasks.COMPLETION_DATE, Tasks.INDEFINED_DATE);
		values.put(Tasks.STATE, Task.State.Active);
		values.put(Tasks.PRIORITY, Task.Priority.Unspecified);
		return database.insert(TABLE_TASKS, null, values);
	}
}
