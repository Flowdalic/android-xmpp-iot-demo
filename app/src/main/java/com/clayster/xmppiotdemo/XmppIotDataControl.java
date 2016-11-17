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

import android.content.Context;
import android.widget.Toast;

import org.asmack.core.AbstractManagedXmppConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.iot.control.IoTControlManager;
import org.jivesoftware.smackx.iot.control.element.IoTSetResponse;
import org.jivesoftware.smackx.iot.control.element.SetBoolData;
import org.jivesoftware.smackx.iot.data.IoTDataManager;
import org.jivesoftware.smackx.iot.data.element.IoTDataField;
import org.jivesoftware.smackx.iot.data.element.IoTFieldsExtension;
import org.jivesoftware.smackx.iot.provisioning.IoTProvisioningManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XmppIotDataControl {

	private static final Logger LOGGER = Logger.getLogger(XmppIotThing.class.getName());

	private static XmppIotDataControl INSTANCE;

	public static synchronized XmppIotDataControl getInstance(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new XmppIotDataControl(context);
		}
		return INSTANCE;
	}

	private final Object mMainActivityLock = new Object();
	private MainActivity mMainActivity;

	private final Context mContext;
	private final XmppManager mXmppManager;
	private final Settings mSettings;

	private volatile boolean mContinousReadOut;

	private XmppIotDataControl(Context context) {
		mContext = context.getApplicationContext();
		mXmppManager = XmppManager.getInstance(mContext);
		mSettings = Settings.getInstance(mContext);
		mXmppManager.addXmppConnectionStatusListener((ma) -> {
			ma.addListener(new AbstractManagedXmppConnectionListener() {
				@Override
				public void authenticated(XMPPConnection connection, boolean resumed) {
					withMainActivity((ma) -> setGuiElements(ma, true));

					if (resumed) return;

					if (!mSettings.isIdentityModeApp()) return;

					final EntityBareJid thingJid = mSettings.getThingJid();
					if (thingJid == null) return;
					IoTProvisioningManager provisioningManager = IoTProvisioningManager.getInstanceFor(connection);
					if (!provisioningManager.isMyFriend(thingJid)) {
						withMainActivity((ma) -> Toast.makeText(ma, "Trying to befriend " + thingJid, Toast.LENGTH_LONG).show());
						try {
							provisioningManager.sendFriendshipRequest(thingJid);
						} catch (SmackException.NotConnectedException | InterruptedException e) {
							LOGGER.log(Level.WARNING, "Could not befriend thing", e);
						}
					}
				}
				@Override
				public void terminated() {
					withMainActivity((ma) -> setGuiElements(ma, false));
				}
			});
		});
	}

	private static void setGuiElements(MainActivity ma, boolean enabled) {
		ma.mReadOutButton.setEnabled(enabled);
		ma.mContiniousReadOutSwitch.setEnabled(enabled);
		ma.mControlSwitch.setEnabled(enabled);
		ma.mClaimThingActivityButton.setEnabled(enabled);
	}

	void mainActivityOnCreate(MainActivity mainActivity) {
		this.mMainActivity = mainActivity;

		mMainActivity.mReadOutButton.setOnClickListener((button) -> performReadOutAsync());
		mMainActivity.mControlSwitch.setOnCheckedChangeListener((button, isChecked) -> controlNotificationAlarmAsync(isChecked));
		mMainActivity.mContiniousReadOutSwitch.setOnCheckedChangeListener((button, isChecked) -> setContinousReadOut(isChecked));

		boolean connectionUsable = mXmppManager.isConnectionUseable();
	    setGuiElements(mainActivity, connectionUsable);
	}

	void mainActivityOnDestroy(MainActivity mainActivity) {
		assert (this.mMainActivity == mainActivity);

		synchronized (mMainActivityLock) {
			this.mMainActivity = null;
		}
	}

	private void withMainActivity(final WithActivity<MainActivity> withMainActivity) {
		synchronized (mMainActivityLock) {
			if (mMainActivity == null) return;
			mMainActivity.runOnUiThread(() -> withMainActivity.withActivity(mMainActivity));
		}
	}

	private void setContinousReadOut(boolean continousReadOut) {
		if (mContinousReadOut == continousReadOut) return;
		mContinousReadOut = continousReadOut;
		if (mContinousReadOut) {
			performContiniousReadOut();
		}
	}

	private void performContiniousReadOut() {
		Async.go(() -> {
			if (!mContinousReadOut) return;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOGGER.log(Level.INFO, "Interrupted", e);
			}
			if (mContinousReadOut) {
				performReadOut();
				performContiniousReadOut();
			}
		});
	}

	private void performReadOutAsync() {
		Async.go(() -> performReadOut());
	}

	private void performReadOut() {
		XMPPTCPConnection connection = mXmppManager.getXmppConnection();
		EntityFullJid fullThingJid = mXmppManager.getFullThingJidOrNotify();
		if (fullThingJid == null) return;

		LOGGER.info("Requesting read out from " + fullThingJid);

		IoTDataManager iotDataManager = IoTDataManager.getInstanceFor(connection);
		final List<IoTFieldsExtension> res;
		try {
			res = iotDataManager.requestMomentaryValuesReadOut(fullThingJid);
		} catch (SmackException.NoResponseException | XMPPErrorException | SmackException.NotConnectedException |InterruptedException e) {
			mXmppManager.withMainActivity((ma) -> Toast.makeText(mContext, "Could not perform read out: " + e, Toast.LENGTH_LONG).show());
			LOGGER.log(Level.WARNING, "Could not perform read out", e);
			return;
		}

		final List<? extends IoTDataField> dataFields = res.get(0).getNodes().get(0).getTimestampElements().get(0).getDataFields();

		mXmppManager.withMainActivity((ma) -> {
			ma.mIotSensorsLinearLayout.removeAllViews();
			for (IoTDataField field : dataFields) {
				IotSensorView iotSensorView = new IotSensorView(ma, field.getName(), field.getValueString());
				ma.mIotSensorsLinearLayout.addView(iotSensorView);
			}
		});
	}

	private void controlNotificationAlarmAsync(boolean torchMode) {
		Async.go(() -> controlNotificationAlarm(torchMode));
	}

	private void controlNotificationAlarm(boolean torchMode) {
		final XMPPTCPConnection connection = mXmppManager.getXmppConnection();
		final EntityFullJid fullThingJid = mXmppManager.getFullThingJidOrNotify();
		if (fullThingJid == null) return;

		SetBoolData setTorch = new SetBoolData(Constants.NOTIFICATION_ALARM, torchMode);
		IoTControlManager ioTControlManager = IoTControlManager.getInstanceFor(connection);

		LOGGER.info("Trying to control " + fullThingJid + " set torchMode=" + torchMode);

		try {
			final IoTSetResponse ioTSetResponse = ioTControlManager.setUsingIq(fullThingJid, setTorch);
		} catch (SmackException.NoResponseException | XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
			mXmppManager.withMainActivity((ma) -> Toast.makeText(mContext, "Could not control thing: " + e, Toast.LENGTH_LONG).show());
			LOGGER.log(Level.SEVERE, "Could not set data", e);
		}
	}
}
