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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XiotVersion {

	private static final Logger LOGGER = Logger.getLogger(XiotVersion.class.getName());

	private static String version;

	public static String getVersion(Context context) {
		if (version != null) return version;

		try {
			InputStream is = context.getResources().openRawResource(R.raw.xiot_version);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			version = reader.readLine();
			try {
				reader.close();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "IOException closing stream", e);
			}
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Could not determine version", e);
			version = "unknown version";
		}

		return version;
	}
}
