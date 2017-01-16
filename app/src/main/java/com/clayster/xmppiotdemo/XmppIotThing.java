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

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.asmack.core.AbstractManagedXmppConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PresenceTypeFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.iot.Thing;
import org.jivesoftware.smackx.iot.control.IoTControlManager;
import org.jivesoftware.smackx.iot.control.ThingControlRequest;
import org.jivesoftware.smackx.iot.control.element.SetBoolData;
import org.jivesoftware.smackx.iot.control.element.SetData;
import org.jivesoftware.smackx.iot.data.IoTDataManager;
import org.jivesoftware.smackx.iot.data.ThingMomentaryReadOutRequest;
import org.jivesoftware.smackx.iot.data.ThingMomentaryReadOutResult;
import org.jivesoftware.smackx.iot.data.element.IoTDataField;
import org.jivesoftware.smackx.iot.discovery.IoTClaimedException;
import org.jivesoftware.smackx.iot.discovery.IoTDiscoveryManager;
import org.jivesoftware.smackx.iot.discovery.ThingState;
import org.jivesoftware.smackx.iot.discovery.ThingStateChangeListener;
import org.jivesoftware.smackx.iot.discovery.element.Tag;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XmppIotThing implements ThingMomentaryReadOutRequest, ThingControlRequest, SensorEventListener {

	private static final String MANUFACTURER = "Clayster AB";
	private static final String MODEL = "XMPP IoT Demo";
	private static final String VERSION = "0.1";
	private static final String KEY = "42";

	private static final Logger LOGGER = Logger.getLogger(XmppIotThing.class.getName());

	private static XmppIotThing INSTANCE;

	public static synchronized XmppIotThing getInstance(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new XmppIotThing(context);
		}
		return INSTANCE;
	}

	private final Context mContext;

	private final Settings mSettings;

	private final Thing mThing;
	private ThingState mThingState;

	private final SensorManager mSensorManager;

	private final Sensor mTemperatureSensor;

	private final Sensor mGravitySensor;

	private final NotificationManager mNotificationManager;

	private final Object mMainActivityLock = new Object();
	private MainActivity mMainActivity;

	private XmppIotThing(Context context) {
		mContext = context.getApplicationContext();
		mSettings = Settings.getInstance(mContext);
		Thing.Builder thingBuilder = Thing.builder()
				.setSerialNumber(mSettings.getThingSerialNumber())
				.setMomentaryReadOutRequestHandler(this)
				.setControlRequestHandler(this);

		if (mSettings.isThingFullMetadata()) {
			thingBuilder.setManufacturer(MANUFACTURER)
					.setModel(MODEL)
					.setVersion(VERSION)
					.setKey(KEY);
		}

		mThing = thingBuilder.build();

		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		mTemperatureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		if (mTemperatureSensor != null) {
			mSensorManager.registerListener(this, mTemperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
		} else {
			LOGGER.info("No temperature sensor found");
		}

		if (mGravitySensor != null) {
			mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
		} else {
			LOGGER.info("No gravity sensor found");
		}

	 	XmppManager.getInstance(mContext).addXmppConnectionStatusListener((ma) -> {
			XMPPTCPConnection connection = ma.getConnection();
			IoTDataManager iotDataManager = IoTDataManager.getInstanceFor(connection);
			iotDataManager.installThing(mThing);
			IoTControlManager ioTControlManager = IoTControlManager.getInstanceFor(connection);
			ioTControlManager.installThing(mThing);
			ma.addListener(new AbstractManagedXmppConnectionListener() {
				@Override
				public void authenticated(XMPPConnection connection, boolean resumed) {
					if (resumed) return;
					onAuthenticated(connection);
				}
			});

			// Try to establish mutual subscription for every entity which we just allowed being
			// subscribed to our presence, if mutual subscription mode is enabled.
			connection.addPacketSendingListener((stanza) -> {
				if (!mSettings.isIdentityModeThing()) return;
				if (!mSettings.isMutualSubscriptionModeEnabled()) return;

				Presence subscribedPresence = (Presence) stanza;
				BareJid to = subscribedPresence.getTo().asBareJid();
				Roster roster = Roster.getInstanceFor(connection);
				try {
					roster.sendSubscriptionRequest(to);
				} catch (SmackException.NotLoggedInException e) {
					LOGGER.log(Level.WARNING, "Could not send subscription request", e);
				}
			}, PresenceTypeFilter.SUBSCRIBED);
		});
	}

	@Override
	public void momentaryReadOutRequest(ThingMomentaryReadOutResult callback) {
		List<IoTDataField> res = new ArrayList<>();

		addBatteryStateTo(res);

		if (mTemperatureSensor != null && mTemperature != null) {
			int temp = (int) (float) mTemperature;
			IoTDataField.IntField tempField = new IoTDataField.IntField("temperature", temp);
			res.add(tempField);
		}

		if (mGravitySensor != null && mGravity != null) {
			int gravity = (int) (float) mGravity;
			IoTDataField.IntField gravityField = new IoTDataField.IntField("gravity", gravity);
			res.add(gravityField);
		}

		callback.momentaryReadOut(res);
	}

	@Override
	public void processRequest(Jid from, Collection<SetData> setDatas) throws XMPPException.XMPPErrorException {
		SetBoolData flashControlData = null;
		for (SetData setData : setDatas) {
			if (!(setData instanceof  SetBoolData)) continue;
			if (!setData.getName().equals(Constants.NOTIFICATION_ALARM)) continue;
			flashControlData = (SetBoolData) setData;
			break;
		}

		if (flashControlData == null) {
			return;
		}

		if (flashControlData.getBooleanValue()) {
			setNotifcationAlarm(NotificationAlarm.on);
		} else {
			setNotifcationAlarm(NotificationAlarm.off);
		}
	}

	Float mTemperature;

	Float mGravity;

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		Sensor sensor = sensorEvent.sensor;
		if (sensor == mTemperatureSensor) {
			mTemperature = sensorEvent.values[0];
		} else if (sensor == mGravitySensor) {
			mGravity = sensorEvent.values[0];
		} else {
			LOGGER.warning("Unknown sensor: " + sensor);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {

	}

	private void addBatteryStateTo(Collection<IoTDataField> dataFields) {
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent intent = mContext.registerReceiver(null, intentFilter);

		int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
		IoTDataField.BooleanField chargingField = new IoTDataField.BooleanField("charging", isCharging);
		dataFields.add(chargingField);

		int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		boolean isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
		IoTDataField.BooleanField pluggedField = new IoTDataField.BooleanField("plugged", isPlugged);
		dataFields.add(pluggedField);

		int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		float batteryPercent = level / (float) scale;
		// Scale to percents.
		batteryPercent *= 100;
		IoTDataField.IntField batteryPercentField = new IoTDataField.IntField("batteryPrecent", (int) batteryPercent);
		dataFields.add(batteryPercentField);
	}

	private enum NotificationAlarm {
		on,
		off,
		;
	}

	private NotificationAlarm mNotificationAlarm = NotificationAlarm.off;


	private void setNotifcationAlarm(NotificationAlarm notificationAlarm) {
		if (mNotificationAlarm == notificationAlarm) return;

		switch (notificationAlarm) {
			case on:
				Notification.Builder builder = new Notification.Builder(mContext);
				builder.setContentTitle("XIOT Notification Alarm")
						.setContentText("XIOT Notification Alarm enabled!")
						.setDefaults(Notification.DEFAULT_LIGHTS)
						.setLights(0xff00ff00, 300, 100)
						.setAutoCancel(true)
						.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
						;

				mNotificationManager.notify(0, builder.build());

				mNotificationAlarm = notificationAlarm;
				break;
			case off:
				mNotificationManager.cancelAll();
				mNotificationAlarm = notificationAlarm;
				break;
		}
	}

	void mainActivityOnCreate(MainActivity mainActivity) {
		this.mMainActivity = mainActivity;

		for (Tag tag : mThing.getMetaTags()) {
			IotThingInfoView thingInfo = new IotThingInfoView(mainActivity, tag.getName(), tag.getValue());
			mainActivity.mIotThingInfosLinearLayout.addView(thingInfo);
		}
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

	private void onAuthenticated(XMPPConnection connection) {
		// If XIOT is not a thing, then we don't need to do anything here.
		if (!mSettings.isIdentityModeThing()) return;

		XmppManager.emptyRoster(connection);

		IoTDiscoveryManager iotDiscoveryManager = IoTDiscoveryManager.getInstanceFor(connection);

		mThingState = null;
		final int MAX_ATTEMPTS = 3;
		int attempts = 0;
		while (mThingState == null && attempts < MAX_ATTEMPTS) {
			try {
				try {
					mThingState = iotDiscoveryManager.registerThing(mThing);
				} catch (IoTClaimedException e) {
					iotDiscoveryManager.unregister();
				}
			} catch(XMPPException.XMPPErrorException | InterruptedException | SmackException e) {
				LOGGER.log(Level.WARNING, "Error registering thing", e);
			}
			attempts++;
		}
		if (mThingState == null) {
			LOGGER.log(Level.SEVERE, "Could not register thing after " + MAX_ATTEMPTS + " attempts");
			return;
		}

		withMainActivity((ma) -> {
			final Jid registry = mThingState.getRegistry();
			ma.runOnUiThread(() -> {
				Toast.makeText(ma, "Thing registered with " + registry, Toast.LENGTH_LONG).show();
			});

			// Thing was just registered, ensure that now owner is shown.
			ma.mOwnerJidTextView.setText(ma.getResources().getString(R.string.owner_jid_text_view_unclaimed));

			IotThingInfoView thingInfo = new IotThingInfoView(ma, "Registry", registry);
			replace(ma.mIotThingInfosLinearLayout, thingInfo);
		});

		mThingState.setThingStateChangeListener(new ThingStateChangeListener() {
			@Override
			public void owned(BareJid bareOwner) {
				EntityBareJid owner = bareOwner.asEntityBareJidIfPossible();
				if (owner == null) throw new IllegalStateException("Could not transform to entity bare JID: " + bareOwner);
				withMainActivity((ma) -> {
					ma.runOnUiThread(() -> {
						Toast.makeText(ma, "Thing owned by " + owner, Toast.LENGTH_LONG).show();
					});

					ma.mOwnerJidTextView.setText(owner);
					mSettings.saveOwner(owner);
				});
			}
		});
	}

	private static void replace(LinearLayout linearLayout, IotThingInfoView updatedThingInfoView) {
		removeOne(linearLayout, updatedThingInfoView.getThingInfoName());
		linearLayout.addView(updatedThingInfoView);
	}

	private static void removeOne(LinearLayout linearLayout, CharSequence infoName) {
		for (int i = 0; i < linearLayout.getChildCount(); i++) {
			View v = linearLayout.getChildAt(i);
			if (!(v instanceof IotThingInfoView)) continue;

			IotThingInfoView thingInfoView = (IotThingInfoView) v;

			if (!thingInfoView.getThingInfoName().equals(infoName)) continue;

			linearLayout.removeView(thingInfoView);
			break;
		}
	}
}
