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

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ManagedXmppConnection<C extends XMPPConnection> {

	private final WeakReference<C> mConnection;
	private final XmppConnectionStatus mStatus = new XmppConnectionStatus();
	private final Set<ManagedXmppConnectionListener> mListeners = new CopyOnWriteArraySet<>();
	private final ConnectionListener mConnectionListener;

	ManagedXmppConnection(C connection) {
		mConnection = new WeakReference<>(connection);

		mConnectionListener = new ConnectionListener() {
			@Override
			public void connected(XMPPConnection connection) {
				for (ManagedXmppConnectionListener listener : mListeners) {
					listener.connected(connection);
				}
			}

			@Override
			public void authenticated(XMPPConnection connection, boolean resumed) {
				for (ManagedXmppConnectionListener listener : mListeners) {
					listener.authenticated(connection, resumed);
				}
			}

			@Override
			public void connectionClosed() {
				for (ManagedXmppConnectionListener listener : mListeners) {
					listener.connectionClosed();
					listener.terminated();
				}
			}

			@Override
			public void connectionClosedOnError(Exception e) {
				for (ManagedXmppConnectionListener listener : mListeners) {
					listener.connectionClosedOnError(e);
					listener.terminated();
				}
			}

			@Override
			public void reconnectionSuccessful() {
				for (ManagedXmppConnectionListener listener : mListeners) {
					listener.reconnectionSuccessful();
				}
			}

			@Override
			public void reconnectingIn(int seconds) {
				for (ManagedXmppConnectionListener listener : mListeners) {
					listener.reconnectingIn(seconds);
				}
			}

			@Override
			public void reconnectionFailed(Exception e) {
				for (ManagedXmppConnectionListener listener : mListeners) {
					listener.reconnectionFailed(e);
				}
			}
		};
		connection.addConnectionListener(mConnectionListener);
	}

	public C getConnection() {
		return mConnection.get();
	}

	public void addListener(ManagedXmppConnectionListener listener) {
		mListeners.add(listener);
	}

	public boolean removeListener(ManagedXmppConnectionListener listener) {
		return mListeners.remove(listener);
	}

	Set<ManagedXmppConnectionListener> getListeners() {
		return mListeners;
	}

	XmppConnectionStatus getStatus() {
		return mStatus;
	}
}
