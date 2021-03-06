package com.bustiblelemons.tasque.rtm;

import static com.bustiblelemons.tasque.utilities.Values.TAG;
import it.bova.rtmapi.DeletedTask;
import it.bova.rtmapi.Note;
import it.bova.rtmapi.Priority;
import it.bova.rtmapi.RtmApi;
import it.bova.rtmapi.RtmApiAuthenticator;
import it.bova.rtmapi.RtmApiException;
import it.bova.rtmapi.RtmApiTransactable;
import it.bova.rtmapi.ServerException;
import it.bova.rtmapi.Settings;
import it.bova.rtmapi.SynchedTasks;
import it.bova.rtmapi.Task;
import it.bova.rtmapi.TaskList;
import it.bova.rtmapi.Token;
import it.bova.rtmapi.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.settings.SettingsUtil;
import com.bustiblelemons.tasque.utilities.PriorityParser;
import com.bustiblelemons.tasque.utilities.Values.Database.Categories;
import com.bustiblelemons.tasque.utilities.Values.Database.Notes;
import com.bustiblelemons.tasque.utilities.Values.Database.Task.CategoryState;
import com.bustiblelemons.tasque.utilities.Values.Database.Task.TaskState;
import com.bustiblelemons.tasque.utilities.Values.Database.Tasks;

/**
 * Facade for using the RTMApi. Designed as a singleton use. Timeline,
 * Authenticator and Transactable are single objects. Initiated at first run.
 * 
 * @author bhm
 * 
 */
public class RTMBackend {

	public static final Object ALL_TASKS_CATEGORY = "All Tasks";

	private static final int TITLE_LENGTH = 13;

	private static RtmApiAuthenticator authenticator;

	// private static LinkedList<Object> listOfTransactions;

	private static String Timeline;

	private static RTMBackend INSTANCE;

	private static Token token;

	private static RtmApiTransactable Transactable;

	private static ExecutorService executorService;

	public static void cacheTask(Context context, String taskName, String taskId, String categoryId, int state) {
		Database.cacheNewTask(context, taskName, taskId, categoryId, state);
	}

	private static boolean categoryExists(List<TaskList> taskLists, String listId) {
		for (TaskList list : taskLists) {
			if (list.getId().equals(listId)) {
				Log.d(TAG, "Detected " + list.getName());
				return true;
			}
		}
		return false;
	}

	private static String getCategoryId(List<TaskList> tasksList, String listName) {
		String r = "";
		for (TaskList list : tasksList) {
			if (list.getName().equals(listName)) {
				r = list.getId();
				Log.d(TAG, "Found " + r);
				return r;
			}
		}
		return r;
	}

	public static void deleteList(Context context, String listId) throws IllegalArgumentException, ServerException,
			RtmApiException, IOException {
		getTransactable(context).listsDelete(getTimeline(context), listId);
	}

	public static void deleteLists(final Context context, final Collection<String> listIds) {
		RTMBackend.executorInstance().submit(new Runnable() {
			@Override
			public void run() {
				for (String listId : listIds) {
					try {
						RTMBackend.deleteList(context, listId);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (ServerException e) {
						e.printStackTrace();
					} catch (RtmApiException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public static void deleteNotes(final Context context, final Collection<String> noteIds) {
		RTMBackend.executorInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					Log.d(TAG, "Notes delete " + noteIds.size());
					RtmApiTransactable t = RTMBackend.getTransactable(context);
					for (String noteId : noteIds) {
						Log.d(TAG, "Will delete a note with id: " + noteId);
						t.tasksDeleteNote(getTimeline(context), noteId);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (ServerException e) {
					e.printStackTrace();
				} catch (RtmApiException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static void deleteTask(Context context, String taskId, String listId) throws IllegalArgumentException,
			ServerException, RtmApiException, IOException {
		getTransactable(context).tasksDelete(getTimeline(context), taskId, getTaskSerieId(context, taskId, listId),
				listId);
	}

	public static void deleteTasks(final Context context, final Iterable<String> taskIds, final String listId) {
		RTMBackend.executorInstance().submit(new Runnable() {
			@Override
			public void run() {
				for (String taskId : taskIds) {
					try {
						Log.d(TAG, "removing task " + taskId + " list " + listId);
						RTMBackend.deleteTask(context, taskId, listId);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (ServerException e) {
						e.printStackTrace();
					} catch (RtmApiException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	private synchronized static ExecutorService executorInstance() {
		return RTMBackend.executorService == null ? RTMBackend.executorService = Executors.newSingleThreadExecutor()
				: RTMBackend.executorService;
	}

	@SuppressWarnings("unused")
	private synchronized static RtmApiAuthenticator getAuthenticator() {
		return authenticator == null ? authenticator = new RtmApiAuthenticator(Milk.API_KEY, Milk.API_SECRET)
				: authenticator;
	}

	public static String getDefaultListId(Context context) {
		String r = "";
		try {
			r = RTMBackend.getUserSettings(context).getDefaultListId();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ServerException e) {
			e.printStackTrace();
		} catch (RtmApiException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return r;
	}

	public synchronized static RTMBackend getInstance() {
		return INSTANCE == null ? INSTANCE = new RTMBackend() : INSTANCE;
	}

	/**
	 * 
	 * @param context
	 * @param taskId
	 * @return
	 * @throws IllegalArgumentException
	 * @throws ServerException
	 * @throws RtmApiException
	 * @throws IOException
	 */
	public static String getTaskSerieId(Context context, String taskId, String listId) throws IllegalArgumentException,
			ServerException, RtmApiException, IOException {
		List<Task> tasks = getTransactable(context).tasksGetList();
		for (Task task : tasks) {
			if (task.getId().equals(taskId)) {
				Log.d(TAG, "Found " + task.getTaskserieId());
				return task.getTaskserieId();
			}
		}
		return "";
	}

	private synchronized static String getTimeline(Context context) throws IllegalArgumentException, ServerException,
			RtmApiException, IOException {
		return Timeline == null ? Timeline = getTransactable(context).timelinesCreate() : Timeline;
	}

	private synchronized static Token getToken(Context context) {
		return RTMBackend.token == null ? RTMBackend.token = SettingsUtil.getRTMToken(context) : RTMBackend.token;
	}

	synchronized static RtmApiTransactable getTransactable(Context context) throws IllegalArgumentException {
		Token token = RTMBackend.getToken(context);
		if (token == null) {
			throw new IllegalArgumentException("Token returned from settings is null");
		}
		return RTMBackend.Transactable == null ? RTMBackend.Transactable = new RtmApiTransactable(Milk.API_KEY,
				Milk.API_SECRET, token) : RTMBackend.Transactable;
	}

	synchronized static RtmApiTransactable getTransactable(Token token) {
		RTMBackend.token = token;
		return RTMBackend.Transactable == null ? RTMBackend.Transactable = new RtmApiTransactable(Milk.API_KEY,
				Milk.API_SECRET, RTMBackend.token) : RTMBackend.Transactable;
	}

	public static Settings getUserSettings(Context context) throws IllegalArgumentException, ServerException,
			RtmApiException, IOException {
		return getTransactable(context).settingsGetList();
	}

	public static String newList(Context context, String listName) {
		try {
			return getTransactable(context).listsAdd(getTimeline(context), listName).getObject().getId();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ServerException e) {
			e.printStackTrace();
		} catch (RtmApiException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static Future<Note> newNote(final Context context, final String listId, final String taskId,
			final String body) {
		Callable<Note> worker = new Callable<Note>() {
			@Override
			public Note call() throws Exception {
				Transaction<Note> noteTrans = null;
				try {
					String taskseriesId = RTMBackend.getTaskSerieId(context, taskId, listId);
					String title = body.length() > RTMBackend.TITLE_LENGTH - 1 ? body.substring(0,
							RTMBackend.TITLE_LENGTH) : body;
					noteTrans = RTMBackend.getTransactable(context).tasksAddNote(getTimeline(context), taskId,
							taskseriesId, listId, title, body);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (ServerException e) {
					e.printStackTrace();
				} catch (RtmApiException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return noteTrans.getObject();
			}
		};
		return RTMBackend.executorInstance().submit(worker);
	}

	public static Transaction<Task> newTask(Context context, String taskName, String listId) {
		Transaction<Task> r = null;
		try {
			r = getTransactable(context).tasksAdd(getTimeline(context), taskName, listId);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ServerException e) {
			e.printStackTrace();
		} catch (RtmApiException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return r;
	}

	/**
	 * 
	 * @param context
	 * @return amount of rows update or inserted
	 */
	public static int refreshLists(Context context) {
		int r = 0;
		RtmApi api = new RtmApi(Milk.API_KEY, Milk.API_SECRET, SettingsUtil.getRTMToken(context));
		SynchedTasks synchedTasks;
		try {
			Date lastSyncDate = SettingsUtil.getRTMLastSync(context);
			if (lastSyncDate != null) {
				synchedTasks = api.tasksGetSynchedList(lastSyncDate);
				r = Database.importSyncedTasks(context, synchedTasks);
			}
		} catch (ServerException e) {
			e.printStackTrace();
		} catch (RtmApiException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return r;
	}

	public static void setDefaultListId(final Context context, final String listId) {
		RTMBackend.executorInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					getTransactable(context).listsSetDefault(getTimeline(context), listId);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (ServerException e) {
					e.printStackTrace();
				} catch (RtmApiException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static void setListName(final Context context, final String listId, final String listName) {
		RTMBackend.executorInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					RTMBackend.getTransactable(context).listsSetName(RTMBackend.getTimeline(context), listId, listName);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (ServerException e) {
					e.printStackTrace();
				} catch (RtmApiException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static void setTaskName(final Context context, final String listId, final String taskId, final String name) {
		RTMBackend.executorInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					String taskseriesId = getTaskSerieId(context, taskId, listId);
					RTMBackend.getTransactable(context).tasksSetName(getTimeline(context), taskId, taskseriesId,
							listId, name);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (ServerException e) {
					e.printStackTrace();
				} catch (RtmApiException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private static void setTaskPriority(Context context, Priority priority, String taskId, String listId,
			String taskseriesId) throws IllegalArgumentException, ServerException, RtmApiException, IOException {
		RTMBackend.getTransactable(context).tasksSetPriority(RTMBackend.getTimeline(context), taskId, taskseriesId,
				listId, priority);
	}

	/**
	 * 
	 * @param context
	 * @param state
	 * @param taskId
	 * @param listId
	 */
	public static void setTaskState(final Context context, final int state, final String taskId, final String listId) {
		RTMBackend.executorInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					String serieID = RTMBackend.getTaskSerieId(context, taskId, listId);
					switch (state) {
					case TaskState.Active:
						getTransactable(context).tasksUncomplete(getTimeline(context), taskId, serieID, listId);
						break;
					case TaskState.Completed:
						Log.d(TAG, "TaskState.Completed.\nTask id: " + taskId + "\nList id: " + listId + "\nSerie ID: "
								+ serieID);
						getTransactable(context).tasksComplete(getTimeline(context), taskId, serieID, listId);
						break;
					case TaskState.Deleted:
						RTMBackend.deleteTask(context, taskId, listId);
						break;
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (ServerException e) {
					e.printStackTrace();
				} catch (RtmApiException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Uploads all cached data to RTM
	 * 
	 * @param context
	 */
	static Collection<String> synchronizeCache(Context context) {
		HashSet<String> ret = new HashSet<String>();
		ret.addAll(RTMBackend.uploadCategories(context, true));
		return ret;
	}

	static int synchronizeFromServer(Context context, Date lastSync) {
		int r = 0;
		try {
			SynchedTasks synchedTasks = getTransactable(context).tasksGetSynchedList(lastSync);
			Log.d(TAG, "From Server retrived " + synchedTasks.getTasks().size());
			List<DeletedTask> deleted = synchedTasks.getDeletedTasks();
			Database.deleteTasks(context, deleted);
			r = Database.importSyncedTasks(context, synchedTasks);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ServerException e) {
			e.printStackTrace();
		} catch (RtmApiException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return r;
	}

	/**
	 * Gets lists from the server and updates local.
	 * 
	 * @param context
	 * @return true of the number of lists changed
	 */
	static boolean synchronizeLists(Context context) {
		int r = 0;
		try {
			ArrayList<String> localIds = Database.getCategoriesIds(context);
			List<TaskList> onlineLists = RTMBackend.getTransactable(context).listsGetList();
			List<TaskList> listsToImport = new LinkedList<TaskList>(onlineLists);
			Collection<String> listsToDelete = new ArrayList<String>(localIds);
			String id;
			for (TaskList list : onlineLists) {
				id = list.getId();
				if (localIds.contains(id)) {
					listsToDelete.remove(id);
					listsToImport.remove(list);
				}
			}
			Log.d(TAG, "New Lists");
			for (TaskList list : listsToImport) {
				Log.d(TAG, list.getName());
			}
			Log.d(TAG, "To delete Lists");
			for (String s : listsToDelete) {
				Log.d(TAG, "Id to delete: " + s);
			}
			r = Database.importCategories(context, listsToImport);
			r += Database.deleteCategories(context, listsToDelete);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ServerException e) {
			e.printStackTrace();
		} catch (RtmApiException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return r > 0 ? true : false;
	}

	public static void updateNote(final Context context, final String noteId, final String body) {
		RTMBackend.executorInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					Log.d(TAG, "notesUpdate(context, " + noteId + ", " + body);
					String title = body.length() > RTMBackend.TITLE_LENGTH - 1 ? body.substring(0,
							RTMBackend.TITLE_LENGTH) : body;
					RTMBackend.getTransactable(context).tasksEditNote(getTimeline(context), noteId, title, body);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (ServerException e) {
					e.printStackTrace();
				} catch (RtmApiException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Uploads tasks from the cache (cached=true) or from the main database
	 * (cached=false) and updates relevant rows.
	 * 
	 * @param context
	 * @param cached
	 *            true to use cache database, false to use the main database.
	 * @return
	 */
	public static Collection<String> uploadCategories(Context context, boolean cached) {
		HashSet<String> ret = new HashSet<String>();
		String listName;
		String listId;
		Cursor cursor = cached ? Database.getCachedCategories(context) : Database.getLocalCategories(context);
		List<TaskList> taskLists = RTMBackend.getLists(context);
		while (cursor.moveToNext()) {
			listName = cursor.getString(cursor.getColumnIndex(Categories.NAME));
			listId = cursor.getString(cursor.getColumnIndex(Categories.ID));
			try {
				if (RTMBackend.categoryExists(taskLists, listId)) {
					RTMBackend.getTransactable(context).listsSetName(getTimeline(context), listId, listName);
					Database.setCategoryName(context, listId, listName);
					Log.d(TAG, "RENAMING LIST " + listId + "\tName:" + listName);
				} else {
					int state = 0;
					try {
						state = cursor.getInt(cursor.getColumnIndex(Categories.STATE));
					} catch (Exception e) {
					}
					if (state == CategoryState.Deleted) {
						Log.d(TAG, "DELETING LIST " + listId + "\t" + listName);
						RTMBackend.getTransactable(context).listsDelete(getTimeline(context), listId);
						Database.deleteCachedCategory(context, listId);
					} else {
						String onlineTaskListId = RTMBackend.getCategoryId(taskLists, listName);
						if (cached || (!cached && onlineTaskListId.length() == 0)) {
							Log.d(TAG, "UPLOADING NEW LIST " + listName);
							Transaction<TaskList> t = getTransactable(context).listsAdd(getTimeline(context), listName);
							TaskList taskList = t.getObject();
							onlineTaskListId = taskList.getId();
							if (cached) {
								Database.setCachedCategoryId(context, listName, listId, onlineTaskListId);
							} else {
								Database.setCategoryId(context, listName, listId, onlineTaskListId);
							}
						}
					}
				}
				ret.add(listId);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (ServerException e) {
				e.printStackTrace();
			} catch (RtmApiException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	/**
	 * 
	 * @param context
	 * @param taskId
	 * @param taskseriesId
	 * @param listId
	 * @param localTaskId
	 * @param cached
	 */
	public static void uploadNotes(final Context context, final String taskId, final String taskseriesId,
			final String listId, final String localTaskId, boolean cached) {
		Cursor cur = cached ? Database.getCachedNotes(context, localTaskId) : Database.getLocalNotes(context,
				localTaskId);
		while (cur.moveToNext()) {
			String text = cur.getString(cur.getColumnIndex(Notes.TEXT));
			String title = text.length() > RTMBackend.TITLE_LENGTH - 1 ? text.substring(0, RTMBackend.TITLE_LENGTH)
					: text;
			try {
				Log.d(TAG, "UPLOADING NOTE FOR " + taskId + "\t" + title + "\t" + text);
				Transaction<Note> noteTransaction = RTMBackend.getTransactable(context).tasksAddNote(
						getTimeline(context), taskId, taskseriesId, listId, title, text);
				String noteId = cur.getString(cur.getColumnIndex(Notes.ID));
				Note note = noteTransaction.getObject();
				Database.newNote(context, taskId, note.getId(), note.getText());
				if (cached) {
					Database.deleteCachedNote(context, noteId);
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (ServerException e) {
				e.printStackTrace();
			} catch (RtmApiException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Uploads tasks from the cache (cached=true) or from the main database
	 * (cached=false). Updates the ID. Automatically polls for relevant notes.
	 * 
	 * @param context
	 * @param cached
	 *            pass false to upload already present tasks in the main
	 *            database.
	 */
	public static int uploadTasks(Context context, boolean cached) {
		int r = 0;
		String taskName;
		String taskId;
		String listId;
		Cursor tasks = cached ? Database.getCachedTasks(context) : Database.getLocalTasks(context);
		Log.d(TAG, "uploadTask(" + cached + ")\n" + tasks.getColumnCount() + " " + tasks.getCount());
		int state;
		int priority;
		while (tasks.moveToNext()) {
			taskId = tasks.getString(tasks.getColumnIndex(Tasks.ID));
			listId = tasks.getString(tasks.getColumnIndex(Tasks.CATEGORY));
			taskName = tasks.getString(tasks.getColumnIndex(Tasks.NAME));
			state = tasks.getInt(tasks.getColumnIndex(Tasks.STATE));
			priority = tasks.getInt(tasks.getColumnIndex(Tasks.PRIORITY));
			try {
				Log.d(TAG, "UPLOADING ListId: " + listId + "\tTaskName: " + taskName + "\tTaskId:" + taskId
						+ "\tState: " + state + "\tPriority:" + PriorityParser.parse(priority));
				Transaction<Task> taskTransaction = RTMBackend.getTransactable(context).tasksAdd(
						RTMBackend.getTimeline(context), taskName, listId);
				Task task = taskTransaction.getObject();
				String onlineTaskId = task.getId();
				String taskseriesId = task.getTaskserieId();
				RTMBackend.setTaskState(context, state, onlineTaskId, listId);
				RTMBackend.uploadNotes(context, onlineTaskId, taskseriesId, listId, taskId, cached);
				RTMBackend.setTaskPriority(context, PriorityParser.parse(priority), onlineTaskId, listId, taskseriesId);
				r++;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (ServerException e) {
				e.printStackTrace();
			} catch (RtmApiException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return r;
	}

	public static boolean useRTM(Context context) {
		return SettingsUtil.useRTMBackend(context) && SettingsUtil.rtmAccountConfigured(context);
	}

	private RTMBackend() {
	}

	public static List<TaskList> getLists(Context context) {
		List<TaskList> r = new LinkedList<TaskList>();
		try {
			r = RTMBackend.getTransactable(context).listsGetList();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ServerException e) {
			e.printStackTrace();
		} catch (RtmApiException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return r;
	}

}