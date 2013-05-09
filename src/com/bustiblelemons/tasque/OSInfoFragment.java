package com.bustiblelemons.tasque;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bustiblelemons.tasque.Values.FragmentArguments;
import com.bustiblelemons.tasque.Values.FragmentArguments.OsInfoFragment.OS;

public class OSInfoFragment extends SherlockFragment {

	View view;
	TextView hint;
	Bundle args;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_os_info, null);
		hint = (TextView) view.findViewById(R.id.fragment_os_info_text);
		args = getArguments();
		if (args != null) {
			int type = args.getInt(FragmentArguments.OsInfoFragment.OS_TYPE);
			switch (type) {
			case OS.LINUX:
				hint.setText(R.string.fragment_importer_hint_linux);
				break;
			case OS.MAC:
				hint.setText(R.string.fragment_importer_hint_mac);
				break;
			case OS.WINDOWS:
				hint.setText(R.string.fragment_importer_hint_windows);
				break;
			default:
				hint.setText(R.string.fragment_importer_hint_linux);
				break;
			}
		}
		return view;
	}
}