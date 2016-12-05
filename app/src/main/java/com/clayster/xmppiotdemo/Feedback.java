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

import org.jivesoftware.smack.util.Async;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

class Feedback {

	private static final Logger LOGGER = Logger.getLogger(Feedback.class.getName());

	static void shareFeedbackAsync(Context context) {
		Async.go(() -> shareFeedback(context));
	}

	static void shareFeedback(Context context) {
		StringBuilder sb = new StringBuilder(4096);
		boolean res = appendLog(sb);
		if (!res) return;

		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
		context.startActivity(intent);
	}

	private static boolean appendLog(StringBuilder sb) {
		final String[] command = new String[] { "logcat", "-d", "-v", "time"};

		Process process;
		try {
			process = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error retrieving log", e);
			return false;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error retrieving log", e);
			return false;
		}

		return true;
	}

	/*
	private void sendFeedbackOld() {
		Intent intent = new Intent(Intent.ACTION_BUG_REPORT);
		intent.setComponent(new ComponentName("com.google.android.gms", "com.google.android.gms.feedback.LegacyBugReportService"));
		ServiceConnection serviceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName componentName, IBinder service) {
				try {
					service.transact(Binder.FIRST_CALL_TRANSACTION, Parcel.obtain(), null, 0);
				} catch (RemoteException e) {
					LOGGER.log(Level.WARNING, "Could not start service", e);
				}
			}
			@Override
			public void onServiceDisconnected(ComponentName componentName) {
			}
		};
		this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	private void sendFeedback() {
		ApplicationErrorReport report = new ApplicationErrorReport();
		report.packageName = getApplication().getPackageName();
		report.time = System.currentTimeMillis();
		report.type = ApplicationErrorReport.TYPE_NONE;

		Intent intent = new Intent(Intent.ACTION_APP_ERROR);
		intent.putExtra(Intent.EXTRA_BUG_REPORT, report);
		startActivity(intent);
	}
	*/
}
