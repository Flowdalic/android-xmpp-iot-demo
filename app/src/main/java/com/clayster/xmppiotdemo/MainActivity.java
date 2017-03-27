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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
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

import org.jxmpp.jid.EntityBareJid;

import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

	private static final Logger LOGGER = Logger.getLogger(MainActivity.class.getName());

	private static final int REQUEST_ENABLE_BLUETOOTH = 1;

	private Settings mSettings;

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

	private LinearLayout mThingJidLinearLayout;

	private LinearLayout mOwnerJidLinearLayout;

	private LinearLayout mIotClaimedLinearLayout;

	LinearLayout mIotSensorsLinearLayout;
	LinearLayout mIotThingInfosLinearLayout;

	private TextView mMyJidTextView;
	private TextView mThingJidTextView;
	TextView mOwnerJidTextView;
	private TextView mClaimedJidTextView;

	private XmppManager xmppManager;
	private XmppIotDataControl mXmppIotDataControl;
	private XmppIotThing mXmppIotThing;

	ImageView myJidPresenceImageView;
	ImageView mThingJidPresenceImageView;
	ImageView mOwnerJidPresenceImageView;

	Button mClaimThingActivityButton;
	Button mReadOutButton;
	Switch mContiniousReadOutSwitch;

	Switch mControlSwitch;

	private XiotBluetoothLeManager mXiotBluetoothLeManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mSettings = Settings.getInstance(this);

		// Hack: Reset the owner to 'null'. Ideally we would want to keep the thing's state persistent and a register/unregister mechanism via an UI element.
		mSettings.saveOwner(null);

		if (mSettings.firstTimeSetupRequired()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Please choose App identity")
					.setMessage("This App can bei either operated as IoT data/control 'App', as IoT 'Thing'. Please choose one. The selection can be later changed in the App's settings.")
					.setNegativeButton("App", (d, i) -> {
						mSettings.firstTimeSetup(Settings.IdentityMode.app);
						reload();
					})
					.setPositiveButton("Thing", (d, i) -> {
						mSettings.firstTimeSetup(Settings.IdentityMode.thing);
						reload();
					})
					.create()
					.show();
		}

		mThingIdentityLinearLayout = (LinearLayout) findViewById(R.id.thing_identity_linear_layout);
		mAppIdentityLinearLayout = (LinearLayout) findViewById(R.id.app_identity_linear_layout);

		mThingJidLinearLayout = (LinearLayout) findViewById(R.id.thing_jid_linear_layout);
		mOwnerJidLinearLayout = (LinearLayout) findViewById(R.id.owner_jid_linear_layout);
		mIotClaimedLinearLayout = (LinearLayout) findViewById(R.id.iot_claiming_linear_layout);

		mSetupLinearLayout = (LinearLayout) findViewById(R.id.setup_linear_layout);
		mMainLinearLayout = (LinearLayout) findViewById(R.id.main_linear_layout);
		mIotSensorsLinearLayout = (LinearLayout) findViewById(R.id.iot_sensors_linear_layout);
		mIotThingInfosLinearLayout = (LinearLayout) findViewById(R.id.thing_information_infos_linear_layout);

		mMyJidTextView = (TextView) findViewById(R.id.my_jid_text_view);
		mThingJidTextView = (TextView) findViewById(R.id.thing_jid_text_view);
		mOwnerJidTextView = (TextView) findViewById(R.id.owner_jid_text_view);
		mClaimedJidTextView = (TextView) findViewById(R.id.claimed_jid_text_view);

		myJidPresenceImageView = (ImageView) findViewById(R.id.my_jid_presence_image_view);
		mThingJidPresenceImageView = (ImageView) findViewById(R.id.thing_jid_presence_image_view);
		mOwnerJidPresenceImageView = (ImageView) findViewById(R.id.owner_jid_presence_image_view);
		mClaimThingActivityButton = (Button) findViewById(R.id.claim_thing_activity_button);
		mReadOutButton = (Button) findViewById(R.id.read_out_button);
		mContiniousReadOutSwitch = (Switch) findViewById(R.id.continues_read_out_switch);
		mControlSwitch = (Switch) findViewById(R.id.control_switch);

		Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
		setSupportActionBar(mainToolbar);

		PreferenceManager.setDefaultValues(this, R.xml.xiot_preferences, false);

		mXmppIotDataControl = XmppIotDataControl.getInstance(this);
		mXmppIotThing = XmppIotThing.getInstance(this);
		xmppManager = XmppManager.getInstance(this);

		mXmppIotThing.mainActivityOnCreate(this);
		mXmppIotDataControl.mainActivityOnCreate(this);
		xmppManager.mainActivityOnCreate(this);

		mXiotBluetoothLeManager = XiotBluetoothLeManager.getInstance(this);

		synchronized (sMainActivityLock) {
			sMainActivity = this;
		}
	}

	public void configureButtonClicked(View view) {
		startActivity(new Intent(this, Setup.class));
	}

	public void claimThingButtonClicked(View view) {
		startActivity(new Intent(this, ClaimThingActivity.class));
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem claimThingOption = menu.findItem(R.id.action_claim_thing);
		// Only show the claim thing menu option if the identity mode is 'app'.
		claimThingOption.setVisible(mSettings.showClaimGuiElements());
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
			case R.id.action_claim_thing:
				startActivity(new Intent(this, ClaimThingActivity.class));
				res = true;
				break;
			case R.id.action_about:
				startActivity(new Intent(this, AboutActivity.class));
				res = true;
				break;
			case R.id.action_send_feedback:
				Feedback.shareFeedbackAsync(this);
				res = true;
				break;
		}
		return res;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mSettings.isBasicConfigurationDone()) {
			mSetupLinearLayout.setVisibility(View.GONE);
			mMainLinearLayout.setVisibility(View.VISIBLE);
		} else {
			mSetupLinearLayout.setBackgroundColor(View.VISIBLE);
			mMainLinearLayout.setVisibility(View.GONE);
		}

		int appIdentityVisibility = mSettings.isIdentityModeApp() ? View.VISIBLE : View.GONE;
		mAppIdentityLinearLayout.setVisibility(appIdentityVisibility);
		mThingJidLinearLayout.setVisibility(appIdentityVisibility);

		int thingIdentityVisibility = mSettings.isIdentityModeThing() ? View.VISIBLE : View.GONE;
		mThingIdentityLinearLayout.setVisibility(thingIdentityVisibility);
		mOwnerJidLinearLayout.setVisibility(thingIdentityVisibility);

		if (mSettings.getThingJid() != null) {
			mThingJidTextView.setText(mSettings.getThingJid());
		}

		if (mSettings.getOwner() != null) {
			mOwnerJidTextView.setText(mSettings.getOwner());
		}

		if (mSettings.getMyJid() != null) {
			mMyJidTextView.setText(mSettings.getMyJid());
		}

		if (mSettings.showClaimGuiElements()) {
			EntityBareJid claimedJid = mSettings.getClaimedJid();
			if (claimedJid != null) {
				mClaimThingActivityButton.setVisibility(View.GONE);
				mClaimedJidTextView.setText(claimedJid);
			} else {
				mClaimThingActivityButton.setVisibility(View.VISIBLE);
			}
			mIotClaimedLinearLayout.setVisibility(View.VISIBLE);
		} else {
			mIotClaimedLinearLayout.setVisibility(View.GONE);
		}

		if (mSettings.isIdentityModeThing()) {
			BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
			if (bluetoothAdapter != null) {
				if (!bluetoothAdapter.isEnabled()) {
					Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
				} else {
					mXiotBluetoothLeManager.enableManager();
				}
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_ENABLE_BLUETOOTH:
				switch (resultCode) {
					case RESULT_OK:
						mXiotBluetoothLeManager.enableManager();
						break;
				}
				break;
			default:
				throw new AssertionError();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		synchronized (sMainActivityLock) {
			sMainActivity = null;
		}
	}

	private void reload() {
		Intent intent = getIntent();
		finish();
		startActivity(intent);
	}

	private static final Object sMainActivityLock = new Object();
	private static MainActivity sMainActivity;

	static void withMainActivity(WithActivity withActivity) {
		synchronized (sMainActivityLock) {
			if (sMainActivity == null) return;

			sMainActivity.runOnUiThread(() -> {
				withActivity.withActivity(sMainActivity);
			});
		}
	}
}
