package com.bustiblelemons.tasque.rtm;

import static com.bustiblelemons.tasque.utilities.Values.TAG;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ResultReceiver;
import android.util.Log;

import com.bustiblelemons.tasque.utilities.Connection;

public class RTMConnectivityReceiver extends BroadcastReceiver {

	public static final String CONNECTIVITY_FILTER = "com.bustiblelemons.tasque.rtm.RTMConnectivityReceiver.CONNECTIVITY_FILTER";
	private ResultReceiver receiver;

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		Log.d(TAG, "Connectivity RECEIVER " + action);
		Intent broadcast = new Intent();
		broadcast.setAction(CONNECTIVITY_FILTER);
		context.sendBroadcast(broadcast);
		if (RTMBackend.useRTM(context) && Connection.isUp(context)) {
			if (isRTMUpdateSericeRunning(context)) {
				Log.d(TAG, "Service is alread running not starting a new one");
			} else {
				startService(context);
			}
		}

	}

	private boolean isRTMUpdateSericeRunning(Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (RTMSyncService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void startService(Context context) {
		Intent service = new Intent(context, RTMSyncService.class);
		if (receiver != null) {
			service.putExtra(RTMSyncService.RESULT_RECEIVER, this.receiver);
		}
		context.startService(service);
	}
}
