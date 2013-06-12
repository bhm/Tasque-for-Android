package com.bustiblelemons.tasque.utilities;

public class Values {

	public static final String TAG = "Tasque";
	public static final int IMPORTER_REQUEST_CODE = 0x0000009;

	public static final class JSONToken {
		public static final String Permission = "permission";
		public static final String Token = "token";
		public static final String UserID = "userID";
		public static final String UserName = "userName";
		public static final String FullUserName = "fullUserName";
	}

	public static final class TasqueArguments {
		public static final String SHOW_DATABASE_CHOOSER = "databaseChooser";
		public static final String LOST_DATABASE = "lostDatabase";
	}

	public static final class ImporterArguments {
		public static final String SYNCED_DATABASE_PATH = "syncedDatabasePath";
		public static final String START_FRESH = "startFresh";
		public static final String USE_REMEMBER_THE_MILK = "useRememberTheMilk";
	}

	public static final class Database {
		public static class Table {
			public static final String ID = "ID";
			public static final String NAME = "Name";
			public static final String EXTERNALID = "ExternalID";
			/**
			 * RTM CACHE FIELDS
			 */
			public static final String ADDED = "Added";
			public static final String DELETED = "Deleted";
			public static final String RENAMED = "Renamed";
		}

		public static final class Task {
			public static class TaskState {
				public final static int Active = 0;
				public final static int Incomplete = 1;
				public final static int Completed = 2;
				public final static int Discarded = 3;
				public final static int Deleted = 4;
				public final static int Renamed = 5;
			}

			public static final class CategoryState extends TaskState {

			}

			public static final class Priority {
				public final static int Unspecified = 0;
				public final static int High = 1;
				public final static int Medium = 2;
				public final static int Low = 3;
			}
		}

		public static final class Categories extends Table {
			public static final String STATE = "State";
		}

		public static final class Notes extends Table {
			public static final String TASK = "Task";
			public static final String TEXT = "Text";
			public static final String STATE = "State";

			public static final class NoteState {
				public final static int Added = 3;
				public final static int Deleted = 4;
				public final static int Renamed = 5;
			}
		}

		public static final class Tasks extends Table {
			// public static final long INDEFINED_DATE = -62135600400l;
			/**
			 * @param INDEFINED_DATE
			 *            date is stored as Int64 in the Tasque desktop. Sqlite3
			 *            API cuts down bits no matter what
			 * 
			 */
			public static final long INDEFINED_DATE = -2006058256l;
			public static final String CATEGORY = "Category";
			public static final String DUE_DATE = "DueDate";
			public static final String COMPLETION_DATE = "CompletionDate";
			public static final String PRIORITY = "Priority";
			public static final String STATE = "State";
		}
	}

	public static final class FragmentArguments {
		public static final String CATEGORY = "category";
		public static final String ID = "id";
		public static final String ALL = "All";
		public static final int ALL_ID = -1;
		public static final String TASK_NAME = "taskName";

		public static final class RTMAuthArguments {
			public static final String AUTH_URL = "auth_url";
		}

		public static final class MultipleFiles {
			public static final String FILES_FOUND = "files_found";
			public static final String LOST_PREVIOUS = "lost_previous";
		}

		public static final class OsInfoFragment {
			public static final String OS_TYPE = "OS_Type";
			public static final int OS_COUNT = 4;

			public static final class OS {
				public static final int LINUX = 0;
				public static final int ANDROID = 1;
				public static final int MAC = 2;
				public static final int WINDOWS = 3;
			}
		}

		public static final String LIST_ID = "ListId";
	}

}
