package com.bustiblelemons.tasque.settings;

import static com.bustiblelemons.tasque.utilities.Values.TAG;

import java.util.List;
import java.util.Map;

import yuku.ambilwarna.widget.AmbilWarnaPreference;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.rtm.RTMConnectivityReceiver;
import com.bustiblelemons.tasque.rtm.RTMLoginFromSettings;
import com.bustiblelemons.tasque.tutorial.SynchronizeTutorialActivity;
import com.bustiblelemons.tasque.utilities.Connection;

public class SettingsActivity extends PreferenceActivity {
	private static final boolean ALWAYS_SIMPLE_PREFS = false;
	private static final int SCREENLAYOUT_SIZE_XLARGE = 0x00000004;
	private Context context;
	private ConnectionReceiver connectionStateReceiver;
	private RTMConnectivityReceiver connectivityReceiver;
	private static String regex;
	private static String currentlyPrefix;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getApplicationContext();
		currentlyPrefix = context.getString(R.string.pref_currently_prefix);
		regex = currentlyPrefix + ".*";
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setupSimplePreferencesScreen();
	}

	private class ConnectionReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "SettingsActivity:onReceive");
			if (isSimplePreferences(context)) {
				setUpLoginPreference(getApplicationContext());
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			connectivityReceiver = new RTMConnectivityReceiver();
			IntentFilter connctivityFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
			registerReceiver(connectivityReceiver, connctivityFilter);
			IntentFilter connectionStateFilter = new IntentFilter(RTMConnectivityReceiver.CONNECTIVITY_FILTER);
			connectionStateReceiver = new ConnectionReceiver();
			registerReceiver(connectionStateReceiver, connectionStateFilter);
		} catch (Exception e) {
			Log.d(TAG, "Problem registering the receiver in SettingsActivity");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			unregisterReceiver(connectionStateReceiver);
			unregisterReceiver(connectivityReceiver);
		} catch (Exception e) {
			Log.d(TAG, "Problem unregistering the receiver in SettingsActivity");
		}
	}

	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}
		addPreferencesFromResource(R.xml.pref_general);
		bindSummaryToValue(findPreference(context.getString(R.string.pref_auto_cap_characters_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_hide_keyboard_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_time_out_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_export_database_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_font_size_completion_date_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_font_size_due_date_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_font_size_list_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_font_size_input_key)));
		findPreference(context.getString(R.string.pref_export_how_to_key)).setOnPreferenceClickListener(
				showHowToSynchronize);
		addPreferencesFromResource(R.xml.pref_backend_rtm);
		Preference loginRTMPreference = findPreference(context.getString(R.string.pref_backend_rtm_configure_key));
		bindSummaryToValue(loginRTMPreference);
		setUpLoginPreference(context);
		bindSummaryToValue(findPreference(context.getString(R.string.pref_backend_rtm_use_data_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_backend_rtm_synchronization_interval_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_backend_rtm_synchronization_from_last_key)));
		addPreferencesFromResource(R.xml.pref_date_time);
		bindSummaryToValue(findPreference(context.getString(R.string.pref_date_show_date_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_date_format_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_date_format_show_hour_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_date_format_show_hour_ampm_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_use_color_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_color_today_key)));
		bindSummaryToValue(findPreference(context.getString(R.string.pref_color_overdue_key)));
		addPreferencesFromResource(R.xml.pref_about);
	}

	private static OnPreferenceClickListener showHowToSynchronize = new OnPreferenceClickListener() {

		@Override
		public boolean onPreferenceClick(Preference preference) {
			Context context = preference.getContext();
			Intent howto = new Intent(context, SynchronizeTutorialActivity.class);
			context.startActivity(howto);
			return true;
		}
	};

	private void setUpLoginPreference(Context context) {
		Preference p = findPreference(context.getString(R.string.pref_backend_rtm_configure_key));
		if (SettingsUtil.rtmAccountConfigured(context)
				|| (Connection.isUp(context) && SettingsUtil.rtmAccountConfigured(context))) {
			String userName = SettingsUtil.getRTMUserName(context);
			p.setTitle(context.getString(R.string.pref_backend_rtm_configure_logged_in_title) + userName);
			p.setSummary(R.string.pref_backend_rtm_configure_logged_in_summary);
			p.setOnPreferenceClickListener(logOUTofRTMListener);
		} else if (Connection.isUp(context) && !SettingsUtil.rtmAccountConfigured(context)) {
			p.setTitle(R.string.pref_backend_rtm_configure_not_logged_in_title);
			p.setSummary(R.string.pref_backend_rtm_configure_not_logged_in_title);
			p.setOnPreferenceClickListener(loginToRTMListener);
		} else if (!Connection.isUp(context) && !SettingsUtil.rtmAccountConfigured(context)) {
			p.setTitle(R.string.pref_backend_rtm_configure_no_connection_title);
			p.setSummary(R.string.pref_backend_rtm_configure_no_connection_summary);
			p.setOnPreferenceClickListener(configureConnectionListener);
		}
	}

	static OnPreferenceClickListener logOUTofRTMListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			Context context = preference.getContext();
			Uri uri = Uri.parse(context.getString(R.string.pref_backend_rtm_configure_url_logout));
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			context.startActivity(intent);
			preference.setSummary(R.string.pref_backend_rtm_configure_not_logged_in_summary);
			return true;
		}
	};

	static OnPreferenceClickListener configureConnectionListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			Context context = preference.getContext();
			Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
			context.startActivity(intent);
			return true;
		}
	};

	static OnPreferenceClickListener loginToRTMListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			Context context = preference.getContext();
			Intent intent = new Intent(context, RTMLoginFromSettings.class);
			context.startActivity(intent);
			return true;
		}
	};

	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= SCREENLAYOUT_SIZE_XLARGE;
	}

	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || !isXLargeTablet(context);
	}

	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onAttachFragment(Fragment fragment) {
		super.onAttachFragment(fragment);
	}

	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		if (!isSimplePreferences(this)) {
			loadHeadersFromResource(R.xml.pref_headers, target);
		}
	}

	static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			if (value != null) {
				String stringValue = "";
				String summary = "";
				String newValue = "";
				Context context = preference.getContext();
				if (value instanceof Boolean) {
					stringValue = (Boolean) value == true ? context.getString(R.string.pref_enabled) : context
							.getString(R.string.pref_disabled);
				} else if (value instanceof String) {
					stringValue = value.toString();
				}
				if (preference instanceof ListPreference) {
					CharSequence[] entries = ((ListPreference) preference).getEntries();
					newValue = (String) entries[((ListPreference) preference).findIndexOfValue((String) value)];
				} else if (preference instanceof AmbilWarnaPreference) {
					return true;
				} else {
					newValue = stringValue;
				}
				if (preference.getSummary() != null) {
					summary = preference.getSummary().toString() + currentlyPrefix;
				} else {
					summary += currentlyPrefix;
				}
				if (preference instanceof CheckBoxPreference) {
					summary = summary.replaceFirst(regex, "");
				} else {
					summary = summary.replaceFirst(regex, currentlyPrefix + newValue.toString());
				}
				preference.setSummary(summary);
			}
			return true;
		}
	};

	static void bindSummaryToValue(Preference preference) {
		try {
			preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
			Map<String, ?> allPrefs = PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getAll();
			sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, allPrefs.get(preference.getKey()));
		} catch (NullPointerException e) {
			Log.d(TAG, "Could not bind value " + preference);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);
			Context context = getActivity().getApplicationContext();
			bindSummaryToValue(findPreference(context.getString(R.string.pref_export_database_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_auto_cap_characters_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_hide_keyboard_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_time_out_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_font_size_completion_date_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_font_size_due_date_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_font_size_list_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_font_size_input_key)));
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class DateTimePreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_date_time_fragment);
			Context context = getActivity().getApplicationContext();
			bindSummaryToValue(findPreference(context.getString(R.string.pref_date_show_date_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_date_format_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_date_format_show_hour_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_date_format_show_hour_ampm_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_color_today_key)));
			bindSummaryToValue(findPreference(context.getString(R.string.pref_color_overdue_key)));
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class AboutPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_about_fragment);
		}
	}
}
