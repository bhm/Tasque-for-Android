package com.bustiblelemons.tasque.main;

import it.bova.rtmapi.DateParser;
import it.bova.rtmapi.ParsingException;
import it.bova.rtmapi.Token;

import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.utilities.PermissionParser;
import com.bustiblelemons.tasque.utilities.Values.JSONToken;

public class SettingsUtil {

	private synchronized static SharedPreferences getPreferencesFile(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	/**
	 * Saves default values from all preference screens.
	 * 
	 * @param context
	 * @param reset
	 *            true to overwrite saved values.
	 */
	public static void setDefaultValues(Context context, boolean reset) {
		PreferenceManager.setDefaultValues(context, R.xml.pref_general, reset);
		PreferenceManager.setDefaultValues(context, R.xml.pref_date_time, reset);
		PreferenceManager.setDefaultValues(context, R.xml.pref_backend_rtm, reset);
	}

	public static void firstRunDone(Context context) {
		SettingsUtil.getPreferencesFile(context).edit().putBoolean(context.getString(R.string.pref_first_run), false)
				.commit();
	}

	public static boolean isFirstRun(Context context) {
		return SettingsUtil.getPreferencesFile(context).getBoolean(context.getString(R.string.pref_first_run), true);
	}

	public static boolean hideKeyboard(Context context) {
		return SettingsUtil.getPreferencesFile(context).getBoolean(context.getString(R.string.pref_hide_keyboard_key),
				false);
	}

	// INPUT
	public static int getTimeOut(Context context) {
		return SettingsUtil.getPreferencesFile(context).getInt(context.getString(R.string.pref_time_out_key), 5);
	}

	public static boolean autoCap(Context context) {
		return SettingsUtil.getPreferencesFile(context).getBoolean(
				context.getString(R.string.pref_auto_cap_characters_key), true);
	}

	// CATEGORIES
	public static void setDefaultCategoryId(Context context, int categoryID) {
		SettingsUtil.getPreferencesFile(context).edit()
				.putInt(context.getString(R.string.pref_default_category_key), categoryID).commit();
	}

	/**
	 * 
	 * @param context
	 * @return 0 if not found
	 */
	public static int getDefaultCategoryId(Context context) {
		return SettingsUtil.getPreferencesFile(context)
				.getInt(context.getString(R.string.pref_default_category_key), 0);
	}

	public static String getDefaultCategoryStringId(Context context) {
		return String.valueOf(SettingsUtil.getDefaultCategoryId(context));
	}

	public static boolean isDefaultCategory(Context context, int categoryId) {
		return (SettingsUtil.getDefaultCategoryId(context) == categoryId) ? true : false;
	}

	public static void setSelectedCategories(Context context, Iterable<String> categories) {
		JSONArray json = new JSONArray();
		for (String cat : categories) {
			json.put(cat);
		}
		SettingsUtil.getPreferencesFile(context).edit()
				.putString(context.getString(R.string.pref_all_selected_categories_key), json.toString()).commit();
	}

	public static ArrayList<String> getSelectedCategories(Context context) {
		ArrayList<String> ret = new ArrayList<String>();
		String JSONCategories = SettingsUtil.getPreferencesFile(context).getString(
				context.getString(R.string.pref_all_selected_categories_key), "");
		try {
			JSONArray array = new JSONArray(JSONCategories);
			for (int i = 0; i < array.length(); i++) {
				ret.add(array.getString(i));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static float getListFontSize(Context context) {
		String _r = SettingsUtil.getPreferencesFile(context).getString(
				context.getString(R.string.pref_font_size_list_key), "13");
		return Float.valueOf(_r);
	}

	public static float getInputFontSize(Context context) {
		String _r = SettingsUtil.getPreferencesFile(context).getString(
				context.getString(R.string.pref_font_size_input_key), "13");
		return Float.valueOf(_r);
	}

	public static float getCompletionDateFontSize(Context context) {
		return Float.valueOf(SettingsUtil.getPreferencesFile(context).getString(
				context.getString(R.string.pref_font_size_completion_date_key), "13"));
	}

	public static float getDueDateFontSize(Context context) {
		return Float.valueOf(SettingsUtil.getPreferencesFile(context).getString(
				context.getString(R.string.pref_font_size_due_date_key), "13"));
	}

	public static void setSelectedCategoriesToAll(Context context) {
		ArrayList<String> categories = Database.getCategoriesIds(context);
		SettingsUtil.setSelectedCategories(context, categories);
	}

	public static boolean showDate(Context context) {
		return SettingsUtil.getPreferencesFile(context).getBoolean(context.getString(R.string.pref_date_show_date_key),
				false);
	}

	public static String getDateFromat(Context context) {
		String r = SettingsUtil.getPreferencesFile(context).getString(context.getString(R.string.pref_date_format_key),
				"");
		return SettingsUtil.showHour(context) ? r + SettingsUtil.getHourFormat(context) : r;
	}

	public static String getHourFormat(Context context) {
		return SettingsUtil.hourFormatAMPM(context) ? " h:mm a" : " HH:mm";
	}

	public static boolean hourFormatAMPM(Context context) {
		return SettingsUtil.getPreferencesFile(context).getBoolean(
				context.getString(R.string.pref_date_format_show_hour_ampm_key), false);
	}

	public static boolean showHour(Context context) {
		return SettingsUtil.getPreferencesFile(context).getBoolean(
				context.getString(R.string.pref_date_format_show_hour_key), false);
	}

	public static boolean syncedDatabsePathSaved(Context context) {
		return SettingsUtil.getSyncedDatabasePath(context).length() > 0 ? true : false;
	}

	/**
	 * 
	 * @param context
	 * @return zero-length is not found. If is saved the absolute path
	 */
	public static String getSyncedDatabasePath(Context context) {
		return SettingsUtil.getPreferencesFile(context).getString(
				context.getString(R.string.pref_database_external_path_key), "");
	}

	public static void setSyncedDatabsePath(Context context, String path) {
		SettingsUtil.getPreferencesFile(context).edit()
				.putString(context.getString(R.string.pref_database_external_path_key), path).commit();
	}

	public static void setExportOnExit(Context context, boolean exportToExternal) {
		SettingsUtil.getPreferencesFile(context).edit()
				.putBoolean(context.getString(R.string.pref_export_database_key), exportToExternal).commit();
	}

	public static boolean getExportOnExit(Context context) {
		return SettingsUtil.getPreferencesFile(context).getBoolean(
				context.getString(R.string.pref_export_database_key), true);
	}

	public static boolean getStartedFresh(Context context) {
		return SettingsUtil.getPreferencesFile(context).getBoolean(context.getString(R.string.pref_started_fresh),
				false);
	}

	public static void setStartedFresh(Context context, boolean startedFresh) {
		SettingsUtil.getPreferencesFile(context).edit()
				.putBoolean(context.getString(R.string.pref_started_fresh), startedFresh).commit();
	}

	public static boolean useColours(Context context) {
		return SettingsUtil.getPreferencesFile(context).getBoolean(context.getString(R.string.pref_use_color_key),
				false);
	}

	public static int getTodayColor(Context context) {
		return SettingsUtil.getPreferencesFile(context).getInt(context.getString(R.string.pref_color_today_key),
				context.getResources().getColor(R.color.default_today_color));
	}

	public static int getOverdueColor(Context context) {
		return SettingsUtil.getPreferencesFile(context).getInt(context.getString(R.string.pref_color_today_key),
				context.getResources().getColor(R.color.default_overdue_color));
	}

	public static boolean startedFresh(Context context) {
		return SettingsUtil.getStartedFresh(context);
	}

	/**
	 * RTM BACKEND
	 */
	public static boolean getUseRTMBackend(Context context) {
		return SettingsUtil.getPreferencesFile(context).getBoolean(
				context.getString(R.string.pref_backend_use_rtm_key), false);
	}

	public static boolean useRTMBackend(Context context) {
		return SettingsUtil.getUseRTMBackend(context);
	}

	public static void setUseRTMBackend(Context context, boolean useRTM) {
		SettingsUtil.getPreferencesFile(context).edit()
				.putBoolean(context.getString(R.string.pref_backend_use_rtm_key), useRTM).commit();
	}

	public static void saveRTMToken(Context context, Token token) {
		JSONObject o = new JSONObject();
		try {
			o.put(JSONToken.Permission, token.getPermission().toString());
			o.put(JSONToken.Token, token.getToken());
			o.put(JSONToken.UserID, token.getUserId());
			o.put(JSONToken.UserName, token.getUserName());
			o.put(JSONToken.FullUserName, token.getFullName());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		SettingsUtil.getPreferencesFile(context).edit()
				.putString(context.getString(R.string.pref_backend_rtm_token), o.toString()).commit();
	}

	public static Token getRTMToken(Context context) {
		String t = SettingsUtil.getPreferencesFile(context).getString(
				context.getString(R.string.pref_backend_rtm_token), "");
		JSONObject o;
		Token token = null;
		try {
			o = new JSONObject(t);
			if (t.length() > 0) {
				token = new Token(PermissionParser.parse(o.getString(JSONToken.Permission)), o.getString(
						JSONToken.Token).toString(), o.getString(JSONToken.UserID), o.getString(JSONToken.UserName),
						o.getString(JSONToken.FullUserName));
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return token;
	}

	public static void setRTMLastSync(Context context, Date date) {
		SettingsUtil.getPreferencesFile(context).edit()
				.putString(context.getString(R.string.pref_backend_rtm_last_sync), DateParser.toISO8601(date)).commit();
	}

	/**
	 * 
	 * @param context
	 * @return if not found will return a date two weeks from now.
	 */
	public static Date getRTMLastSync(Context context) {
		Date r = new Date(System.currentTimeMillis() - SettingsUtil.getRTMSynchronizationFrom(context));
		try {
			String dateString = SettingsUtil.getPreferencesFile(context).getString(
					context.getString(R.string.pref_backend_rtm_last_sync), "");
			if (dateString.length() > 0) {
				r = DateParser.parseDate(dateString);
			}
		} catch (ParsingException e) {
			e.printStackTrace();
			return r;
		}
		return r;
	}

	private static long getRTMSynchronizationFrom(Context context) {
		return Long.valueOf(SettingsUtil.getPreferencesFile(context).getString(
				context.getString(R.string.pref_backend_rtm_synchronization_from_last_key), "1800000"));
	}

	public static boolean useMobileData(Context context) {
		return SettingsUtil.getPreferencesFile(context).getBoolean(
				context.getString(R.string.pref_backend_rtm_use_data_key), true);
	}

	public static Integer getRTMIntervalUpdate(Context context) {
		return Integer.valueOf(SettingsUtil.getPreferencesFile(context).getString(
				context.getString(R.string.pref_backend_rtm_synchronization_interval_key), "1800000"));
	}
	
	public static boolean useRTMUpdateService(Context context) {
		return !SettingsUtil.getPreferencesFile(context).getBoolean(context.getString(R.string.pref_backend_rtm_synchronization_only_running_key), false);
	}

}