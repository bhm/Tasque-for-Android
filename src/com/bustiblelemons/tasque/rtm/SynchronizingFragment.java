package com.bustiblelemons.tasque.rtm;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.bustiblelemons.tasque.R;

public class SynchronizingFragment extends SherlockFragment implements OnClickListener {

	public static final String FRAGMENT_TAG = "synchronizing";
	private View view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_synchronizing, null);
		(view.findViewById(R.id.fragment_synchronizing_cancel)).setOnClickListener(this);
		setRetainInstance(true);
		return view;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.fragment_synchronizing_cancel:
			Intent cancelBroadcast = new Intent();
			cancelBroadcast.setAction(RTMSyncService.SyncCancelReceiver.INTENT_FILTER);
			cancelBroadcast.putExtra(RTMSyncService.SyncCancelReceiver.SHOULD_STOP_SERVICE, true);
			getActivity().sendBroadcast(cancelBroadcast);
			break;
		default:
			break;
		}
	}

}
