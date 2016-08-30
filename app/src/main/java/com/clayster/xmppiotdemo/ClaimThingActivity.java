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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.iot.Thing;
import org.jivesoftware.smackx.iot.discovery.IoTDiscoveryManager;
import org.jivesoftware.smackx.iot.discovery.element.IoTClaimed;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ClaimThingActivity extends AppCompatActivity {

	private static final Logger LOGGER = Logger.getLogger(ClaimThingActivity.class.getName());

	private TextView mRegistryTextView;
	private TextView mSnTextView;
	private TextView mManTextView;
	private TextView mModelTextView;
	private TextView mVTextView;
	private TextView mKeyTextView;

	private Button mClaimButton;

	private XmppManager mXmppManger;

	private Jid mRegistry;

	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);

		setContentView(R.layout.activity_claim_thing);

		mRegistryTextView = (TextView) findViewById(R.id.registry_text_view);
		mSnTextView = (TextView) findViewById(R.id.sn_text_view);
		mManTextView = (TextView) findViewById(R.id.man_text_view);
		mModelTextView = (TextView) findViewById(R.id.model_text_view);
		mVTextView = (TextView) findViewById(R.id.v_text_view);
		mKeyTextView = (TextView) findViewById(R.id.key_text_view);

		mClaimButton = (Button) findViewById(R.id.claim_button);

		mXmppManger = XmppManager.getInstance(this);

		Async.go(() -> findRegistry());
	}

	private void showInGui(CharSequence toast) {
		runOnUiThread(() -> {
			Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
		});
	}

	private void findRegistry() {
		final XMPPTCPConnection connection = mXmppManger.getXmppConnection();
		if (connection == null) {
			String abortReason = "No connection configured";
			showInGui(abortReason);
			LOGGER.warning(abortReason);
			finish();
			return;
		}

		if (!connection.isAuthenticated()) {
			String abortReason = "Connection not authenticated";
			showInGui(abortReason);
			LOGGER.warning(abortReason);
			finish();
			return;
		}

		Exception exception = null;
		IoTDiscoveryManager ioTDiscoveryManager = IoTDiscoveryManager.getInstanceFor(connection);
		try {
			mRegistry = ioTDiscoveryManager.findRegistry();
		} catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
			exception = e;
		}
		if (mRegistry == null || exception != null) {
			if (exception == null) {
				exception = new SmackException("No registry provided by local XMPP service.");
			}
			showInGui("Could not find registry: " + exception);
			LOGGER.log(Level.WARNING, "Could not find a registry", exception);
			finish();
			return;
		}

		runOnUiThread(() -> {
			mRegistryTextView.setText("Registry: " + mRegistry);
			mClaimButton.setEnabled(true);
		});
	}

	public void claimButtonClicked(View view) {
		final String sn = mSnTextView.getText().toString();
		if (StringUtils.isNullOrEmpty(sn)) {
			return;
		}

		final String man = mManTextView.getText().toString();
		if (StringUtils.isNullOrEmpty(man)) {
			return;
		}

		final String model = mModelTextView.getText().toString();
		if (StringUtils.isNullOrEmpty(model)) {
			return;
		}

		final String v = mVTextView.getText().toString();
		if (StringUtils.isNullOrEmpty(v)) {
			return;
		}

		final String key = mKeyTextView.getText().toString();
		if (StringUtils.isNullOrEmpty(key)) {
			return;
		}

		final Thing thing = Thing.builder()
				.setSerialNumber(sn)
				.setManufacturer(man)
				.setModel(model)
				.setVersion(v)
				.setKey(key)
				.build();

		Async.go(() -> claimButtonClicked(thing));
	}

	private void claimButtonClicked(final Thing thing) {

		final XMPPTCPConnection connection = mXmppManger.getXmppConnection();
		if (connection == null) {
			return;
		}

		if (connection.isAuthenticated()) {
			return;
		}

		IoTDiscoveryManager ioTDiscoveryManager = IoTDiscoveryManager.getInstanceFor(connection);

		IoTClaimed iotClaimed;
		try {
			iotClaimed = ioTDiscoveryManager.claimThing(mRegistry, thing.getMetaTags(), true);
		} catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
			return;
		}

		EntityBareJid claimedJid = iotClaimed.getJid().asEntityBareJidIfPossible();
		if (claimedJid == null) {
			throw new IllegalStateException();
		}
		Settings settings = Settings.getInstance(this);
		settings.setClaimedJid(claimedJid);

		showInGui("Thing " + claimedJid + " claimed.");
		runOnUiThread(() -> {
			finish();
		});
	}
}
