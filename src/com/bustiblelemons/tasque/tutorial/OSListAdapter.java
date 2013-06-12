package com.bustiblelemons.tasque.tutorial;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bustiblelemons.tasque.R;

/**
 * Created 11 Jun 2013
 */
public class OSListAdapter extends BaseAdapter {

	private Context context;
	private String[] OS = new String[] { "Linux", "Android", "Windows", "OSX" };
	private Resources resources;

	public OSListAdapter(Context context) {
		this.context = context;
	}

	@Override
	public int getCount() {
		return OS.length;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	public int getOsType(int position) {
		switch (position) {
		case 0:
			return OSChooserFragment.LINUX;
		case 1:
			return OSChooserFragment.ANDROID;
		case 2:
			return OSChooserFragment.WINDOWS;
		case 3:
			return OSChooserFragment.OSX;
		default:
			return OSChooserFragment.LINUX;
		}
	}

	private Drawable getDrawable(int position) {
		resources = context.getResources();
		switch (position) {
		case 0:
			return resources.getDrawable(OSChooserFragment.LINUX);
		case 1:
			return resources.getDrawable(OSChooserFragment.ANDROID);
		case 2:
			return resources.getDrawable(OSChooserFragment.WINDOWS);
		case 3:
			return resources.getDrawable(OSChooserFragment.OSX);
		default:
			return resources.getDrawable(OSChooserFragment.LINUX);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = LayoutInflater.from(context).inflate(R.layout.single_os_chooser_row, null);
		} else {
			view = convertView;
		}
		TextView title = (TextView) view.findViewById(R.id.single_os_chooser_title);
		title.setText(OS[position]);
		Drawable d = getDrawable(position);
		Log.d("Tasque", "getView(" +position + ",\t" + convertView + ",\t" + parent + "\tDrawable:" + d);
		title.setCompoundDrawables(d, null, null, null);
		return view;
	}
}
