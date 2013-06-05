package com.bustiblelemons.tasque.database;

import static com.bustiblelemons.tasque.utilities.Values.TAG;
import it.bova.rtmapi.Note;
import it.bova.rtmapi.Task;
import it.bova.rtmapi.TaskList;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.bustiblelemons.tasque.utilities.Values.Database.Categories;
import com.bustiblelemons.tasque.utilities.Values.Database.Notes;
import com.bustiblelemons.tasque.utilities.Values.Database.Notes.NoteState;
import com.bustiblelemons.tasque.utilities.Values.Database.Task.CategoryState;
import com.bustiblelemons.tasque.utilities.Values.Database.Task.Priority;
import com.bustiblelemons.tasque.utilities.Values.Database.Task.TaskState;
import com.bustiblelemons.tasque.utilities.Values.Database.Tasks;

public class DatabaseAdapter {
	private class DatabaseHelper extends SQLiteOpenHelper {
		private String databaseName;

		public DatabaseHelper(Context context, String databaseName) {
			super(context, databaseName, null, DATABASE_VERSION);
			this.databaseName = databaseName;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.v(TAG, "Created a database named " + databaseName);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onCreate(db);
		}
	}

	public static DatabaseAdapter cacheDatabase(Context context) {
		return new DatabaseAdapter(context, RTM_CACHE_DATBASE_NAME);
	}

	public static DatabaseAdapter localDatabase(Context context) {
		return new DatabaseAdapter(context, LOCAL_DATBASE_NAME);
	}

	@SuppressWarnings("unused")
	private Context context;

	public final static String DATABASE_NAME = "sqlitebackend.db";
	private final static String LOCAL_DATBASE_NAME = "sqlitebackend.db";
	private final static String RTM_CACHE_DATBASE_NAME = "rtmcache.db";

	private final static String TABLE_NOTES = "Notes";

	private final static String TABLE_TASKS = "Tasks";
	private final static String TABLE_CATEGORIES = "Categories";

	private final int DATABASE_VERSION = 1;

	private SQLiteDatabase database;

	private DatabaseHelper databaseHelper;

	DatabaseAdapter(Context context) {
		this.context = context;
		databaseHelper = new DatabaseHelper(context, LOCAL_DATBASE_NAME);
	}

	DatabaseAdapter(Context context, String databaseName) {
		this.context = context;
		databaseHelper = new DatabaseHelper(context, databaseName);
	}

	public long cacheCategory(String listId) {
		ContentValues values = new ContentValues();
		values.put(Categories.STATE, TaskState.Deleted);
		values.put(Categories.ID, listId);
		return database.insert(TABLE_CATEGORIES, null, values);
	}

	public long cacheCategoryRenamed(String listId, String listName) {
		ContentValues values = new ContentValues();
		values.put(Categories.ID, listId);
		values.put(Categories.NAME, listName);
		values.put(Categories.STATE, CategoryState.Renamed);
		return database.replace(TABLE_CATEGORIES, null, values);
	}

	long cacheNewNote(String taskId, String body) {
		ContentValues values = new ContentValues();
		values.put(Notes.TEXT, body);
		values.put(Notes.TASK, taskId);
		values.put(Notes.STATE, NoteState.Added);
		return database.insert(TABLE_NOTES, null, values);
	}

	public int cacheNotes(Cursor notes) {
		int r = 0;
		while (notes.moveToNext()) {
			ContentValues values = this.getValues(notes);
			values.put(Tasks.DELETED, 1);
			r += database.replace(TABLE_NOTES, null, values) > 0 ? 1 : 0;
		}
		return r;
	}

	long cacheTaskRenamed(Cursor taskCursor, String name) {
		ContentValues values = getValues(taskCursor);
		values.put(Tasks.NAME, name);
		values.put(Tasks.STATE, TaskState.Renamed);
		return database.insert(TABLE_TASKS, null, values);
	}

	public int cacheTasks(Cursor cur, int state) {
		int r = 0;
		ContentValues values;
		while (cur.moveToNext()) {
			values = getValues(cur);
			values.put(Tasks.STATE, state);
			r = database.insert(TABLE_TASKS, null, values) > 0 ? 1 : 0;
		}
		return r;
	}

	boolean categoryExists(String listId) {
		return database.query(TABLE_CATEGORIES, null, Categories.ID + "=?", new String[] { listId }, null, null, null)
				.getCount() > 0 ? true : false;
	}

	boolean categoryNamedExists(String name) {
		return database.query(TABLE_CATEGORIES, null, Categories.NAME + "=?", new String[] { name }, null, null, null)
				.getCount() > 0 ? true : false;
	}

	void Close() {
		databaseHelper.close();
	}

	int deleteCategory(String categoryId) {
		database.delete(TABLE_TASKS, Tasks.CATEGORY + "=?", new String[] { categoryId });
		this.moveTasksToInbox(categoryId);
		return database.delete(TABLE_CATEGORIES, Categories.ID + "=?", new String[] { categoryId });
	}
	
	private int moveTasksToInbox(String categoryId) {
		ContentValues values = new ContentValues();
		String inboxId = getCategoryId("Inbox");
		values.put(Tasks.CATEGORY, inboxId);
		return database.update(TABLE_TASKS, values, Tasks.CATEGORY + "=?", new String[] { categoryId });
	}

	int deleteNote(String taskID, String noteId) {
		String whereClause = Notes.TASK + "=? AND " + Notes.ID + "=?";
		return database.delete(TABLE_NOTES, whereClause, new String[] { taskID, noteId });
	}

	int deleteNotes(String taskId) {
		return database.delete(TABLE_NOTES, Notes.TASK + "=?", new String[] { taskId });
	}

	int deleteTask(String taskId) {
		return database.delete(TABLE_TASKS, Tasks.ID + "=?", new String[] { taskId });
	}

	void execQuery(String sql) {
		database.execSQL(sql);
	}

	Cursor getCachedNotes(String taskID) {
		Cursor cur = database.query(TABLE_NOTES, null, Notes.TASK + "=? AND " + Notes.STATE + "<>?", new String[] {
				taskID, String.valueOf(NoteState.Deleted) }, null, null, null);
		Log.d(TAG, "Number of notes: " + cur.getCount());
		return cur;
	}

	protected int getCategoriesCount() {
		return database.query(TABLE_CATEGORIES, null, null, null, null, null, null).getCount();
	}

	protected Cursor getCategoriesCursor() {
		Cursor ret = database.query(TABLE_CATEGORIES, null, null, null, null, null, Categories.NAME + " ASC");
		Log.d(TAG, "Categories: " + ret.getCount() + " x " + ret.getColumnCount());
		return ret;
	}

	protected String getCategoryId(String categoryName) {
		Cursor cur = database.query(TABLE_CATEGORIES, new String[] { Categories.ID }, Categories.NAME + "=?",
				new String[] { categoryName }, null, null, null);
		Log.d(TAG, "Getting task identifier for category " + categoryName);
		Log.d(TAG, "Cursor: " + cur.getCount() + " X " + cur.getColumnCount());
		cur.moveToFirst();
		return cur.getString(cur.getColumnIndex(Categories.ID));
	}

	ArrayList<String> getCategoryIds() {
		Cursor cur = database.query(TABLE_CATEGORIES, new String[] { Categories.ID }, null, null, null, null, null);
		Log.d(TAG, "Recovered number of categories: " + cur.getCount());
		ArrayList<String> r = new ArrayList<String>();
		while (cur.moveToNext()) {
			r.add(cur.getString(cur.getColumnIndex(Categories.ID)));
		}
		return r;
	}

	String getCategoryName(int categoryId) {
		Cursor cur = database.query(TABLE_CATEGORIES, null, Categories.ID + "=?",
				new String[] { String.valueOf(categoryId) }, null, null, null);
		Log.d(TAG, cur.getCount() + " " + cur.getColumnCount());
		return cur.getString(0);
	}

	Cursor getCompletedTasks(Iterable<String> categories) {
		MatrixCursor ret = new MatrixCursor(new String[] { Tasks.ID, Tasks.CATEGORY, Tasks.NAME, Tasks.DUE_DATE,
				Tasks.COMPLETION_DATE, Tasks.PRIORITY, Tasks.STATE, Tasks.EXTERNALID });
		for (String cat : categories) {
			Cursor cursor = this.getCompletedTasks(cat);
			while (cursor.moveToNext()) {
				ArrayList<Object> columnValues = new ArrayList<Object>();
				for (int i = 0; i < cursor.getColumnCount(); i++) {
					try {
						columnValues.add(cursor.getString(cursor.getColumnIndexOrThrow(ret.getColumnName(i))));
					} catch (IndexOutOfBoundsException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						columnValues.add("");
					}
				}
				ret.addRow(columnValues);
			}
		}
		return ret;
	}

	Cursor getCompletedTasks(String categoryID) {
		Cursor r = database.query(TABLE_TASKS, null, Tasks.STATE + "=? AND " + Tasks.CATEGORY + "=?", new String[] {
				String.valueOf(TaskState.Completed), categoryID }, null, null, Tasks.COMPLETION_DATE + " ASC");
		Log.d(TAG, "Completed tasks: " + r.getCount());
		return r;
	}

	String getNote(String taskID) {
		return database.query(TABLE_NOTES, new String[] { Notes.TEXT }, Notes.ID + "=?", new String[] { taskID }, null,
				null, null).getString(0);
	}

	public Cursor getNotes() {
		Cursor r = database.query(TABLE_NOTES, null, null, null, null, null, null);
		return r;
	}

	Cursor getNotes(String taskID) {
		Cursor cur = database.query(TABLE_NOTES, null, Notes.TASK + "=?", new String[] { taskID }, null, null, null);
		Log.d(TAG, "Number of notes: " + cur.getCount());
		return cur;
	}

	public Cursor getNotesCursor(Collection<String> ids) {
		StringBuilder b = new StringBuilder();
		Iterator<String> it = ids.iterator();
		while (it.hasNext()) {
			it.next();
			b.append(Notes.ID).append("=?");
			if (it.hasNext()) {
				b.append(" OR ");
			}
		}
		Cursor r = database.query(TABLE_NOTES, null, b.toString(), ids.toArray(new String[ids.size()]), null, null,
				null);
		Log.d(TAG, "Notes cursor size for cache " + r.getColumnCount() + " " + r.getCount());
		return r;
	}

	Cursor getTaskCursor(String taskId) {
		Cursor cur = database.query(TABLE_TASKS, null, Tasks.ID + "=?", new String[] { taskId }, null, null, null);
		Log.d(TAG, "For rename: " + cur.getColumnCount() + " " + cur.getCount());
		return cur;
	}

	String getTaskName(String taskId) {
		Cursor cur = database.query(TABLE_TASKS, new String[] { Tasks.NAME }, Tasks.ID + "=?", new String[] { taskId },
				null, null, null);
		if (cur.moveToFirst()) {
			return cur.getString(0);
		} else {
			return "";
		}
	}

	Cursor getTasks() {
		Cursor cur = database.query(TABLE_TASKS, null, Tasks.STATE + "<>?",
				new String[] { String.valueOf(TaskState.Deleted) }, null, null, null);
		Log.d(TAG, "Cursor for all tasks " + cur.getCount() + " X " + cur.getColumnCount());
		return cur;
	}

	Cursor getTasks(Collection<String> ids) {
		StringBuilder b = new StringBuilder();
		Iterator<String> it = ids.iterator();
		while (it.hasNext()) {
			it.next();
			b.append(Tasks.ID + "=?");
			if (it.hasNext()) {
				b.append(" OR ");
			}
		}
		Cursor r = database.query(TABLE_TASKS, null, b.toString(), ids.toArray(new String[ids.size()]), null, null,
				null);
		Log.d(TAG, "Tasks size for cache " + r.getColumnCount() + " " + r.getCount());
		return r;
	}

	Cursor getTasks(int categoryId) {
		Cursor cursor = database.query(TABLE_TASKS, null, Tasks.CATEGORY + "=? AND " + Tasks.STATE + "=? OR "
				+ Tasks.CATEGORY + "=? AND " + Tasks.STATE + "=?",
				new String[] { String.valueOf(categoryId), String.valueOf(TaskState.Active),
						String.valueOf(categoryId), String.valueOf(TaskState.Renamed) }, null, null, Tasks.NAME
						+ " ASC");
		Log.d(TAG, "\nGetting tasks. Category " + categoryId + "\nSize: " + cursor.getCount());
		return cursor;
	}

	Cursor getTasks(Iterable<String> categories) {
		MatrixCursor ret = new MatrixCursor(new String[] { Tasks.ID, Tasks.CATEGORY, Tasks.NAME, Tasks.DUE_DATE,
				Tasks.COMPLETION_DATE, Tasks.PRIORITY, Tasks.STATE, Tasks.EXTERNALID });
		for (String cat : categories) {
			Cursor cursor = this.getTasks(Integer.valueOf(cat));
			while (cursor.moveToNext()) {
				ArrayList<Object> columnValues = new ArrayList<Object>();
				for (int i = 0; i < cursor.getColumnCount(); i++) {
					try {
						columnValues.add(cursor.getString(cursor.getColumnIndexOrThrow(ret.getColumnName(i))));
					} catch (IndexOutOfBoundsException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						columnValues.add("");
					}
				}
				ret.addRow(columnValues);
			}
		}
		return ret;
	}

	Cursor getTasks(String listId) {
		return this.getTasks(Integer.valueOf(listId));
	}

	private ContentValues getValues(Cursor cur) {
		ContentValues values = new ContentValues();
		Log.d(TAG, "Extracting values " + cur.getColumnCount() + " " + cur.getCount());
		cur.moveToFirst();
		for (int i = 0; i < cur.getColumnCount(); i++) {
			values.put(cur.getColumnName(i), cur.getString(i));
		}
		return values;
	}

	private ContentValues getValues(Task task) {
		ContentValues values = new ContentValues();
		values.put(Tasks.ID, task.getId());
		values.put(Tasks.NAME, task.getName());
		if (task.getDue() != null) {
			values.put(Tasks.DUE_DATE, task.getDue().getTime() / 1000);
		}
		if (task.getCompleted() != null) {
			values.put(Tasks.COMPLETION_DATE, task.getCompleted().getTime() / 1000);
			values.put(Tasks.STATE, TaskState.Completed);
		} else if (task.getDeleted() != null) {
			values.put(Tasks.STATE, TaskState.Deleted);
		} else {
			values.put(Tasks.STATE, TaskState.Active);
			values.put(Tasks.DUE_DATE, Tasks.INDEFINED_DATE);
			values.put(Tasks.COMPLETION_DATE, Tasks.INDEFINED_DATE);
		}
		return values;
	}

	int importTaskList(List<Task> taskList, String listId) {
		int r = 0;
		for (Task task : taskList) {
			Log.d(TAG, "Inserting a task: " + task.getName() + " to a list " + listId);
			if (task.getNotes() != null) {
				insertNotes(task);
			}
			ContentValues values = getValues(task);
			values.put(Tasks.CATEGORY, listId);
			database.replace(TABLE_TASKS, null, values);
		}
		return r;
	}

	/**
	 * RTM BACKEND
	 */
	/**
	 * 
	 * @param categories
	 * @return total of inserted lists. Updates don't count
	 */
	int insertCategories(List<TaskList> categories) {
		int r = 0;
		for (TaskList list : categories) {
			ContentValues values = new ContentValues();
			if (this.categoryNamedExists(list.getName())) {
				Log.d(TAG, "Category " + list.getName() + " DOES exists. Update.");
				values.put(Categories.ID, list.getId());
				database.update(TABLE_CATEGORIES, values, Categories.NAME + "=?", new String[] { list.getName() });
			} else {
				Log.d(TAG, "Category " + list.getName() + " DOES NOT exists. Insert!");
				values.put(Categories.ID, list.getId());
				values.put(Categories.NAME, list.getName());
				r += database.insert(TABLE_CATEGORIES, null, values) > 0 ? 1 : 0;
			}
		}
		return r;
	}

	public int insertList(TaskList list) {
		ContentValues values = new ContentValues();
		int r = 0;
		if (this.categoryNamedExists(list.getName())) {
			Log.d(TAG, "Category " + list.getName() + " DOES exists. Update.");
			values.put(Categories.ID, list.getId());
			database.update(TABLE_CATEGORIES, values, Categories.NAME + "=?", new String[] { list.getName() });
		} else {
			Log.d(TAG, "Category " + list.getName() + " DOES NOT exists. Insert!");
			values.put(Categories.ID, list.getId());
			values.put(Categories.NAME, list.getName());
			r += database.insert(TABLE_CATEGORIES, null, values) > 0 ? 1 : 0;
		}
		return r;
	}

	void insertNotes(Task task) {
		database.delete(TABLE_NOTES, Notes.TASK + "=?", new String[] { task.getId() });
		for (Note note : task.getNotes()) {
			Log.d(TAG, "Inserting notes for " + task.getName() + "\nNote: " + note.getText());
			ContentValues noteValues = new ContentValues();
			noteValues.put(Notes.TASK, task.getId());
			noteValues.put(Notes.NAME, note.getTitle());
			noteValues.put(Notes.TEXT, note.getText());
			int a = database.update(TABLE_NOTES, noteValues, Notes.TASK + "=? AND " + Notes.ID + "=?", new String[] {
					task.getId(), note.getId() });
			if (a == 0) {
				noteValues.put(Notes.ID, note.getId());
				database.insertWithOnConflict(TABLE_NOTES, null, noteValues, SQLiteDatabase.CONFLICT_IGNORE);
			}
		}
	}

	long newCategory(String categoryName) {
		Cursor check = database.query(TABLE_CATEGORIES, null, Categories.NAME + "=?", new String[] { categoryName },
				null, null, null, null);
		if (check.getCount() > 0) {
			return -1;
		}
		ContentValues values = new ContentValues();
		values.put(Categories.NAME, categoryName);
		return database.insert(TABLE_CATEGORIES, null, values);
	}

	long newCategory(String listName, String listId) {
		ContentValues values = new ContentValues();
		values.put(Categories.ID, listId);
		values.put(Categories.NAME, listName);
		return database.insert(TABLE_CATEGORIES, null, values);
	}

	long newNote(String taskID, String body) {
		ContentValues values = new ContentValues();
		values.put(Notes.TEXT, body);
		values.put(Notes.TASK, taskID);
		return database.insert(TABLE_NOTES, null, values);
	}

	long newNote(String taskId, String noteId, String text) {
		ContentValues values = new ContentValues();
		values.put(Notes.TEXT, text);
		values.put(Notes.TASK, taskId);
		values.put(Notes.ID, noteId);
		return database.insert(TABLE_NOTES, null, values);
	}

	/**
	 * Inserts new or replaces old tasks.
	 * 
	 * @param task
	 * @return number of inserted tasks
	 */
	int newOrUpdateTask(Task task) {
		int r = 0;
		ContentValues values = getValues(task);
		if (task.getNotes() != null) {
			insertNotes(task);
		}
		values.put(Tasks.CATEGORY, task.getListId());
		database.replace(TABLE_TASKS, null, values);
		return r;
	}

	long newTask(String taskName, String taskId, String categoryId) {
		return newTask(taskName, taskId, categoryId, TaskState.Active);
	}

	long newTask(String taskName, String taskId, String categoryId, int state) {
		ContentValues values = new ContentValues();
		if (taskId != null) {
			values.put(Tasks.ID, Integer.valueOf(taskId));
		}
		values.put(Tasks.CATEGORY, categoryId);
		values.put(Tasks.NAME, taskName);
		values.put(Tasks.DUE_DATE, Tasks.INDEFINED_DATE);
		values.put(Tasks.COMPLETION_DATE, Tasks.INDEFINED_DATE);
		values.put(Tasks.STATE, state);
		values.put(Tasks.PRIORITY, Priority.Unspecified);
		return database.insert(TABLE_TASKS, null, values);
	}

	DatabaseAdapter Open() throws SQLException {
		database = databaseHelper.getWritableDatabase();
		return this;
	}

	public int setCategoryName(String listId, String listName) {
		ContentValues values = new ContentValues();
		values.put(Categories.ID, listId);
		values.put(Categories.NAME, listName);
		return database.update(TABLE_CATEGORIES, values, Categories.ID + "=?", new String[] { listId });
	}

	int setTaskActive(String taskId) {
		ContentValues values = new ContentValues();
		Log.d(TAG, "Marking ID: " + taskId + " as active");
		values.put(Tasks.STATE, TaskState.Active);
		values.put(Tasks.COMPLETION_DATE, Tasks.INDEFINED_DATE);
		return database.update(TABLE_TASKS, values, Tasks.ID + "=?", new String[] { taskId });
	}

	int setTaskCompleted(String taskId) {
		Log.d(TAG, "Marking ID " + taskId + " as done");
		return setTaskStatus(taskId, TaskState.Completed, System.currentTimeMillis());
	}

	/**
	 * This should mark as 3 in State column. Never delete.
	 * 
	 * @param task
	 * @return
	 */
	int setTaskDeleted(String taskID) throws SQLException {
		return setTaskStatus(taskID, TaskState.Deleted);
	}

	int setTaskPriority(String id, int priority) {
		ContentValues values = new ContentValues();
		values.put(Tasks.PRIORITY, priority);
		return database.update(TABLE_TASKS, values, Tasks.ID + "=?", new String[] { id });
	}

	int setTasksDeleted(String listId) {
		ContentValues values = new ContentValues();
		values.put(Tasks.STATE, TaskState.Deleted);
		return database.update(TABLE_TASKS, values, Tasks.CATEGORY + "=?", new String[] { listId });
	}

	int setTaskStatus(String id, int status) {
		ContentValues values = new ContentValues();
		values.put(Tasks.STATE, status);
		return database.update(TABLE_TASKS, values, Tasks.ID + "=?", new String[] { id });
	}

	int setTaskStatus(String id, int status, long date) {
		ContentValues values = new ContentValues();
		values.put(Tasks.COMPLETION_DATE, date / 1000);
		values.put(Tasks.STATE, status);
		return database.update(TABLE_TASKS, values, Tasks.ID + "=?", new String[] { id });
	}

	int updateCategoryId(String categoryName, String oldListId, String categoryId) {
		ContentValues values = new ContentValues();
		values.put(Categories.ID, categoryId);
		return database.update(TABLE_CATEGORIES, values, Categories.ID + "=?", new String[] { oldListId });
	}

	int updateNote(String noteId, String body) {
		ContentValues values = new ContentValues();
		values.put(Notes.TEXT, body);
		return database.update(TABLE_NOTES, values, Notes.ID + "=?", new String[] { noteId });
	}

	int updateNoteId(String taskId, String newNoteId, String body) {
		ContentValues values = new ContentValues();
		values.put(Notes.ID, newNoteId);
		Log.d(TAG, "Updating for\tId: " + taskId + "\tBody: " + body);
		return database.update(TABLE_NOTES, values, Notes.TASK + "=? AND " + Notes.TEXT + "=?", new String[] { taskId,
				body });
	}

	/**
	 * 
	 * @param id
	 * @param name
	 *            new value for a name;
	 * @return
	 */
	int updateTaskName(String id, String name) {
		ContentValues values = new ContentValues();
		values.put(Tasks.NAME, name);
		return database.update(TABLE_TASKS, values, Tasks.ID + "=?", new String[] { id });
	}
}