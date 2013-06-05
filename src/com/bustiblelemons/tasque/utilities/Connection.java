package com.bustiblelemons.tasque.utilities;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;

import com.bustiblelemons.tasque.main.SettingsUtil;

public class Connection {

	private static ConnectivityManager getConnectivityManager(Context context) {
		return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	public static boolean isWiFiUp(Context context) {
		return getConnectivityManager(context).getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
	}

	public static boolean isWiMaxUp(Context context) {
		return getConnectivityManager(context).getNetworkInfo(ConnectivityManager.TYPE_WIMAX).isConnected();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private static boolean isEthernetUp(Context context) {
		return getConnectivityManager(context).getNetworkInfo(ConnectivityManager.TYPE_ETHERNET).isConnected();
	}

	public static boolean isMobileUp(Context context) {
		return getConnectivityManager(context).getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
	}

	public static boolean isUpOhterUp(Context context) {
		return Connection.isWiFiUp(context) || Connection.isWiMaxUp(context) || Connection.isEthernetUp(context);
	}

	public static boolean isUp(Context context) {
		return (SettingsUtil.useMobileData(context) ? Connection.isWiFiUp(context) : Connection.isUpOhterUp(context));
	}
}
