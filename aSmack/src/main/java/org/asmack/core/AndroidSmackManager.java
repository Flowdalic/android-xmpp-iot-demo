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

import android.content.Context;
import android.content.Intent;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringTransformer;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AndroidSmackManager {

	private static final Logger LOGGER = Logger.getLogger(AndroidSmackManager.class.getName());

	static {
		// Some network types, especially GPRS or EDGE is rural areas have a very slow response
		// time. Smack's default packet reply timeout of 5 seconds is way to low for such networks,
		// so we increase it to 2 minutes.
		// This value must also be greater then the highest returned bundle and defer value.
		SmackConfiguration.setDefaultPacketReplyTimeout(2 * 60 * 1000);

		SmackConfiguration.addDisabledSmackClass("org.jivesoftares.smack.legacy");
		SmackConfiguration.addDisabledSmackClass("org.jivesoftware.smack.java7");
		SmackConfiguration.addDisabledSmackClass("org.jivesoftware.smackx.iot");

		DNSUtil.setIdnaTransformer(new StringTransformer() {
			@Override
			public String transform(String string) {
				return java.net.IDN.toASCII(string);
			}
		});
		SASLMechanism.setSaslPrepTransformer(new StringTransformer() {
			@Override
			public String transform(String string) {
				return Normalizer.normalize(string, Normalizer.Form.NFKC);
			}
		});
	}

	private static AndroidSmackManager INSTANCE;

	public static synchronized AndroidSmackManager getInstance(Context context) {
		Objects.requireNonNull(context, "Context argument must not be null");
		if (INSTANCE == null) {
			INSTANCE = new AndroidSmackManager(context);
		}
		return INSTANCE;
	}

	private final Context context;

	private final Map<XMPPTCPConnection, ManagedXmppConnection<XMPPTCPConnection>> mManagedConnections = new WeakHashMap<>();

	private final Set<NewManagedConnectionListener> mNewConnectionListeners = new CopyOnWriteArraySet<>();

	private AndroidSmackManager(Context context) {
		this.context = context;
	}

	public XMPPTCPConnection createManagedConnection(XMPPTCPConnectionConfiguration configuration) {
		XMPPTCPConnection connection = new XMPPTCPConnection(configuration);
		ManagedXmppConnection managedConnection = new ManagedXmppConnection(connection);

		for (NewManagedConnectionListener newManagedConnectionListener : mNewConnectionListeners) {
			newManagedConnectionListener.newConnection(managedConnection);
		}

		synchronized (managedConnection) {
			mManagedConnections.put(connection, managedConnection);
		}

		return connection;
	}

	public ManagedXmppConnection<XMPPTCPConnection> getManagedXmppConnectionFor(XMPPTCPConnection connection) {
		synchronized (mManagedConnections) {
			return mManagedConnections.get(connection);
		}
	}

	public void disconnectAndUnmanage(XMPPTCPConnection connection) {
		connection.disconnect();
		synchronized (mManagedConnections) {
			mManagedConnections.remove(connection);
		}
	}

	public void addNewManagedConnectionListener(NewManagedConnectionListener newManagedConnectionListener) {
		mNewConnectionListeners.add(newManagedConnectionListener);
	}

	public void removeNewManagedConnectionListener(NewManagedConnectionListener newManagedConnectionListener) {
		mNewConnectionListeners.remove(newManagedConnectionListener);
	}

	public void enable() {
		sendIntentToService(AndroidSmackService.ACTION_START_SERVICE);
	}

	public void disable() {
		sendIntentToService(AndroidSmackService.ACTION_STOP_SERVICE);
	}

	private void sendIntentToService(String action) {
		Intent intent = new Intent(context, AndroidSmackService.class);
		intent.setAction(action);
		context.startService(intent);
	}

	void connectConnections() {
		forAllManagedConnections(new WithXmppConnection<XMPPTCPConnection>() {
			@Override
			public void withXmppConnection(XMPPTCPConnection connection, ManagedXmppConnection managedXmppConnection) {
				try {
					connection.connect();
				} catch (SmackException | XMPPException | InterruptedException | IOException e) {
					LOGGER.log(Level.WARNING, "connect() throw", e);

					Iterator<ManagedXmppConnectionListener> it = managedXmppConnection.getListeners().iterator();
					while (it.hasNext()) {
						ManagedXmppConnectionListener listener = it.next();
						listener.connectionAttemptFailed(e, managedXmppConnection);
					}

					return;
				}

				try {
					connection.login();
				} catch (SmackException | XMPPException | InterruptedException | IOException e) {
					LOGGER.log(Level.WARNING, "login() throw", e);

					Iterator<ManagedXmppConnectionListener> it = managedXmppConnection.getListeners().iterator();
					while (it.hasNext()) {
						ManagedXmppConnectionListener listener = it.next();
						listener.loginAttemptFailed(e, managedXmppConnection);
					}

					return;
				}
			}
		});
	}

	void disconnectConnections() {
		forAllManagedConnections(new WithXmppConnection<XMPPTCPConnection>() {
			@Override
			public void withXmppConnection(XMPPTCPConnection connection, ManagedXmppConnection managedXmppConnection) {
				connection.disconnect();
			}
		});
	}

	void forAllManagedConnections(WithXmppConnection<XMPPTCPConnection> withXmppConnection) {
		synchronized (mManagedConnections) {
			for (ManagedXmppConnection<XMPPTCPConnection> managedXmppConnection : mManagedConnections.values()) {
				final XMPPTCPConnection connection = managedXmppConnection.getConnection();
				if (connection == null) {
					mManagedConnections.remove(managedXmppConnection);
					continue;
				}
				withXmppConnection.withXmppConnection(connection, managedXmppConnection);
			}
		}
	}

	interface WithXmppConnection<C extends XMPPConnection> {
		void withXmppConnection(C connection, ManagedXmppConnection<C> managedConnection);
	}
}
