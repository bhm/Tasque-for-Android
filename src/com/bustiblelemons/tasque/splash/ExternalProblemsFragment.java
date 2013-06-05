package com.bustiblelemons.tasque.splash;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.utilities.Utility;

public class ExternalProblemsFragment extends SherlockFragment implements OnTouchListener {

	private View view;
	private TextView hint;
	private Context context;
	private static Thread checker;
	private static Handler handler;
	private static final int EXTERNAL_AVAILABLE = 100002;
	private static final int FOUND_DATABASE = 100003;
	public static final int DATABASE_COPIED = 100004;
	public static final int DATABASE_NOT_FOUND = 100005;

	public interface OnDatabaseSearchFinished {
		public void databaseSearchFinished(int result);
	}

	private OnDatabaseSearchFinished searchFinishedCallback;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnDatabaseSearchFinished) {
			searchFinishedCallback = (OnDatabaseSearchFinished) activity;
		} else {
			throw new ClassCastException(activity.getClass().getSimpleName() + " should implement interface "
					+ OnDatabaseSearchFinished.class.getSimpleName());
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_external_problems, null);
		hint = (TextView) view.findViewById(R.id.fragment_external_problems_hint);
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity().getApplicationContext();
		handler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				switch (msg.arg1) {
				case EXTERNAL_AVAILABLE:
					hint.setText(R.string.fragment_external_problems_hint_memory_mounted);
					break;
				case DATABASE_NOT_FOUND:
					searchFinishedCallback.databaseSearchFinished(DATABASE_NOT_FOUND);
					checker.stop();
					break;
				case FOUND_DATABASE:
					try {
						Utility.copyDatabase(context);
					} finally {
						checker.stop();
						searchFinishedCallback.databaseSearchFinished(DATABASE_COPIED);
					}
					break;
				}
			};
		};
		checker = new Thread(new Runnable() {
			Handler mHandler = handler;

			@Override
			public void run() {
				try {
					Thread.sleep(3000);
					if (Utility.isExtenalAvailable()) {
						Message msg = handler.obtainMessage();
						if (Utility.isDatabaseSynced(context)) {
							msg.arg1 = FOUND_DATABASE;
						} else {
							msg.arg1 = DATABASE_NOT_FOUND;
						}
						mHandler.sendMessage(msg);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		});
		checker.start();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

}
