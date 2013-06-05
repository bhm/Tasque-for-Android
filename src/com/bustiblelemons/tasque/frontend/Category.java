package com.bustiblelemons.tasque.frontend;

import java.util.Collection;

import android.content.Context;

import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.rtm.RTMBackend;
import com.bustiblelemons.tasque.utilities.Connection;

public class Category {

	public static boolean rename(Context context, String categoryId, String categoryName) {
		long r = -1;
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				RTMBackend.setListName(context, categoryId, categoryName);
				r = Database.setCategoryName(context, categoryId, categoryName);
			} else {
				r = Database.cacheCateogryRenamed(context, categoryId, categoryName);
			}
		} else {
			r = Database.setCategoryName(context, categoryId, categoryName);
		}
		
		if (r > 0) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean insert(Context context, String categoryName) {
		long r = -1;
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				String listId = RTMBackend.newList(context, categoryName);
				if (listId.length() > 0) {
					r = Database.newCategory(context, categoryName, listId);
				}
			} else {
				r = Database.cacheNewCategory(context, categoryName);
			}

		} else {
			r = Database.newCategory(context, categoryName);
		}
		if (r > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Tasks in a list should be moved to inbox, they are not deleted.
	 * @param context
	 * @param listIds
	 */
	public static void delete(Context context, Collection<String> listIds) {
		if (RTMBackend.useRTM(context)) {
			if (Connection.isUp(context)) {
				RTMBackend.deleteLists(context, listIds);
				Database.deleteCategories(context, listIds);
			} else {
				Database.cacheCategoriesDeleted(context, listIds);
			}
		} else {
			Database.deleteCategories(context, listIds);
		}
	}
}
