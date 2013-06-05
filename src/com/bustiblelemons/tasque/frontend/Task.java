package com.bustiblelemons.tasque.frontend;

import it.bova.rtmapi.Transaction;

import java.util.Collection;

import android.content.Context;

import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.rtm.RTMBackend;
import com.bustiblelemons.tasque.utilities.Connection;
import com.bustiblelemons.tasque.utilities.Values.Database.Task.TaskState;

/**
 * 
 * @author bhm Facade class handling both SQLiteBackend, RTMBackend and
 *         intendedly future backends
 */
public class Task {
	/**
	 * 
	 * @param context
	 * @param listId
	 * @param taskId
	 * @param taskName
	 */
	public static void rename(Context context, String listId, String taskId, String taskName) {
		Database.updateTask(context, taskId, taskName);
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				RTMBackend.setTaskName(context, listId, String.valueOf(taskId), taskName);
			} else {
				Database.cacheTaskRenamed(context, String.valueOf(taskId), taskName);
			}
		}
	}

	/**
	 * 
	 * @param context
	 * @param listId
	 * @param taskName
	 */
	public static void add(Context context, String listId, String taskName) {
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				Transaction<it.bova.rtmapi.Task> t = RTMBackend.newTask(context, taskName, listId);
				it.bova.rtmapi.Task task = t.getObject();
				Database.newTask(context, taskName, task.getId(), listId);
			} else {
				Database.cacheNewTask(context, taskName, listId);
			}
		} else {
			Database.newTask(context, taskName, listId);
		}
	}

	/**
	 * 
	 * @param context
	 * @param listId
	 * @param taskId
	 * @param taskName
	 */
	public static void markDone(Context context, String listId, String taskId, String taskName) {
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				RTMBackend.setTaskState(context, TaskState.Completed, taskId, listId);
			} else {
				RTMBackend.cacheTask(context, taskName, taskId, listId, TaskState.Completed);
			}
		}
		Database.markTaskDone(context, taskId);
	}

	public static void delete(Context context, String listId, Collection<String> tasksToDelete) {
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				RTMBackend.deleteTasks(context, tasksToDelete, listId);
			} else {
				Database.cacheTasksDeleted(context, tasksToDelete);
			}
		}
		Database.markTasksDeleted(context, tasksToDelete);
	}

	public static void markActive(Context context, String listId, String taskId, String taskName) {
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				RTMBackend.setTaskState(context, TaskState.Active, taskId, listId);
			} else {
				RTMBackend.cacheTask(context, taskName, taskId, listId, TaskState.Active);
			}
		}
		Database.markTaskActive(context, taskId);
	}

}
