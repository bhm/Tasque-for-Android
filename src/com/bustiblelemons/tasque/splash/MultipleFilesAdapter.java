package com.bustiblelemons.tasque.splash;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.bustiblelemons.tasque.R;
import com.bustiblelemons.tasque.main.SettingsUtil;
import com.bustiblelemons.tasque.utilities.Utility;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MultipleFilesAdapter extends BaseAdapter {

	private Context context;
	private ArrayList<File> files;

	public MultipleFilesAdapter(Context context, ArrayList<File> files) {
		this.context = context;
		this.files = files;
	}

	@Override
	public int getCount() {
		return this.files.size();
	}

	public String getPath(int position) {
		return files.get(position).getAbsolutePath();
	}

	@Override
	public Object getItem(int position) {
		return files.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = LayoutInflater.from(context).inflate(R.layout.single_row_multiple_files, null);
		} else {
			view = convertView;
		}
		TextView title = (TextView) view.findViewById(R.id.single_row_multiple_files_title);
		TextView date = (TextView) view.findViewById(R.id.single_row_multiple_files_date);
		TextView size = (TextView) view.findViewById(R.id.single_row_multiple_files_size);
		File f = files.get(position);
		title.setText(f.getParentFile() + "/" + f.getName());
		Utility.applyFontSize(title);
		String dateString = (new SimpleDateFormat(SettingsUtil.getDateFromat(context))).format(new Date(f
				.lastModified()));
		date.setText(dateString);
		size.setText((f.length() / 1024) + "kb");
		return view;
	}

}
