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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import com.bustiblelemons.tasque.settings.SettingsUtil;

public class RTMAuthFragment extends SherlockFragment implements OnTouchListener {
	public static final String FRAGMENT_TAG = "rtm_auth";
	private View view;
	private TextSwitcher switcher;

	public interface OnCompleteAuthentication {
		public void onCompleteAuthentication(Object token);
	}

	private OnCompleteAuthentication completeAuthentication;

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
		Log.d(TAG, "RTMAuthFragment:onCreate");
		super.onCreate(savedInstanceState);
	}

	private String frob;
	private boolean browserLanched = false;
	private String authUrl;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "RTMAuthFragment:onCreateView");
		view = inflater.inflate(R.layout.fragment_rtm_auth, null);
		switcher = (TextSwitcher) view.findViewById(R.id.fragment_rtm_auth_feedback_text);
		setRetainInstance(false);
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "RTMAuthFragment:onStart");
		ExecutorService service = Executors.newSingleThreadExecutor();
		Callable<String> urlRequester = new Callable<String>() {
			@Override
			public String call() throws Exception {
				String r = "";
				try {
					RtmApiAuthenticator auth = new RtmApiAuthenticator(Milk.API_KEY, Milk.API_SECRET);
					Log.d(TAG, "Auth: " + auth);
					frob = auth.authGetFrob();
					Log.d(TAG, "Frob: " + frob);
					r = auth.authGetDesktopUrl(Permission.DELETE, frob);
				} catch (ServerException e) {
					e.printStackTrace();
				} catch (RtmApiException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return r;
			}
		};
		Future<String> f = service.submit(urlRequester);
		try {
			authUrl = f.get();
			Log.d(TAG, "Retreived URL: " + authUrl);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			switcher.setText("");
			if (!browserLanched) {
				this.launchBrowser(authUrl);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "RTMAuthFragment:onResume");
	}

	private void launchBrowser(String url) {
		try {
			Uri uri = Uri.parse(url);
			Intent browser = new Intent(Intent.ACTION_VIEW, uri);
			startActivityForResult(browser, 10);
			browserLanched = true;
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "Request: " + requestCode + "\tResult Code: " + resultCode);
		this.getToken();
	}

	protected void getToken() {
		new Importer().execute(frob);
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
				this.sleep();
				RtmApiTransactable transactable = RTMBackend.getTransactable(token);
				SettingsUtil.saveRTMToken(context, token);
				publishProgress(context.getString(R.string.fragment_auth_rtm_categories_saved_token));
				this.sleep();
				if (!SettingsUtil.rtmAccountConfigured(context)) {
					publishProgress(context.getString(R.string.fragment_auth_rtm_categories_catch_up));
					RTMBackend.uploadCategories(context, false);
					RTMBackend.uploadTasks(context, false);
				}
				Database.createFreshDatabase(context, false, true);
				Database.createCacheDatabase(context);
				List<TaskList> lists = transactable.listsGetList();
				Database.importCategories(context, lists);
				publishProgress(context.getString(R.string.fragment_auth_rtm_categories_inserted));
				this.sleep();
				publishProgress(context.getString(R.string.fragment_auth_rtm_got_lists_prefix));
				for (TaskList list : lists) {
					List<Task> tasks = transactable.tasksGetByList(list);
					Database.importTasks(context, tasks, list.getId());
					publishProgress(list.getName());
				}
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
				Log.d(TAG, "Done importing everything");
				switcher.setText(context.getString(R.string.fragment_auth_rtm_auth_done));
				this.sleep();
				try {
					SettingsUtil.setRTMAccountConfigured(context, true);
					completeAuthentication.onCompleteAuthentication(token);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case PROBLEM:
				Log.d(TAG, "Problem\n");
				SettingsUtil.setRTMAccountConfigured(context, false);
				detachRTMAuthFragment.onDetachRTMAuthFragment();
				this.sleep();
			default:
				break;
			}
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "Remember The Milk Fragment Active.");
		return true;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

}
