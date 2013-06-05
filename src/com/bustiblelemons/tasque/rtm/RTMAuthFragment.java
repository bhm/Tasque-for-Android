package com.bustiblelemons.tasque.rtm;

import static com.bustiblelemons.tasque.utilities.Values.TAG;
import it.bova.rtmapi.Permission;
import it.bova.rtmapi.RtmApiAuthenticator;
import it.bova.rtmapi.RtmApiException;
import it.bova.rtmapi.RtmApiTransactable;
import it.bova.rtmapi.ServerException;
import it.bova.rtmapi.Task;
import it.bova.rtmapi.TaskList;
import it.bova.rtmapi.Token;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextSwitcher;

import com.actionbarsherlock.app.SherlockFragment;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.database.Database;
import com.bustiblelemons.tasque.main.SettingsUtil;

public class RTMAuthFragment extends SherlockFragment implements OnTouchListener {
	public static final String FRAGMENT_TAG = "rtm_auth";
	private View view;
	private String authURL;
	private TextSwitcher switcher;

	public interface OnCompleteAuthentication {
		public void onCompleteAuthentication(Object token);
	}

	private OnCompleteAuthentication completeAuthentication;
	private RtmApiAuthenticator auth;

	public interface OnShowRTMAuthFragment {
		public void onShowRTMAuthFragment();
	}

	public interface OnDetachRTMAuthFragment {
		public void onDetachRTMAuthFragment();
	}

	private OnDetachRTMAuthFragment detachRTMAuthFragment;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		completeAuthentication = (OnCompleteAuthentication) activity;
		detachRTMAuthFragment = (OnDetachRTMAuthFragment) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		auth = new RtmApiAuthenticator(Milk.API_KEY, Milk.API_SECRET);
	}

	private String frob;
	private boolean browserLanched;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_rtm_auth, null);
		switcher = (TextSwitcher) view.findViewById(R.id.fragment_rtm_auth_feedback_text);
		setRetainInstance(true);
		return view;
	}

	protected void getToken() {
		new Importer().execute(frob);
	}

	@Override
	public void onStart() {
		super.onStart();
		try {
			switcher.setText("");
			frob = auth.authGetFrob();
			authURL = auth.authGetDesktopUrl(Permission.DELETE, frob);
		} catch (ServerException e) {
			e.printStackTrace();
		} catch (RtmApiException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (!browserLanched) {
				this.launchBrowser();
			}
		}
	}

	private void launchBrowser() {
		Log.d(FRAGMENT_TAG, "URL: " + authURL);
		Uri uri = Uri.parse(authURL);
		Intent browser = new Intent(Intent.ACTION_VIEW, uri);
		startActivityForResult(browser, 10);
		browserLanched = true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		this.getToken();
	}

	private class Importer extends AsyncTask<Object, String, Integer> {

		private Context context;
		private Token token;
		private String frob;

		private static final int SUCCESS = 0x001;
		private static final int PROBLEM = 0x002;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			this.context = getActivity().getApplicationContext();
		}

		@Override
		protected Integer doInBackground(Object... params) {
			RtmApiAuthenticator auth = new RtmApiAuthenticator(Milk.API_KEY, Milk.API_SECRET);
			frob = (String) params[0];
			try {
				publishProgress(context.getString(R.string.fragment_auth_rtm_getting_auth_url));
				token = auth.authGetToken(frob);
				publishProgress(context.getString(R.string.fragment_auth_rtm_got_url));
				RtmApiTransactable transactable = RTMBackend.getTransactable(token);
				SettingsUtil.saveRTMToken(context, token);
				Database.createFreshDatabase(context, false);
				Database.createCacheDatabase(context);
				List<TaskList> lists = transactable.listsGetList();
				Database.importCategories(context, lists);
				publishProgress(context.getString(R.string.fragment_auth_rtm_categories_inserted));
				this.sleep();
				publishProgress(context.getString(R.string.fragment_auth_rtm_got_lists_prefix));
				for (TaskList list : lists) {
					List<Task> tasks = transactable.tasksGetByList(list);
					Database.importTasks(context, tasks, list.getId());
					publishProgress(String.format(context.getString(R.string.fragment_auth_rtm_got_list),
							list.getName()));
				}
				publishProgress(context.getString(R.string.fragment_auth_rtm_auth_done));
				this.sleep();
			} catch (ServerException e) {
				e.printStackTrace();
				publishProgress(context.getString(R.string.fragment_auth_rtm_problem_hint));
				this.sleep();
				return PROBLEM;
			} catch (RtmApiException e) {
				e.printStackTrace();
				return PROBLEM;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return SUCCESS;
		}

		private void sleep() {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			switcher.setText(values[0]);
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			switch (result) {
			case SUCCESS:
				Log.d(FRAGMENT_TAG, "Done importing everything");
				switcher.setText(context.getString(R.string.fragment_auth_rtm_auth_done));
				this.sleep();
				try {
					completeAuthentication.onCompleteAuthentication(token);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case PROBLEM:
				Log.d(FRAGMENT_TAG, "Problem\n");
				detachRTMAuthFragment.onDetachRTMAuthFragment();
				this.sleep();
			default:
				break;
			}
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(FRAGMENT_TAG, "Remember The Milk Fragment Active.");
		return true;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

}
