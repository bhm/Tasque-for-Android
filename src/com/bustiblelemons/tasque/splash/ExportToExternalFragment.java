package com.bustiblelemons.tasque.splash;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.main.SettingsUtil;

public class ExportToExternalFragment extends SherlockFragment implements OnClickListener, OnTouchListener {

	public static final String FRAGMENT_TAG = "export";
	View view;
	private TextView no;
	private TextView yes;
	private Context context;

	public interface OnExportOptionsChosen {
		public void onExportOptionsChosen();
	}

	public interface OnShowExportOptions {
		public void onShowExportOptions();
	}

	private OnExportOptionsChosen onExportOptionsChosen;
	private TextView question;
	private String exportFile;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		onExportOptionsChosen = (OnExportOptionsChosen) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		context = getActivity().getApplicationContext();
		view = inflater.inflate(R.layout.fragment_export_to_external, null);
		yes = (TextView) view.findViewById(R.id.fragment_export_to_dropbox_yes);
		no = (TextView) view.findViewById(R.id.fragment_export_to_dropbox_no);
		question = (TextView) view.findViewById(R.id.fragment_export_to_dropbox_question);
		exportFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Tasque/sqlitebackend.db";
		question.setText(String.format(context.getString(R.string.fragment_export_to_dropbox_question), exportFile));
		yes.setOnClickListener(this);
		no.setOnClickListener(this);

		return view;
	}

	@Override
	public void onClick(View v) {
		boolean exportToExternal = false;
		switch (v.getId()) {
		case R.id.fragment_export_to_dropbox_yes:
			exportToExternal = true;
		case R.id.fragment_export_to_dropbox_no:
			SettingsUtil.setExportOnExit(context, exportToExternal);
			SettingsUtil.setSyncedDatabsePath(context, exportFile);
			onExportOptionsChosen.onExportOptionsChosen();
			getActivity().getSupportFragmentManager().popBackStack();
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}
}
