package com.bustiblelemons.tasque.utilities;

public class Values {

	public static final String TAG = "Tasque";
	public static final int IMPORTER_REQUEST_CODE = 0x0000009;

	public static final class TasqueArguments {
		public static final String SHOW_DATABASE_CHOOSER = "databaseChooser";
		public static final String LOST_DATABASE = "lostDatabase";
	}

	public static final class ImporterArguments {
		public static final String SYNCED_DATABASE_PATH = "syncedDatabasePath";
		public static final String START_FRESH = "startFresh";
		public static final String USE_REMEMBER_THE_MILK = "useRememberTheMilk";
	}

	public static final class FragmentFlags {
		public static final String NOTES_FRAGMENT = "notes_fragment";
		public static final String IMPORTER_FRAGMENT = "importer_fragment";
		public static final String CATEGORIES_FRAGMENT = "show_in_all_fragment";
		public static final String MULTIPLEFILES_FRAGMENT = "multiplefiles_fragment";
		public static final String COMPLETED_TASKS_FRAGMENT = "completed_tasks_fragment";
	}

	public static final class Database {
		public static final class Task {
			public static final class State {
				public final static int Active = 0;
				public final static int Incomplete = 1;
				public final static int Completed = 2;
				public final static int Discarded = 3;
				public final static int Deleted = 3;
			}

			public static final class Priority {
				public final static int Unspecified = 0;
				public final static int High = 1;
				public final static int Medium = 2;
				public final static int Low = 3;
			}
		}

		public static class Table {
			public static final String ID = "ID";
			public static final String NAME = "Name";
			public static final String EXTERNALID = "ExternalID";
		}

		public static final class Categories extends Table {

		}

		public static final class Notes extends Table {
			public static final String TASK = "Task";
			public static final String TEXT = "Text";
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
			public static final String ON_DATE = "onDate";
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

		public static final class MultipleFiles {
			public static final String FILES_FOUND = "files_found";
			public static final String LOST_PREVIOUS = "lostPrevious";
		}

		public static final class OsInfoFragment {
			public static final String OS_TYPE = "OS_Type";
			public static final int OS_COUNT = 3;

			public static final class OS {
				public static final int LINUX = 0;
				public static final int MAC = 1;
				public static final int WINDOWS = 2;
			}
		}
	}

}
