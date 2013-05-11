package com.bustiblelemons.tasque.splash;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.main.ExportToExternalFragment.OnShowExportOptions;
import com.bustiblelemons.tasque.main.SettingsUtil;
import com.bustiblelemons.tasque.utilities.Database;
import com.bustiblelemons.tasque.utilities.Values.FragmentArguments;

public class MultipleFilesChooserFragment extends SherlockFragment implements OnItemClickListener, OnTouchListener, OnClickListener {

	private View view;
	private ListView listView;
	private MultipleFilesAdapter adapter;
	private Context context;
	private Bundle args;

	public interface OnMultipleFilesDetected {
		public void onShowMultipleFilesDetected(ArrayList<File> files, boolean lostPrevious);
	}

	public interface OnSyncedFileChosen {
		public void syncedFileChosen(String filePath);
	}
	
	private OnShowExportOptions showExportOptions;
	private OnSyncedFileChosen syncedFileChosen;
	private TextView hint;
	private TextView startFresh;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnSyncedFileChosen) {
			syncedFileChosen = (OnSyncedFileChosen) activity;
		} else {
			throw new ClassCastException(activity.getClass().getSimpleName() + " should implement "
					+ OnSyncedFileChosen.class.getSimpleName());
		}
		showExportOptions = (OnShowExportOptions) activity; 
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_multiple_files, null);
		startFresh = (TextView) view.findViewById(R.id.fragment_multiple_start_fresh);
		startFresh.setOnClickListener(this);
		listView = (ListView) view.findViewById(R.id.fragment_multiple_list);
		hint = (TextView) view.findViewById(R.id.fragment_multiple_hint);
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		getSherlockActivity().getSupportActionBar().hide();
		args = getArguments();
		if (args.getBoolean(FragmentArguments.MultipleFiles.LOST_PREVIOUS)) {
			hint.setText(R.string.fragment_multiple_hint);
		}
		context = getActivity().getApplicationContext();
		@SuppressWarnings("unchecked")
		ArrayList<File> files = (ArrayList<File>) args.getSerializable(FragmentArguments.MultipleFiles.FILES_FOUND);
		adapter = new MultipleFilesAdapter(context, files);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		File f = (File) arg0.getAdapter().getItem(arg2);
		syncedFileChosen.syncedFileChosen(f.getAbsolutePath());
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.fragment_multiple_start_fresh:
			try {
				Database.creatFreshDatabase(context);
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				SettingsUtil.setStartedFresh(context, true);
				showExportOptions.onShowExportOptions();
			}	
		default:
			break;
		}
		
	}

}
