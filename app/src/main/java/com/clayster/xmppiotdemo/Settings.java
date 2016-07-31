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
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.widget.EditText;

import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class Settings {

	private static Settings INSTANCE;

	public static synchronized Settings getInstance(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new Settings(context);
		}
		return INSTANCE;
	}

	private static final String MY_JID_KEY = "MY_JID";
	private static final String PASSWORD_KEY = "PASSWORD";
	private static final String OTHER_JID_KEY = "OTHER_JID";

	private final SharedPreferences preferences;

	private EntityBareJid myJidCache;
	private EntityBareJid otherJidCache;

	private Settings(Context context) {
		this.preferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
	}

	public void saveBasics(@NonNull EntityBareJid myJid, @NonNull CharSequence password, @NonNull EntityBareJid otherJid) {
		preferences.edit()
				.putString(MY_JID_KEY, myJid.toString())
				.putString(PASSWORD_KEY, password.toString())
				.putString(OTHER_JID_KEY, otherJid.toString()).apply();
		myJidCache = myJid;
		otherJidCache = otherJid;
	}

	public XMPPTCPConnectionConfiguration.Builder getConnectionConfigBuilder() {
		XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
		if (myJidCache == null) {
			String myJidString = preferences.getString(MY_JID_KEY, null);
			if (myJidString == null) return null;
			try {
				myJidCache = JidCreate.entityBareFrom(myJidString);
			} catch (XmppStringprepException e) {
				throw new IllegalStateException(e);
			}
		}
		String password = preferences.getString(PASSWORD_KEY, null);
		builder.setUsernameAndPassword(myJidCache.getLocalpart(), password);
		builder.setXmppDomain(myJidCache.asDomainBareJid());
		return builder;
	}

	public EntityBareJid getOtherJid() {
		if (otherJidCache == null) {
			String otherJidString = preferences.getString(OTHER_JID_KEY, null);
			if (otherJidString == null) return null;
			try {
				otherJidCache = JidCreate.entityBareFrom(otherJidString);
			} catch (XmppStringprepException e) {
				throw new IllegalStateException(e);
			}
		}
		return otherJidCache;
	}

	public void populateEditTexts(EditText myJidText, EditText passwordText, EditText otherJidText) {
		String myJid = preferences.getString(MY_JID_KEY, null);
		if (myJid != null) myJidText.setText(myJid);

		String password = preferences.getString(PASSWORD_KEY, null);
		if (password != null) passwordText.setText(password);

		String otherJid = preferences.getString(OTHER_JID_KEY, null);
		if (otherJid != null) otherJidText.setText(otherJid);
	}
}
