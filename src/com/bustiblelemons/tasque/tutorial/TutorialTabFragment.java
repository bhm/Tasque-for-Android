package com.bustiblelemons.tasque.tutorial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bustiblelemons.tasque.R;

/**
 * Created 10 Jun 2013
 */
public class TutorialTabFragment extends SherlockFragment implements OnTouchListener {

	private Bundle args;
	private int os_type;
	private int step;
	static String STEP = "step";
	public static String OS_TYPE = "os_type";
	private View view;

	static final int STEP_ONE = 1;
	static final int STEP_TWO = 2;
	static final int STEP_THREE = 3;

	private TextView hint;
	private TextView explanation;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		args = getArguments();
		step = args.getInt(STEP);
		os_type = args.getInt(OS_TYPE);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_tutorial_step, null);
		hint = (TextView) view.findViewById(R.id.synchronize_step_hint);
		explanation = (TextView) view.findViewById(R.id.synchronize_step_explanation);
		switch (step) {
		case STEP_ONE:
			hint.setText(R.string.tutorial_step_one_hint);
			break;
		case STEP_TWO:
			hint.setText(R.string.tutorial_step_two_hint);
			explanation.setVisibility(View.VISIBLE);
			break;
		case STEP_THREE:
			hint.setText(R.string.tutorial_step_three_hint);
			explanation.setVisibility(View.GONE);
			break;
		}
		setOS(os_type);
		return view;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

	public void setOS(int os) {
		switch (step) {
		case STEP_ONE:
			switch (os) {
			case OSChooserFragment.LINUX:
				explanation.setText(R.string.tutorial_step_one_linux_hint);
				break;
			case OSChooserFragment.ANDROID:
				explanation.setText(R.string.tutorial_step_one_android_hint);
				break;
			case OSChooserFragment.WINDOWS:
				explanation.setText(R.string.tutorial_step_one_windows_hint);
				break;
			case OSChooserFragment.OSX:
				explanation.setText(R.string.tutorial_step_one_mac_hint);
				break;
			}
			break;
		case STEP_TWO:
			switch (os) {
			case OSChooserFragment.LINUX:
				explanation.setText(R.string.tutorial_step_two_linux_hint);
				break;
			case OSChooserFragment.ANDROID:
				explanation.setText(R.string.tutorial_step_two_android_hint);
				break;
			case OSChooserFragment.WINDOWS:
				explanation.setText(R.string.tutorial_step_two_windows_hint);
				break;
			case OSChooserFragment.OSX:
				explanation.setText(R.string.tutorial_step_two_mac_hint);
				break;
			}
			break;
		}
	}
}
//