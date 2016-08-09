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

public class IotSensorView extends RelativeLayout {

	private final TextView mSensorName;
	private final TextView mSensorValue;

	public IotSensorView(Context context, AttributeSet attrs) {
		super(context, attrs);

		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.iot_sensor, this);

		mSensorName = (TextView) findViewById(R.id.iot_sensor_name_text_view);
		mSensorValue = (TextView) findViewById(R.id.iot_sensor_value_text_view);

		String sensorName, sensorValue;
		{
			TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IotSensor, 0, 0);
			try {
				sensorName = typedArray.getString(R.styleable.IotSensor_name);
				sensorValue = typedArray.getString(R.styleable.IotSensor_value);
			} finally {
				typedArray.recycle();
			}
		}
		setSensor(sensorName, sensorValue);
	}

	public IotSensorView(Context context, CharSequence sensorName, CharSequence sensorValue) {
		this(context, null);
		setSensor(sensorName, sensorValue);
	}

	public void setSensor(CharSequence name, CharSequence value) {
		mSensorName.setText(name);
		mSensorValue.setText(value);
	}
}
