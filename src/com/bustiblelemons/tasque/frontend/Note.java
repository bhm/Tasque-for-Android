package com.bustiblelemons.tasque.frontend;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.content.Context;

import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.rtm.RTMBackend;
import com.bustiblelemons.tasque.utilities.Connection;

/**
 * Created 29 May 2013
 */
public class Note {

	public static void add(Context context, String listId, String taskId, String body) {
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				Future<it.bova.rtmapi.Note> result = RTMBackend.newNote(context, listId, taskId, body);
				Database.newNote(context, taskId, body);
				while (!result.isDone()) {

				}
				try {
					Database.updateNoteId(context, result.get().getId(), taskId, body);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			} else {
				Database.cacheNewNote(context, taskId, body);
			}
		} else {
			Database.newNote(context, taskId, body);
		}
	}

	public static void update(Context context, String noteId, String listId, String taskId, String oldNote, String body) {
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				Database.updateNote(context, noteId, body);
				Database.updateCachedNote(context, noteId, body);
				RTMBackend.updateNote(context, noteId, body);
			} else {
				Database.updateCachedNote(context, noteId, body);
			}
		} else {
			Database.updateNote(context, taskId, body);
		}
	}

	public static void delete(Context context, String taskId, Collection<String> forDeletion) {
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				RTMBackend.deleteNotes(context, forDeletion);
				Database.deleteNotes(context, taskId, forDeletion);
			} else {
				Database.cacheNotesDeleted(context, forDeletion);
			}
		} else {
			Database.deleteNotes(context, taskId, forDeletion);	
		}		
	}
}
