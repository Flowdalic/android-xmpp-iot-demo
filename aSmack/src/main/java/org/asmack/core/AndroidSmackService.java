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

package org.asmack.core;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class AndroidSmackService extends Service {

	public static final String ACTION_START_SERVICE = Constants.PACKAGE + '.' +  "START_SERVICE";
	public static final String ACTION_STOP_SERVICE = Constants.PACKAGE + '.' + "STOP_SERVICE";

	static final String ACTION_NETWORK_CONNECTED = Constants.PACKAGE + '.' + "NETWORK_CONNECTED";
	static final String ACTION_NETWORK_DISCONNECTED = Constants.PACKAGE + '.' + "NETWORK_DISCONNECTED";
	static final String ACTION_NETWORK_TYPE_CHANGED = Constants.PACKAGE + '.' + "NETWORK_TYPE_CHANGED";

	private static final Set<String> HANDLE_ONCE_INTENT_ACTIONS = new HashSet<>();

	static {
		HANDLE_ONCE_INTENT_ACTIONS.add(ACTION_START_SERVICE);
		HANDLE_ONCE_INTENT_ACTIONS.add(ACTION_STOP_SERVICE);
	}

	private static final Logger LOGGER = Logger.getLogger(AndroidSmackService.class.getName());

	private static final String IS_RUNNING_KEY = "isRunning";

	private static boolean sIsRunning = false;
	private static SharedPreferences sSharedPreferences;

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	private AndroidSmackManager mAndroidSmackManager;

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			onHandleIntent((Intent) msg.obj);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		HandlerThread thread = new HandlerThread("AndroidSmackServiceHandler");
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		mAndroidSmackManager = AndroidSmackManager.getInstance(this);

		sSharedPreferences = getSharedPreferences("AndroidSmackService", MODE_PRIVATE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mServiceLooper.quit();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			intent = new Intent(ACTION_START_SERVICE);
		} else if (sIsRunning != sSharedPreferences.getBoolean(IS_RUNNING_KEY, false)) {

		}

		boolean stickyStart = true;
		final String action = intent.getAction();
		switch (action) {
			case ACTION_STOP_SERVICE:
				setIsRunning(false);
				stickyStart = false;
				break;
			case ACTION_START_SERVICE:
				setIsRunning(true);
		}

		if (!sIsRunning && !action.endsWith(ACTION_START_SERVICE)) {
			LOGGER.fine("Service not running and action (" + action
					+ ") not start. Don't start sticky");
			stickyStart = false;
		}

		Message message = mServiceHandler.obtainMessage();
		message.obj = intent;
		message.what = intent.getAction().hashCode();
		mServiceHandler.sendMessage(message);

		return stickyStart ? START_STICKY : START_NOT_STICKY;
	}

	private void onHandleIntent(Intent intent) {

		final String action = intent.getAction();
		if (HANDLE_ONCE_INTENT_ACTIONS.contains(action) && mServiceHandler.hasMessages(action.hashCode())) {
			LOGGER.fine("Not handling " + action + " because another intent is in the queue");
			return;
		}

		switch (action) {
			case ACTION_START_SERVICE:
				NetworkConnectivityReceiver.register(this);
				mAndroidSmackManager.connectConnections();
				break;
			case ACTION_STOP_SERVICE:
				NetworkConnectivityReceiver.unregister(this);
				mAndroidSmackManager.disconnectConnections();
				break;
			case ACTION_NETWORK_CONNECTED:
				break;
			case ACTION_NETWORK_DISCONNECTED:
				break;
			case ACTION_NETWORK_TYPE_CHANGED:
				break;
			default:
				throw new IllegalStateException("Unknown intent action: " + action);
		}
	}

	private static void setIsRunning(boolean isRunning) {
		sSharedPreferences.edit().putBoolean(IS_RUNNING_KEY, isRunning);
		sIsRunning = isRunning;
	}
}
