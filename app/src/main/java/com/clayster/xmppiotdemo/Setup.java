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
import android.widget.EditText;
import android.widget.Toast;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.util.JidUtil;
import org.jxmpp.stringprep.XmppStringprepException;

public class Setup extends AppCompatActivity {

	private EditText myJidText;
	private EditText passwordText;
	private EditText otherJidText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setup);

		myJidText = (EditText) findViewById(R.id.myJidText);
		passwordText = (EditText) findViewById(R.id.passwordText);
		otherJidText = (EditText) findViewById(R.id.otherJidText);

		Settings.getInstance(this).populateEditTexts(myJidText, passwordText, otherJidText);

		JidTextWatcher.watch(myJidText);
		JidTextWatcher.watch(otherJidText);
	}

	public void saveButtonClicked(View view) {
		EntityBareJid myJid;
		try {
			myJid = JidUtil.validateEntityBareJid(myJidText.getText());
		} catch (JidUtil.NotAEntityBareJidStringException | XmppStringprepException e) {
			myJidText.setError("Invalid JID: " + e.getLocalizedMessage());
			return;
		}

		CharSequence password = passwordText.getText();
		if (password.length() == 0) {
			passwordText.setError("Password missing");
			return;
		}

		EntityBareJid otherJid;
		try {
			otherJid = JidUtil.validateEntityBareJid(otherJidText.getText());
		} catch (JidUtil.NotAEntityBareJidStringException | XmppStringprepException e) {
			myJidText.setError("Invalid JID: " + e.getLocalizedMessage());
			return;
		}

		Toast.makeText(this, "My JID: " + myJid + " Password: " + password + " Other Jid: " + otherJid, Toast.LENGTH_LONG).show();
		Settings.getInstance(this).saveBasics(myJid, password, otherJid);
		finish();
	}
}
