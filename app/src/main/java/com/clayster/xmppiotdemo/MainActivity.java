/*
 * Copyright ⓒ 2016 Florian Schmaus.
 *
 * This file is part of XIOT.
 *
 * XIOT is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XIOT is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XIOT.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.clayster.xmppiotdemo;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

	private Settings settings;

	/**
	 * The GUI elements shown if XIOT is not configured yet.
	 */
	private LinearLayout mSetupLinearLayout;

	/**
	 * The GUI elements shown after XIOT has been configured.
	 */
	private LinearLayout mMainLinearLayout;

	/**
	 * The GUI elements used by the 'app' identity.
	 */
	private LinearLayout mAppIdentityLinearLayout;

	/**
	 * The GUI elements used by the 'thing' identity.
	 */
	private LinearLayout mThingIdentityLinearLayout;

	LinearLayout mIotSensorsLinearLayout;
	LinearLayout mIotThingInfosLinearLayout;

	private TextView mMyJidTextView;
	private TextView mOtherJidTextView;

	private XmppManager xmppManager;
	private XmppIotDataControl mXmppIotDataControl;
	private XmppIotThing mXmppIotThing;

	ImageView myJidPresenceImageView;
	ImageView otherJidPresenceImageView;
	Button mReadOutButton;
	Switch mContiniousReadOutSwitch;

	Switch mControlSwitch;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mThingIdentityLinearLayout = (LinearLayout) findViewById(R.id.thing_identity_linear_layout);
		mAppIdentityLinearLayout = (LinearLayout) findViewById(R.id.app_identity_linear_layout);

		mSetupLinearLayout = (LinearLayout) findViewById(R.id.setup_linear_layout);
		mMainLinearLayout = (LinearLayout) findViewById(R.id.main_linear_layout);
		mIotSensorsLinearLayout = (LinearLayout) findViewById(R.id.iot_sensors_linear_layout);
		mIotThingInfosLinearLayout = (LinearLayout) findViewById(R.id.thing_information_infos_linear_layout);

		mMyJidTextView = (TextView) findViewById(R.id.my_jid_text_view);
		mOtherJidTextView = (TextView) findViewById(R.id.otherJidTextView);

		myJidPresenceImageView = (ImageView) findViewById(R.id.my_jid_presence_image_view);
		otherJidPresenceImageView = (ImageView) findViewById(R.id.other_jid_presence_image_view);
		mReadOutButton = (Button) findViewById(R.id.read_out_button);
		mContiniousReadOutSwitch = (Switch) findViewById(R.id.continues_read_out_switch);
		mControlSwitch = (Switch) findViewById(R.id.control_switch);

		Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
		setSupportActionBar(mainToolbar);

		PreferenceManager.setDefaultValues(this, R.xml.xiot_preferences, false);

		settings = Settings.getInstance(this);

		mXmppIotThing = XmppIotThing.getInstance(this);
		mXmppIotThing.mainActivityOnCreate(this);

		xmppManager = XmppManager.getInstance(this);
		xmppManager.mainActivityOnCreate(this);

		mXmppIotDataControl = XmppIotDataControl.getInstance(this);
		mXmppIotDataControl.mainActivityOnCreate(this);
	}

	public void configureButtonClicked(View view) {
		startActivity(new Intent(this, Setup.class));
	}

	public void enableSwitchToggled(View view) {
		Switch enableSwitch = (Switch) view;
		if (enableSwitch.isChecked()) {
			xmppManager.enable();
		} else {
			xmppManager.disable();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean res = false;
		switch (item.getItemId()) {
			case R.id.action_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				res = true;
				break;
		}
		return res;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (settings.isBasicConfigurationDone()) {
			mSetupLinearLayout.setVisibility(View.GONE);
			mMainLinearLayout.setVisibility(View.VISIBLE);
		} else {
			mSetupLinearLayout.setBackgroundColor(View.VISIBLE);
			mMainLinearLayout.setVisibility(View.GONE);
		}

		int appIdentityVisibility = settings.isIdentityModeApp() ? View.VISIBLE : View.GONE;
		mAppIdentityLinearLayout.setVisibility(appIdentityVisibility);

		int thingIdentityVisibility = settings.isIdentityModeThing() ? View.VISIBLE : View.GONE;
		mThingIdentityLinearLayout.setVisibility(thingIdentityVisibility);

		if (settings.getOtherJid() != null) {
			mOtherJidTextView.setText(settings.getOtherJid());
		}

		if (settings.getMyJid() != null) {
			mMyJidTextView.setText(settings.getMyJid());
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		xmppManager.mainActivityOnDestroy(this);
		mXmppIotDataControl.mainActivityOnDestroy(this);
		mXmppIotThing.mainActivityOnDestroy(this);
	}
}
