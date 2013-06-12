package com.bustiblelemons.tasque.tutorial;

import static com.bustiblelemons.tasque.utilities.Values.TAG;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bustiblelemons.tasque.R;

/**
 * Created 11 Jun 2013
 */
public class OSChooserFragment extends SherlockFragment implements OnTouchListener, OnItemClickListener, OnClickListener {

	static final int LINUX = R.drawable.ic_action_linux;
	static final int ANDROID = R.drawable.ic_action_android;
	static final int WINDOWS = R.drawable.ic_action_windows;
	static final int OSX = R.drawable.ic_action_osx;
	public static final String FRAGMENT_TAG = "os_chooser";

	private View view;
	private ListView list;
	private OSListAdapter adapter;
	private Context context;

	public interface OSChooserFragmentListener {
		public void onShowOSChooser();

		public void onChosenOS(int osType);
	}

	private OSChooserFragmentListener chooserFragmentListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		chooserFragmentListener = (OSChooserFragmentListener) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.context = inflater.getContext();
		view = inflater.inflate(R.layout.fragment_os_chooser, null);
		list = (ListView) view.findViewById(R.id.fragment_os_chooser_list);
		adapter = new OSListAdapter(context);
		list.setAdapter(adapter);
		list.setOnItemClickListener(this);
		return view;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		Log.d(TAG, "OSChooserFragment:onItemClick(" + position + ")");
		chooserFragmentListener.onChosenOS(adapter.getOsType(position));
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "onClick");
	}

}
