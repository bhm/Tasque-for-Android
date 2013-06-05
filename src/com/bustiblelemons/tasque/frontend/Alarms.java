package com.bustiblelemons.tasque.frontend;

import static com.bustiblelemons.tasque.utilities.Values.TAG;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bustiblelemons.tasque.main.SettingsUtil;
import com.bustiblelemons.tasque.rtm.RTMSyncBroadcastReceiver;
import com.bustiblelemons.tasque.rtm.RTMSyncService;

/**
 * Created 31 May 2013
 */
public class Alarms {
	public static void cancel(Context context) {
		Log.d(TAG, "Canceling alarms for updates");
		AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, RTMSyncService.class);
		PendingIntent pending = PendingIntent.getService(context, RTMSyncService.REQUEST_CODE, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		pending.cancel();
		manager.cancel(pending);
	}

	public static void setUp(Context context) {
		// int intervalMillis = SettingsUtil.getRTMIntervalUpdate(context);
		// FIXME Still doesn't fire up properly
		Alarms.cancel(context);
		int intervalMillis = SettingsUtil.getRTMIntervalUpdate(context);
		if (intervalMillis > 0) {
			AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(context, RTMSyncBroadcastReceiver.class);
			PendingIntent pending = PendingIntent.getBroadcast(context, RTMSyncService.REQUEST_CODE, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			Log.d(TAG, "Another update in " + (intervalMillis / 1000) / 60 + " minutes.");
			manager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + intervalMillis, intervalMillis,
					pending);
		} else {
			Log.d(TAG, "There will be no updates");
		}
	}

}
