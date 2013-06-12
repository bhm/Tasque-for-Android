package com.bustiblelemons.tasque.settings;

import static com.bustiblelemons.tasque.utilities.Values.TAG;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.rtm.RTMConnectivityReceiver;
import com.bustiblelemons.tasque.utilities.Connection;

/**
 * Created 9 Jun 2013
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class RTMBackendFragmentSettings extends PreferenceFragment {

	private class ConnectionReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "SettingsActivity:onReceive");
			try {
				setUpLoginPreference(getActivity().getApplicationContext());
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
	}

	private ConnectionReceiver connectionStateReceiver;
	private IntentFilter connectionStateFilter;
	private Context context;
	private RTMConnectivityReceiver connectivityReceiver;

	private boolean setUpLoginPreference(Context context) {
		Preference p = findPreference(context.getString(R.string.pref_backend_rtm_configure_key));
		if (SettingsUtil.rtmAccountConfigured(context)
				|| (Connection.isUp(context) && SettingsUtil.rtmAccountConfigured(context))) {
			String userName = SettingsUtil.getRTMUserName(context);
			p.setTitle(context.getString(R.string.pref_backend_rtm_configure_logged_in_title) + userName);
			p.setSummary(R.string.pref_backend_rtm_configure_logged_in_summary);
			p.setOnPreferenceClickListener(SettingsActivity.logOUTofRTMListener);
			return true;
		} else if (Connection.isUp(context) && !SettingsUtil.rtmAccountConfigured(context)) {
			p.setTitle(R.string.pref_backend_rtm_configure_not_logged_in_title);
			p.setSummary(R.string.pref_backend_rtm_configure_not_logged_in_title);
			p.setOnPreferenceClickListener(SettingsActivity.loginToRTMListener);
			return true;
		} else if (!Connection.isUp(context) && !SettingsUtil.rtmAccountConfigured(context)) {
			p.setTitle(R.string.pref_backend_rtm_configure_no_connection_title);
			p.setSummary(R.string.pref_backend_rtm_configure_no_connection_summary);
			p.setOnPreferenceClickListener(SettingsActivity.configureConnectionListener);
			return true;
		}
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity().getApplicationContext();
		addPreferencesFromResource(R.xml.pref_backend_rtm_fragment);
		SettingsActivity.bindSummaryToValue(findPreference(context.getString(R.string.pref_backend_use_rtm_key)));
		Preference loginRTMPreference = findPreference(context.getString(R.string.pref_backend_rtm_configure_key));
		SettingsActivity.bindSummaryToValue(loginRTMPreference);
		setUpLoginPreference(context);
		SettingsActivity.bindSummaryToValue(findPreference(context.getString(R.string.pref_backend_rtm_use_data_key)));
		SettingsActivity.bindSummaryToValue(findPreference(context
				.getString(R.string.pref_backend_rtm_synchronization_interval_key)));
		SettingsActivity.bindSummaryToValue(findPreference(context
				.getString(R.string.pref_backend_rtm_synchronization_from_last_key)));
	}

	@Override
	public void onResume() {
		super.onResume();
		try {
			connectivityReceiver = new RTMConnectivityReceiver();
			IntentFilter connctivityFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
			context.registerReceiver(connectivityReceiver, connctivityFilter);
			connectionStateFilter = new IntentFilter(RTMConnectivityReceiver.CONNECTIVITY_FILTER);
			connectionStateReceiver = new ConnectionReceiver();
			context.registerReceiver(connectionStateReceiver, connectionStateFilter);
		} catch (Exception e) {
			Log.d(TAG, "Problem registering receivers\t" + e.getCause());
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		try {
			context.unregisterReceiver(connectionStateReceiver);
			context.unregisterReceiver(connectivityReceiver);
		} catch (Exception e) {
			Log.d(TAG, "Problem unregistering the receiver in SettingsActivity");
		}
	}
}
