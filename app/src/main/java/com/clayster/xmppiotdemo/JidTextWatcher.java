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

import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import org.jxmpp.jid.util.JidUtil;

public class JidTextWatcher implements TextWatcher {

	private final EditText jidEditText;

	private enum State {
		validJid,
		invalidJid,
		empty,
		;
	}

	public static void watch(EditText jid) {
		new JidTextWatcher(jid);
	}

	private State state;

	private JidTextWatcher(EditText jidEditText) {
		this.jidEditText = jidEditText;
		jidEditText.addTextChangedListener(this);
		state = determineState(jidEditText.getText());
	}

	@Override
	public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
	}

	@Override
	public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
	}

	@Override
	public void afterTextChanged(Editable editable) {
		State currentState = determineState(editable);
		if (state == currentState) {
			return;
		}
		state = currentState;
		switch (state) {
			case empty:
				jidEditText.setBackgroundColor(Color.WHITE);
				break;
			case validJid:
				jidEditText.setBackgroundColor(Color.GREEN);
				break;
			case invalidJid:
				jidEditText.setBackgroundColor(Color.RED);
				break;
		}
	}

	private static State determineState(CharSequence cs) {
		if (cs.length() == 0) {
			return State.empty;
		} else if (JidUtil.isValidEntityBareJid(cs)) {
			return State.validJid;
		} else {
			return State.invalidJid;
		}
	}
}
