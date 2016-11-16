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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.EditText;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

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
	private static final String THING_JID_KEY = "THING_JID";
	private static final String OWNER_JID_KEY = "OWNER_JID";
	private static final String CLAIMED_JID_KEY = "CLAIMED_JID";

	private static final String IDENTIY_MODE_KEY = "pref_identityMode";

	private static final String THING_SN_KEY = "pref_thingSn";
	private static final String REQUIRES_FIRST_TIME_SETUP_KEY = "pref_requireFirstTimeSetup";

	private final String IOT_CLAIM_ENABLED_KEY;

	private final SharedPreferences preferences;
	private final MemorizingTrustManager mMemorizingTrustManager;

	private EntityBareJid myJidCache;
	private EntityBareJid thingJidCache;
	private EntityBareJid ownerJidCache;
	private EntityBareJid claimedJidCache;
	private XMPPTCPConnectionConfiguration.Builder confBuilderCache;

	private Settings(Context context) {
		this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
		mMemorizingTrustManager = new MemorizingTrustManager(context.getApplicationContext());

		IOT_CLAIM_ENABLED_KEY = context.getResources().getString(R.string.iot_claim_enabled_pref_key);

		preferences.registerOnSharedPreferenceChangeListener((p, k) -> {
			switch (k) {
				case IDENTIY_MODE_KEY:
					String newIdentityMode = p.getString(IDENTIY_MODE_KEY, "");
					switch (newIdentityMode) {
						case "thing":
					}
					break;
			}
		});
	}

	public void saveBasics(@NonNull EntityBareJid myJid, @NonNull CharSequence password, EntityBareJid thingJid) {
		SharedPreferences.Editor editor = preferences.edit()
				.putString(MY_JID_KEY, myJid.toString())
				.putString(PASSWORD_KEY, password.toString());
		if (thingJid != null) {
			editor.putString(THING_JID_KEY, thingJid.toString());
		}
		editor.apply();

		myJidCache = myJid;
		thingJidCache = thingJid;
		confBuilderCache = null;
	}

	public void saveOwner(EntityBareJid owner) {
		if (owner != null) {
			preferences.edit().putString(OWNER_JID_KEY, owner.toString()).apply();
		} else {
			preferences.edit().remove(OWNER_JID_KEY).apply();
		}
		ownerJidCache = owner;
	}

	public EntityBareJid getOwner() {
		if (ownerJidCache == null) {
			String ownerJidString = preferences.getString(OWNER_JID_KEY, null);
			if (ownerJidString == null) return null;
			try {
				ownerJidCache = JidCreate.entityBareFrom(ownerJidString);
			} catch (XmppStringprepException e) {
				throw new IllegalStateException(e);
			}
		}
		return ownerJidCache;
	}

	public boolean isBasicConfigurationDone() {
		if (isIdentityModeApp() && getThingJid() == null) return false;

		return getMyJid() != null && StringUtils.isNotEmpty(getPassword());
	}

	public XMPPTCPConnectionConfiguration.Builder getConnectionConfigBuilder() {
		if (!isBasicConfigurationDone()) return null;

		if (confBuilderCache == null) {
			XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();

			String password = getPassword();
			builder.setUsernameAndPassword(getMyJid().getLocalpart(), password);
			builder.setXmppDomain(getMyJid().asDomainBareJid());

			builder.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

			SSLContext sc;
			try {
				sc = SSLContext.getInstance("TLS");
				sc.init(null, new X509TrustManager[] { mMemorizingTrustManager }, new java.security.SecureRandom());
			} catch (KeyManagementException | NoSuchAlgorithmException e) {
				throw new IllegalStateException(e);
			}
			builder.setCustomSSLContext(sc);

			confBuilderCache = builder;
		}
		return confBuilderCache;
	}

	public EntityBareJid getMyJid() {
		if (myJidCache == null) {
			String myJidString = preferences.getString(MY_JID_KEY, null);
			if (myJidString == null) return null;
			try {
				myJidCache = JidCreate.entityBareFrom(myJidString);
			} catch (XmppStringprepException e) {
				throw new IllegalStateException(e);
			}
		}
		return myJidCache;
	}

	public String getPassword() {
		return preferences.getString(PASSWORD_KEY, null);
	}

	public EntityBareJid getThingJid() {
		if (thingJidCache == null) {
			String otherJidString = preferences.getString(THING_JID_KEY, null);
			if (otherJidString == null) return null;
			try {
				thingJidCache = JidCreate.entityBareFrom(otherJidString);
			} catch (XmppStringprepException e) {
				throw new IllegalStateException(e);
			}
		}
		return thingJidCache;
	}

	void setClaimedJid(EntityBareJid claimedJid) {
		preferences.edit().putString(CLAIMED_JID_KEY, claimedJid.toString()).apply();
	}

	public EntityBareJid getClaimedJid() {
		if (claimedJidCache == null) {
			String claimedJidString = preferences.getString(CLAIMED_JID_KEY, null);
			if (claimedJidString == null) return null;
			try {
				claimedJidCache = JidCreate.entityBareFrom(claimedJidString);
			} catch (XmppStringprepException e) {
				throw new IllegalStateException(e);
			}
		}
		return claimedJidCache;
	}

	public void populateEditTexts(EditText myJidText, EditText passwordText, EditText otherJidText) {
		String myJid = preferences.getString(MY_JID_KEY, null);
		if (myJid != null) myJidText.setText(myJid);

		String password = preferences.getString(PASSWORD_KEY, null);
		if (password != null) passwordText.setText(password);

		String otherJid = preferences.getString(THING_JID_KEY, null);
		if (otherJid != null) otherJidText.setText(otherJid);
	}

	enum IdentityMode {
		app,
		thing,
	}

	void firstTimeSetup(IdentityMode identityMode) {
		preferences.edit()
				.putString(IDENTIY_MODE_KEY, identityMode.toString())
				.putBoolean(REQUIRES_FIRST_TIME_SETUP_KEY, false)
				.apply();
	}

	public IdentityMode getIdentityMode() {
		String identityMode = preferences.getString(IDENTIY_MODE_KEY, "both");
		switch (identityMode) {
			case "app":
				return IdentityMode.app;
			case "thing":
				return IdentityMode.thing;
			default:
				throw new IllegalStateException();
		}
	}

	public boolean isIdentityModeApp() {
		switch (getIdentityMode()) {
			case app:
				return true;
			default:
				return false;
		}
	}

	public boolean isIdentityModeThing() {
		switch (getIdentityMode()) {
			case thing:
				return true;
			default:
				return false;
		}
	}

	public String getThingSerialNumber() {
		String sn = preferences.getString(THING_SN_KEY, null);
		if (sn == null) {
			sn = StringUtils.randomString(8);
			preferences.edit().putString(THING_SN_KEY, sn);
		}
		return sn;
	}

	public boolean showClaimGuiElements() {
		return preferences.getBoolean(IOT_CLAIM_ENABLED_KEY, false);
	}

	public boolean firstTimeSetupRequired() {
		return preferences.getBoolean(REQUIRES_FIRST_TIME_SETUP_KEY, true);
	}
}
