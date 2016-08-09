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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.iot.data.IoTDataManager;
import org.jivesoftware.smackx.iot.data.element.IoTDataField;
import org.jivesoftware.smackx.iot.data.element.IoTFieldsExtension;
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

	private final Context mContext;
	private final XmppManager mXmppManager;
	private final Settings mSettings;


	private XmppIotDataControl(Context context) {
		mContext = context.getApplicationContext();
		mXmppManager = XmppManager.getInstance(mContext);
		mSettings = Settings.getInstance(mContext);
	}

	void performReadOutAsync() {
		Async.go(() -> performReadOut());
	}

	void performReadOut() {
		XMPPTCPConnection connection = mXmppManager.getXmppConnection();
		EntityFullJid fullOtherJid = mXmppManager.getFullOtherJid();

		LOGGER.info("Requesting read out from " + fullOtherJid);

		IoTDataManager iotDataManager = IoTDataManager.getInstanceFor(connection);
		final List<IoTFieldsExtension> res;
		try {
			res = iotDataManager.requestMomentaryValuesReadOut(fullOtherJid);
		} catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException |InterruptedException e) {
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
}
