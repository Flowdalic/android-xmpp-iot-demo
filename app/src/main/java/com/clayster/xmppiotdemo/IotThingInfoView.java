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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class IotThingInfoView extends RelativeLayout {

	private final TextView mThingInfoName;
	private final TextView mThingInfoValue;

	public IotThingInfoView(Context context, AttributeSet attrs) {
		super(context, attrs);

		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.iot_thing_info, this);

		mThingInfoName = (TextView) findViewById(R.id.iot_thing_info_name_text_view);
		mThingInfoValue = (TextView) findViewById(R.id.iot_thing_info_value_text_view);

		String thingInfoName, thingInfoValue;
		{
			TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IotThingInfo, 0, 0);
			try {
				thingInfoName = typedArray.getString(R.styleable.IotThingInfo_name);
				thingInfoValue = typedArray.getString(R.styleable.IotThingInfo_value);
			} finally {
				typedArray.recycle();
			}
		}
		setThingInfo(thingInfoName, thingInfoValue);
	}

	public IotThingInfoView(Context context, CharSequence thingInfoName, CharSequence thingInfoValue) {
		this(context, null);
		setThingInfo(thingInfoName, thingInfoValue);
	}

	public void setThingInfo(CharSequence name, CharSequence value) {
		mThingInfoName.setText(name);
		mThingInfoValue.setText(value);
	}

	public CharSequence getThingInfoName() {
		return mThingInfoName.getText();
	}
}
