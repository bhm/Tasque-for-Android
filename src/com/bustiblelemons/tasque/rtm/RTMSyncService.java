package com.bustiblelemons.tasque.rtm;

import static com.bustiblelemons.tasque.utilities.Values.TAG;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.bustiblelemons.tasque.settings.SettingsUtil;

public class RTMSyncService extends IntentService {

	public static final String INTENT_FILTER = "com.bustiblelemons.tasque.rtm.RTMSyncService.SYNCHRONIZATION";
	public static final String FORCE_SYNC = "force_synchronization";
	public static final String SYNC_STATUS = "SyncStatus";
	public static final int SYNC_BEGIN = 0x001;
	public static final int SYNC_CATEGORIES_CHANGED = 0x002;
	public static final int SYNC_CATEGORIES_UPLOADED = 0x0022;
	public static final int SYNC_DONE = 0x003;
	public static final int SYNC_DONE_EMPTY = 0x004;
	public static final int SYNC_POSTPONED = 0x005;
	public static final int SERVICE_STOPPED = 0x006;
	public static final String RESULT_RECEIVER = "ResultReceiver";
	public static final String SYNCHRONIZED_LISTS = "SynchronizedLists";
	public static final int REQUEST_CODE = 0x404;
	protected static boolean STOP_SERVICE;

	public interface OnRTMRefresh {
		public void startRTMRefreshService(Context context, boolean force);
	}

	private Context context;
	private Bundle resultData;
	private Date lastSync;
	private Date now;
	private SimpleDateFormat format;
	private Intent synchIntent;
	private SyncCancelReceiver receiver;
	private Integer updateInterval;

	public class SyncCancelReceiver extends BroadcastReceiver {

		public static final String SHOULD_STOP_SERVICE = "should_stop_service";
		protected static final String INTENT_FILTER = "com.bustiblelemons.tasque.rtm.RTMSyncService.CANCEL_SERVICE";

		@Override
		public void onReceive(Context context, Intent intent) {
			STOP_SERVICE = intent.getBooleanExtra(SHOULD_STOP_SERVICE, false);
			Log.d(TAG, "\t\t\t\tSynchronization Cancel Receiver\tService should stop : " + STOP_SERVICE);
		}

	}

	public RTMSyncService() {
		super(TAG);
		Log.d(TAG, "RTMSyncService");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		receiver = new SyncCancelReceiver();
		IntentFilter filter = new IntentFilter(SyncCancelReceiver.INTENT_FILTER);
		registerReceiver(receiver, filter);
		synchIntent = new Intent();
		resultData = new Bundle();
		this.context = getApplicationContext();
		lastSync = SettingsUtil.getRTMLastSync(context);
		updateInterval = SettingsUtil.getRTMIntervalUpdate(context);
		now = new Date();
		format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		Log.d(TAG, "Now is " + format.format(now)
				+ (lastSync != null ? "\nLast sync on " + format.format(lastSync) : ""));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
		Log.d(TAG, "RTMSynService:onDestroy()");
		synchIntent.putExtra(RTMSyncService.SYNC_STATUS, RTMSyncService.SERVICE_STOPPED);
		sendBroadcast(synchIntent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		boolean force = intent.getBooleanExtra(RTMSyncService.FORCE_SYNC, false);
		long passedTime = now.getTime() - lastSync.getTime();
		Log.d(TAG, "Update Interval: " + updateInterval + "\tPassed Time: " + passedTime);
		if (passedTime >= updateInterval || force) {
			synchIntent.setAction(RTMSyncService.INTENT_FILTER);
			synchIntent.putExtra(RTMSyncService.SYNC_STATUS, RTMSyncService.SYNC_BEGIN);
			sendBroadcast(synchIntent);
			Log.d(TAG, "Setting the default list ");
			RTMBackend.setDefaultListId(context, SettingsUtil.getDefaultCategoryStringId(context));
			if (STOP_SERVICE) {
				return;
			}
			boolean synchedLists = RTMBackend.synchronizeLists(context);
			if (synchedLists) {
				synchIntent.putExtra(RTMSyncService.SYNC_STATUS, RTMSyncService.SYNC_CATEGORIES_CHANGED);
				sendBroadcast(synchIntent);
			}
			if (STOP_SERVICE) {
				return;
			}
			Log.d(TAG, "Synchronizing Categories ");
			HashSet<String> synchedCache = (HashSet<String>) RTMBackend.uploadCategories(context, true);
			resultData.putSerializable(SYNCHRONIZED_LISTS, synchedCache);
			synchIntent.putExtra(RTMSyncService.SYNC_STATUS, RTMSyncService.SYNC_CATEGORIES_UPLOADED);
			synchIntent.putExtra(RTMSyncService.SYNCHRONIZED_LISTS, synchedCache);
			sendBroadcast(synchIntent);
			if (STOP_SERVICE) {
				return;
			}
			Log.d(TAG, "Synchronizing Tasks ");
			RTMBackend.uploadTasks(context, true);

			if (STOP_SERVICE) {
				return;
			}
			int syncWithServer = RTMBackend.synchronizeFromServer(context, lastSync);
			Log.d(TAG, "Synchronized from cache " + synchedCache.size());
			Log.d(TAG, "Synchronized lists from the server " + synchedLists);
			Log.d(TAG, "Synchronized tasks from server " + syncWithServer);
			SettingsUtil.setRTMLastSync(context, now);
			synchIntent.putExtra(RTMSyncService.SYNC_STATUS, RTMSyncService.SYNC_DONE);
			sendBroadcast(synchIntent);
		} else {
			Log.d(TAG, "Update should not go off yet");
		}
		stopSelf();
	}

}
