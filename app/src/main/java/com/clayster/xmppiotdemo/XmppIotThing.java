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
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;

import org.asmack.core.AndroidSmackManager;
import org.jivesoftware.smack.XMPPException;
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
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class XmppIotThing implements ThingMomentaryReadOutRequest, ThingControlRequest, SensorEventListener {

	private static final String MANUFACTURER = "Clayster AB";
	private static final String MODEL = "XMPP IoT Demo";
	private static final String VERSION = "0.1";
	private static final String SN = "1";
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

	private final Thing mThing;

	private final SensorManager mSensorManager;

	private final Sensor mTemperatureSensor;

	private final Sensor mGravitySensor;

	private XmppIotThing(Context context) {
		mContext = context.getApplicationContext();
		mThing = Thing.builder()
				.setManufacturer(MANUFACTURER)
				.setModel(MODEL)
				.setVersion(VERSION)
				.setSerialNumber(SN)
				.setKey(KEY)
				.setMomentaryReadOutRequestHandler(this)
				.setControlRequestHandler(this)
				.build();

		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		mTemperatureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

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

		AndroidSmackManager androidSmackManager = AndroidSmackManager.getInstance(context);
		androidSmackManager.addNewManagedConnectionListener((connection) -> {
			onNewConnection(connection);
		});
	}

	private void onNewConnection(XMPPTCPConnection connection) {
		IoTDataManager iotDataManager = IoTDataManager.getInstanceFor(connection);
		iotDataManager.installThing(mThing);
		IoTControlManager ioTControlManager = IoTControlManager.getInstanceFor(connection);
		ioTControlManager.installThing(mThing);
//		IoTProvisioningManager ioTProvisioningManager = IoTProvisioningManager.getInstanceFor(connection);
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
			if (!setData.getName().equals("flashlight")) continue;
			flashControlData = (SetBoolData) setData;
			break;
		}

		if (flashControlData == null) {
			return;
		}

		if (flashControlData.getBooleanValue()) {
			setFlashLightState(FlashLightState.on);
		} else {
			setFlashLightState(FlashLightState.off);
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
		boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
		IoTDataField.BooleanField chargingField = new IoTDataField.BooleanField("charging", isCharging);
		dataFields.add(chargingField);

		int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		float batteryPercent = level / (float) scale;
		IoTDataField.IntField batteryPercentField = new IoTDataField.IntField("batteryPrecent", (int) batteryPercent);
		dataFields.add(batteryPercentField);
	}

	private enum FlashLightState {
		on,
		off,
		;
	}

	private Camera mCamera;
	private FlashLightState mFlashLightState = FlashLightState.off;


	private void setFlashLightState(FlashLightState flashLightState) {
		if (mFlashLightState == flashLightState) return;

		switch (flashLightState) {
			case on:
				if (mCamera != null) {
					mCamera.release();
				}
				mCamera = Camera.open();
				Camera.Parameters parameters = mCamera.getParameters();
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
				mCamera.setParameters(parameters);
				mCamera.startPreview();
				mFlashLightState = flashLightState;
				break;
			case off:
				if (mCamera == null) {
					break;
				}
				mCamera.startPreview();
				mCamera.release();
				mFlashLightState = flashLightState;
				break;
		}
	}
}
