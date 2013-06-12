package com.bustiblelemons.tasque.rtm;

import it.bova.rtmapi.Token;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bustiblelemons.tasque.rtm.RTMAuthFragment.OnCompleteAuthentication;
import com.bustiblelemons.tasque.rtm.RTMAuthFragment.OnDetachRTMAuthFragment;
import com.bustiblelemons.tasque.settings.SettingsUtil;

/**
 * Created 5 Jun 2013
 */
public class RTMLoginFromSettings extends SherlockFragmentActivity implements OnTouchListener, OnDetachRTMAuthFragment,
		OnCompleteAuthentication {

	private FragmentManager fmanager;
	private RTMAuthFragment rtmAuthFragment;
	private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getApplicationContext();
		rtmAuthFragment = new RTMAuthFragment();
		fmanager = getSupportFragmentManager();
		FragmentTransaction transaction = fmanager.beginTransaction();
		transaction.add(android.R.id.content, rtmAuthFragment, RTMAuthFragment.FRAGMENT_TAG);
		transaction.commit();
	}

	@Override
	public void onCompleteAuthentication(Object token) {
		try {
			SettingsUtil.saveRTMToken(context, (Token) token);
			SettingsUtil.setUseRTMBackend(context, true);
			FragmentTransaction t = fmanager.beginTransaction();
			t.remove(rtmAuthFragment);
			t.commit();
			this.finish();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDetachRTMAuthFragment() {
		fmanager.beginTransaction().remove(rtmAuthFragment).commit();
		this.finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return rtmAuthFragment.isVisible() ? rtmAuthFragment.onKeyDown(keyCode, event) : super
				.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}
}
