package com.bustiblelemons.tasque;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bustiblelemons.tasque.Values.FragmentArguments;

public class MultipleFilesChooserFragment extends SherlockFragment implements OnItemClickListener, OnTouchListener {

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

	private OnSyncedFileChosen syncedFileChosen;
	private TextView hint;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnSyncedFileChosen) {
			syncedFileChosen = (OnSyncedFileChosen) activity;
		} else {
			throw new ClassCastException(activity.getClass().getSimpleName() + " should implement "
					+ OnSyncedFileChosen.class.getSimpleName());
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_multiple_files, null);
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

}
