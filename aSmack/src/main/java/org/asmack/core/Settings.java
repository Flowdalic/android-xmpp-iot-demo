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
import android.content.SharedPreferences;

public class Settings {

	private static final String LAST_ACTIVE_NETWORK_KEY = "LAST_ACTIVE_NETWORK";
	private static final String NETWORK_DEBUG_LOG_KEY = "NETWORK_DEBUG_LOG";

	private static Settings INSTANCE;

	public static synchronized Settings getInstance(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new Settings(context);
		}
		return INSTANCE;
	}
	private final Context mContext;
	private final SharedPreferences mSharedPreferences;

	private Settings(Context context) {
		mContext = context;
		mSharedPreferences = context.getSharedPreferences("AndroidSmack", Context.MODE_PRIVATE);
	}

	public String getLastActiveNetwork() {
		return mSharedPreferences.getString(LAST_ACTIVE_NETWORK_KEY, null);
	}

	void setLastActiveNetwork(String network) {
		mSharedPreferences.edit().putString(LAST_ACTIVE_NETWORK_KEY, network).apply();
	}

	public boolean isNetworkDebugLogEnabled() {
		return mSharedPreferences.getBoolean(NETWORK_DEBUG_LOG_KEY, false);
	}
}
