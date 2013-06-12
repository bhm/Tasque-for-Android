package com.bustiblelemons.tasque.rtm;

import static com.bustiblelemons.tasque.utilities.Values.TAG;

import com.bustiblelemons.tasque.settings.SettingsUtil;
import com.bustiblelemons.tasque.utilities.Connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RTMSyncBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Connection.isUp(context)) {
			Log.d(TAG, "A proper connection is present.");
			context.startService(new Intent(context, RTMSyncService.class));
		} else {
			Log.d(TAG, "Connection is not up.\nWill try in " + (SettingsUtil.getRTMIntervalUpdate(context) / 1000)
					/ 60 + " min again.");
		}
	}
}
