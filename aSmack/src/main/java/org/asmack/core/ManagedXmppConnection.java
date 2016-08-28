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

import org.jivesoftware.smack.XMPPConnection;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ManagedXmppConnection<C extends XMPPConnection> {

	private final WeakReference<C> mConnection;
	private final XmppConnectionStatus mStatus = new XmppConnectionStatus();
	private final Set<ManagedXmppConnectionListener> listeners = new CopyOnWriteArraySet<>();

	ManagedXmppConnection(C connection) {
		mConnection = new WeakReference<>(connection);
	}

	public C getConnection() {
		return mConnection.get();
	}

	public void addListener(ManagedXmppConnectionListener listener) {
		listeners.add(listener);
	}

	public boolean removeListener(ManagedXmppConnectionListener listener) {
		return listeners.remove(listener);
	}

	Set<ManagedXmppConnectionListener> getListeners() {
		return listeners;
	}

	XmppConnectionStatus getStatus() {
		return mStatus;
	}
}
