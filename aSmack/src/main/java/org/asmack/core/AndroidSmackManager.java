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

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringTransformer;

import java.text.Normalizer;
import java.util.Map;
import java.util.WeakHashMap;

public class AndroidSmackManager {

	static {
		// Some network types, especially GPRS or EDGE is rural areas have a very slow response
		// time. Smack's default packet reply timeout of 5 seconds is way to low for such networks,
		// so we increase it to 2 minutes.
		// This value must also be greater then the highest returned bundle and defer value.
		SmackConfiguration.setDefaultPacketReplyTimeout(2 * 60 * 1000);

		SmackConfiguration.addDisabledSmackClass("org.jivesoftares.smack.legacy");
		SmackConfiguration.addDisabledSmackClass("org.jivesoftware.smack.java7");

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

	private enum State {
		enabled,
		disabled,
		;
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

	private final Map<XMPPTCPConnection, XmppConnectionStatus> xmppTcpConnections = new WeakHashMap<>();

	private State state;

	private AndroidSmackManager(Context context) {
		this.context = context;
	}

	public XMPPTCPConnection createManagedConnection(XMPPTCPConnectionConfiguration configuration) {
		XMPPTCPConnection connection = new XMPPTCPConnection(configuration);
		xmppTcpConnections.put(connection, new XmppConnectionStatus());
		return connection;
	}

	public synchronized void enable() {
		if (state == State.enabled) return;

		state = State.enabled;
	}

	public synchronized void disable() {
		if (state == State.disabled) return;

		state = State.disabled;
	}
}
