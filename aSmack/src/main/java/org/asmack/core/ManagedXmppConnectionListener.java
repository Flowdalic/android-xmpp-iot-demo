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

public interface ManagedXmppConnectionListener extends ConnectionListener {

	void connectionAttemptFailed(Exception e, ManagedXmppConnection connection);

	void loginAttemptFailed(Exception e, ManagedXmppConnection connection);

	void stateChanged(XmppConnectionState oldState, XmppConnectionState newState, ManagedXmppConnection connection);

	void terminated();
}
