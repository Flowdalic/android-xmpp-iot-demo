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
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import org.asmack.core.ASmackVersion;
import org.jivesoftware.smack.SmackConfiguration;

public class AboutActivity extends AppCompatActivity {

	private TextView mXiotVersionTextView;
	private TextView mAsmackVersionTextView;
	private TextView mAsmackUrlTextView;
	private TextView mSmackUrlTextView;
	private TextView mSmackVersionTextView;
	private TextView mSmackCopyrightTextView;
	private TextView mMtmUrlTextview;
	private TextView mMtmCopyrightTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		mXiotVersionTextView = (TextView) findViewById(R.id.about_xiot_version_text_view);
		mAsmackVersionTextView = (TextView) findViewById(R.id.about_asmack_version_text_view);
		mAsmackUrlTextView = (TextView) findViewById(R.id.about_asmack_url_text_view);
		mSmackUrlTextView = (TextView) findViewById(R.id.about_smack_url_text_view);
		mSmackVersionTextView = (TextView) findViewById(R.id.about_smack_version_text_view);
		mSmackCopyrightTextView = (TextView) findViewById(R.id.about_smack_copyright_text_view);
		mMtmUrlTextview = (TextView) findViewById(R.id.about_mtm_url_text_view);
		mMtmCopyrightTextView = (TextView) findViewById(R.id.about_mtm_copyright_text_view);

		mXiotVersionTextView.setText(XiotVersion.getVersion(this));

		mAsmackVersionTextView.setText(ASmackVersion.getVersion(this));

		makeLinkTextView(mAsmackUrlTextView, "http://asmack.org", "asmack.org");

		makeLinkTextView(mSmackUrlTextView, "https://igniterealtime.org/projects/smack", "igniterealtime.org/projects/smack");

		mSmackVersionTextView.setText("Version " + SmackConfiguration.getVersion());

		final String smackCopyright =
"Copyright © 2011-2016 Florian Schmaus\n" +
"Copyright © 2013-2014 Georg Lukas\n" +
"Copyright © 2014 Lars Noschinski\n" +
"Copyright © 2014 Vyacheslav Blinov\n" +
"Copyright © 2014 Andriy Tsykholyas\n" +
"Copyright © 2009-2013 Robin Collier\n" +
"Copyright © 2009 Jonas Ådahl\n" +
"Copyright © 2003-2010 Jive Software\n" +
"Copyright © 2001-2004 Apache Software Foundation";
		mSmackCopyrightTextView.setText(smackCopyright);

		makeLinkTextView(mMtmUrlTextview, "https://github.com/ge0rg/MemorizingTrustManager", "github.com/ge0rg/MemorizingTrustManager");

		final String mtmCopyright =
"Copyright 2010-2016 Georg Lukas";
		mMtmCopyrightTextView.setText(mtmCopyright);
	}

	private static void makeLinkTextView(TextView textView, String url, String text) {
		final Spanned content = Html.fromHtml("<a href='" + url + "'>" + text + "</a>");
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		textView.setText(content);
	}
}
