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
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import org.asmack.core.AbstractManagedXmppConnectionListener;
import org.asmack.core.AndroidSmackManager;
import org.asmack.core.ManagedXmppConnection;
import org.asmack.core.XmppConnectionState;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageWithBodiesFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.AbstractRosterListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.SubscribeListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XmppManager {

	private static final Logger LOGGER = Logger.getLogger(XmppManager.class.getName());

	private static XmppManager INSTANCE;

	public static synchronized XmppManager getInstance(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new XmppManager(context);
		}
		return INSTANCE;
	}

	private final Context mContext;
	private final Settings settings;
	private final AndroidSmackManager asmackManager;

	private XMPPTCPConnectionConfiguration.Builder usedBuilder;
	private XMPPTCPConnection xmppConnection;
	private volatile XmppConnectionState mXmppConnectionState = XmppConnectionState.Disconnected;
	private Roster roster;

	private final Object mainActivityLock = new Object();
	private MainActivity mainActivity;

	private final Drawable mOnlineDrawable;
	private final Drawable mOfflineDrawable;
	private final Drawable mConnectingDrawlable;

	private RosterListener mRosterListener;

	private XmppManager(Context context) {
		this.mContext = context.getApplicationContext();
		this.settings = Settings.getInstance(mContext);
		this.asmackManager = AndroidSmackManager.getInstance(mContext);

		mOnlineDrawable = ContextCompat.getDrawable(mContext, android.R.drawable.presence_online);
		mOfflineDrawable = ContextCompat.getDrawable(mContext, android.R.drawable.presence_offline);
		mConnectingDrawlable = ContextCompat.getDrawable(mContext, android.R.drawable.presence_away);
	}

	public void adoptXmppConfiguration() {
		XMPPTCPConnectionConfiguration.Builder builder = settings.getConnectionConfigBuilder();
		if (builder == null) throw new IllegalStateException();

		if (xmppConnection != null) {
			xmppConnection.disconnect();
			xmppConnection = null;

			roster.removeRosterListener(mRosterListener);
			roster = null;
		}

		builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

		try {
			builder.setResource(Resourcepart.from("Smack"));
		} catch (XmppStringprepException e) {
			throw new IllegalStateException(e);
		}

		builder.setDebuggerEnabled(true);

		XMPPTCPConnectionConfiguration conf = builder.build();

		xmppConnection = asmackManager.createManagedConnection(conf);
		ManagedXmppConnection<XMPPTCPConnection> managedXmppConnection = asmackManager.getManagedXmppConnectionFor(xmppConnection);
		managedXmppConnection.addListener(new AbstractManagedXmppConnectionListener() {
			@Override
			public void connected(XMPPConnection connection) {
				setConnectionState(XmppConnectionState.Connected);
			}

			@Override
			public void authenticated(XMPPConnection connection, boolean resumed) {
				maybeSetOtherJidPresenceGui();
				RosterEntry otherJidEntry = roster.getEntry(settings.getOtherJid());

				if (otherJidEntry == null || (!otherJidEntry.canSeeHisPresence() && !otherJidEntry.isSubscriptionPending())) {
					try {
						roster.sendSubscriptionRequest(settings.getOtherJid());
					} catch (SmackException.NotLoggedInException | SmackException.NotConnectedException | InterruptedException e) {
						LOGGER.log(Level.SEVERE, "Could not send subscription request to other JID", e);
					}
				}

				connection.addAsyncStanzaListener(mMessageListener, MessageWithBodiesFilter.INSTANCE);

				setConnectionState(XmppConnectionState.Authenticated);
			}

			@Override
			public void connectionClosed() {
				connectionTerminated();
			}

			@Override
			public void connectionClosedOnError(Exception e) {
				connectionTerminated();
			}

			@Override
			public void connectionAttemptFailed(Exception e, ManagedXmppConnection connection) {
				withMainActivity((ma) -> Toast.makeText(ma, "Connection failed: " + e, Toast.LENGTH_LONG).show());
			}

			@Override
			public void loginAttemptFailed(Exception e, ManagedXmppConnection connection) {
				withMainActivity((ma) -> Toast.makeText(ma, "Login failed: " + e, Toast.LENGTH_LONG).show());
			}

			@Override
			public void stateChanged(XmppConnectionState oldState, XmppConnectionState newState, ManagedXmppConnection connection) {
				withMainActivity((ma) -> Toast.makeText(ma,
						"Connection state change from " + oldState + " to " + newState, Toast.LENGTH_LONG).show());
			}
		});

		roster = Roster.getInstanceFor(xmppConnection);
		roster.setSubscribeListener((from, presence) -> {
			if (from.equals(settings.getOtherJid())) {
				return SubscribeListener.SubscribeAnswer.Approve;
			} else {
				return SubscribeListener.SubscribeAnswer.Deny;
			}
		});
		mRosterListener = new AbstractRosterListener() {
			@Override
			public void presenceChanged(Presence presence) {
				if (!presence.getFrom().asBareJid().equals(settings.getOtherJid())) return;
				maybeSetOtherJidPresenceGui();
			}
		};
		roster.addRosterListener(mRosterListener);
	}

	public void enable() {
		assert (xmppConnection != null);
		asmackManager.enable();
	}

	public void disable() {
		asmackManager.disable();
	}

	boolean isConnectionUseable() {
		final XMPPTCPConnection connection = xmppConnection;
		return connection != null && connection.isAuthenticated();
	}

	void mainActivityOnCreate(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
		if (settings.isBasicConfigurationDone() && xmppConnection == null) {
			adoptXmppConfiguration();
		}
		if (xmppConnection == null) return;

		maybeSetOtherJidPresenceGui();
		maybeUpdateConnectionStateGui();
	}

	void mainActivityOnDestroy(MainActivity mainActivity) {
		assert (this.mainActivity == mainActivity);

		synchronized (mainActivityLock) {
			this.mainActivity = null;
		}
	}

	private final StanzaListener mMessageListener = (stanza) -> {
			Message message = (Message) stanza;
			withMainActivity((ma) -> Toast.makeText(ma, "XIOT: " + message.getBody(), Toast.LENGTH_LONG).show());
	};

	private void connectionTerminated() {
		xmppConnection.removeAsyncStanzaListener(mMessageListener);
		setConnectionState(XmppConnectionState.Disconnected);
	}

	private void maybeSetOtherJidPresenceGui() {
		Presence presence = roster.getPresence(settings.getOtherJid());
		final Drawable drawable;
		if (presence != null && presence.isAvailable()) {
			drawable = mOnlineDrawable;
		} else {
			drawable = mOfflineDrawable;
		}
		withMainActivity((ma) ->
					ma.otherJidPresenceImageView.setImageDrawable(drawable)
		);
	}

	void withMainActivity(final WithActivity<MainActivity> withMainActivity) {
		synchronized (mainActivityLock) {
			if (mainActivity == null) return;
			mainActivity.runOnUiThread(() -> withMainActivity.withActivity(mainActivity));
		}
	}

	EntityFullJid getFullOtherJid() {
		Presence presence = roster.getPresence(settings.getOtherJid());
		if (presence == null) return null;
		EntityFullJid fullOtherJid = presence.getFrom().asEntityFullJidIfPossible();
		if (fullOtherJid == null) throw new IllegalStateException();
		return fullOtherJid;
	}

	XMPPTCPConnection getXmppConnection() {
		return xmppConnection;
	}

	private final Set<XmppConnectionListener> mXmppConnectionStatusListeners = new CopyOnWriteArraySet<>();

	boolean addXmppConnectionStatusListener(XmppConnectionListener listener) {
		return mXmppConnectionStatusListeners.add(listener);
	}

	boolean removeXmppConnectionStatusListener(XmppConnectionListener listener) {
		return mXmppConnectionStatusListeners.remove(listener);
	}

	interface XmppConnectionListener {
		void newConnection(ManagedXmppConnection<XMPPTCPConnection> connection);
	}

	private void setConnectionState(XmppConnectionState state) {
		mXmppConnectionState = state;
		maybeUpdateConnectionStateGui();
	}

	private void maybeUpdateConnectionStateGui() {
		final Drawable drawable;
		switch (mXmppConnectionState) {
			case Connecting:
			case Connected:
				drawable = mConnectingDrawlable;
				break;
			case Authenticated:
				drawable = mOnlineDrawable;
				break;
			default:
				drawable = mOfflineDrawable;
				break;
		}
		withMainActivity((ma) -> {
			ma.myJidPresenceImageView.setImageDrawable(drawable);
		});
	}

	XmppConnectionState getConnectionState() {
		return mXmppConnectionState;
	}
}
