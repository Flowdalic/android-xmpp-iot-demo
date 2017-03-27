/*
 * Copyright ⓒ 2017 Florian Schmaus.
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
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.widget.Toast;

import org.jivesoftware.smack.util.Async;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class XiotBluetoothLeManager {

	private static final Logger LOGGER = Logger.getLogger(XiotBluetoothLeManager.class.getName());

	private static XiotBluetoothLeManager INSTANCE;

	static synchronized XiotBluetoothLeManager getInstance(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new XiotBluetoothLeManager(context);
		}
		return INSTANCE;
	}

	private final Context mContext;
	private final BluetoothManager mBluetoothManager;
	private final BluetoothAdapter mBluetoothAdapter;

	private BluetoothGatt mPolarH7BluetoothGatt;

	private int heartRate = -1;
	private long heartRateTimestamp = -1;

	private XiotBluetoothLeManager(Context context) {
		mContext = context;
		mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();

		context.registerReceiver(mBluetoothAdapterStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	}

	void enableManager() {
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) return;

		if (mPolarH7BluetoothGatt != null) return;

		startBleDeviceDiscovery();
	}

	void disableManager() {
		resetHeartRateInformation();
		if (mPolarH7BluetoothGatt == null) return;

		mPolarH7BluetoothGatt.close();
		mPolarH7BluetoothGatt = null;
	}

	private final BroadcastReceiver mBluetoothAdapterStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
			switch (state) {
				case BluetoothAdapter.STATE_TURNING_OFF:
					disableManager();
					break;
			}
		}
	};

	private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			String name = device.getName();
			BluetoothClass bluetoothClass = device.getBluetoothClass();
			String address = device.getAddress();
			int type = device.getType();
			ParcelUuid[] uuids = device.getUuids();

			StringBuilder deviceInfo = new StringBuilder();
			deviceInfo.append(name + " - " + bluetoothClass + " (" + address + ',' + type + ") [");
			if (uuids != null) {
				for (ParcelUuid uuid : uuids) {
					deviceInfo.append(uuid).append(", ");
				}
			}
			deviceInfo.append(']');

			LOGGER.info("Found Bluetooth device '" + deviceInfo + "' with rssi " + rssi);

			if (!name.startsWith("Polar H7")) return;

			MainActivity.withMainActivity((ma) -> {
				Toast.makeText(ma, "Found Polar H7 device, trying to discover services", Toast.LENGTH_SHORT).show();
			});
			stopBleDeviceDiscovery();

			device.connectGatt(mContext, true, mBluetoothGattCallback);
		}
	};

	private final static UUID UUID_HEART_RATE_MEASURMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
	private final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			switch (newState) {
				case BluetoothProfile.STATE_CONNECTED:
					LOGGER.info("GATT connected: " + gatt);
					gatt.discoverServices();
					break;
				case BluetoothProfile.STATE_DISCONNECTED:
					LOGGER.info("GATT disconnected: " + gatt);
					MainActivity.withMainActivity((ma) -> {
						Toast.makeText(ma, "Connection to Polar H7 lost", Toast.LENGTH_SHORT).show();
					});
					mPolarH7BluetoothGatt = null;
					resetHeartRateInformation();
					break;
				default:
					throw new AssertionError();
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status != BluetoothGatt.GATT_SUCCESS) {
				return;
			}

			List<BluetoothGattService> gattServices = gatt.getServices();

			StringBuilder sb = new StringBuilder();
			sb.append("Discovered services: ");
			for (BluetoothGattService bgs : gattServices) {
				sb.append("GATT service: ").append(bgs.getUuid()).append(", ")
						.append(bgs.getInstanceId()).append(", ").append(bgs.getType()).append(" [");
				List<BluetoothGattCharacteristic> characteristics = bgs.getCharacteristics();
				for (BluetoothGattCharacteristic bgc : characteristics) {
					if (bgc.getUuid().equals(UUID_HEART_RATE_MEASURMENT)) {

						LOGGER.info("Found heart rate measurement characteristic, enabling notifications");
						MainActivity.withMainActivity((ma) -> {
							Toast.makeText(ma, "Found heart rate measurement characteristic of Polar H7", Toast.LENGTH_SHORT).show();
						});

						gatt.setCharacteristicNotification(bgc, true);
						BluetoothGattDescriptor descriptor = bgc.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
						descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
						boolean res = gatt.writeDescriptor(descriptor);
						if (!res) {
							LOGGER.severe("Write operation enabling notification for value on GATT was NOT successful");
						} else {
							mPolarH7BluetoothGatt = gatt;
						}
					}

					sb.append(bgc.getUuid()).append(" {");
					List<BluetoothGattDescriptor> descriptors = bgc.getDescriptors();
					for (BluetoothGattDescriptor bgd : descriptors){
						sb.append(bgd).append(", ");
					}
					sb.append("} ");
				}
				sb.append(']');
			}

			LOGGER.info(sb.toString());
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status != BluetoothGatt.GATT_SUCCESS) {
				return;
			}

			newCharacteristicData(characteristic);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			newCharacteristicData(characteristic);
		}

		private void newCharacteristicData(BluetoothGattCharacteristic characteristic) {
			UUID uuid = characteristic.getUuid();
			if (UUID_HEART_RATE_MEASURMENT.equals(uuid)) {
				int flag = characteristic.getProperties();
				int format = -1;
				if ((flag & 0x01) != 0) {
					format = BluetoothGattCharacteristic.FORMAT_UINT16;
				} else {
					format = BluetoothGattCharacteristic.FORMAT_UINT8;
				}
				heartRate = characteristic.getIntValue(format, 1);
				heartRateTimestamp = System.currentTimeMillis();
				LOGGER.info("Found heart rate: " + heartRate);
			} else {
				byte[] data = characteristic.getValue();
				StringBuilder sb = new StringBuilder(data.length * 2);
				for (byte b : data) {
					sb.append(String.format("%02X ", b));
				}
				LOGGER.info("Characteristic read " + uuid + " value: " + sb.toString());
			}
		}
	};

	// Stop scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;

	private volatile boolean mBluetoothLeScanOngoing;

	void startBleDeviceDiscovery() {

		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) return;

		if (mBluetoothLeScanOngoing) return;

		Async.go(() -> {
			try {
				Thread.sleep(SCAN_PERIOD);
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}

			stopBleDeviceDiscovery();
		});

		LOGGER.info("Starting Bluetooth LE scan on " + mBluetoothAdapter);
		MainActivity.withMainActivity((ma) -> {
			Toast.makeText(ma, "Starting Bluetooth LE scan", Toast.LENGTH_SHORT).show();
		});
		mBluetoothLeScanOngoing = true;
		mBluetoothAdapter.startLeScan(mLeScanCallback);
	}

	int getHeartRate() {
		return heartRate;
	}

	private void stopBleDeviceDiscovery() {
		LOGGER.info("Stopping Bluetooth LE scan on " + mBluetoothAdapter);
		mBluetoothLeScanOngoing = false;
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
	}

	private void resetHeartRateInformation() {
		heartRate = -1;
		heartRateTimestamp = -1;
	}
}
