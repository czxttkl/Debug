/**
 * 
 * @author "Shang Ma"
 *
 */
package com.sx.debug.sensor;

import java.util.Observable;

import com.sx.debug.helper.ModeManager;
import com.sx.debug.helper.ModeManager.AccEventModeManager;


import android.R.integer;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.widget.Toast;

public class LinearAccEventListener extends Observable implements SensorEventListener {

	private static final String TAG = "AccEventListener";
	private static final float ALPHA = 0.8f;

	private static final int state_invalid = -1;
	private static final int state_initial = 0;
	private static final int state_first = 1;
	private static final int state_second = 2;
	private static final int state_third = 3;
	private static final int state_forth = 4;
	private static final int state_fifth = 5;

	private static final long timeDifference = 1000000000;

	private Context mContext;
	private int currState;
	private int prevState;
	private float[] gravity;
	private long lastUpdateTime;
	private long currUpdateTime;

	public LinearAccEventListener(Context context) {
		gravity = new float[3];
		currState = state_invalid;
		mContext = context;
		lastUpdateTime = -1;
		currUpdateTime = -1;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		int mode = AccEventModeManager.getAccEventModeManager().getCurrentMode();
		switch (mode) {
		case AccEventModeManager.MODE_SPRAY_JUMP:
			float[] values = event.values.clone();
//			values = highPass(values[0], values[1], values[2]);

			if (currState == state_invalid) {
				if (values[2] < 1 && values[2] > -1) {
					currState = state_initial;
					lastUpdateTime = event.timestamp;
				}
			}
			if (currState == state_initial) {
				if (values[2] < -2 && values[2] > -4) {
					currState = state_first;
					lastUpdateTime = event.timestamp;
				}
			}

			if (currState == state_first) {
				if ((event.timestamp - lastUpdateTime) > timeDifference) {
					currState = state_invalid;
				} else if (values[2] < 5.5 && values[2] > 3.5) {
					// Log.e(TAG, "time for first: " + (event.timestamp - lastUpdateTime));
					currState = state_second;
					lastUpdateTime = event.timestamp;
				}
			}

			if (currState == state_second) {
				if ((event.timestamp - lastUpdateTime) > timeDifference) {
					currState = state_invalid;
				} else if (values[2] < -6 && values[2] > -10) {
					// Log.e(TAG, "time for second: " + (event.timestamp - lastUpdateTime));
					currState = state_third;
					lastUpdateTime = event.timestamp;
				}
			}

			if (currState == state_third) {
				if ((event.timestamp - lastUpdateTime) > timeDifference) {
					currState = state_invalid;
				} else if (values[2] < 6 && values[2] > 3) {
					// Log.e(TAG, "time for third: " + (event.timestamp - lastUpdateTime));
					currState = state_forth;
					lastUpdateTime = event.timestamp;
				}
			}

			if (currState == state_forth) {
				if ((event.timestamp - lastUpdateTime) > timeDifference) {
					currState = state_invalid;
				} else if (values[2] < 2 && values[2] > -2) {
					 Log.e(TAG, "time for forth: " + (event.timestamp - lastUpdateTime));
					currState = state_fifth;
					lastUpdateTime = event.timestamp;
				}
			}

			if (currState == state_fifth) {
				Log.e(TAG, "linear should be notified!");
				setChanged();
				notifyObservers();
				currState = state_invalid;
			}
			
			Log.d(TAG, "linear value[2]:" + values[2] + " currstate:" + currState + " mode:"+ mode );
			
			break;
			
		
		default:
			// do nothing
		}
		// Log.i(TAG, "current state: " + currState);
	}

	/**
	 * This method derived from the Android documentation and is available under the Apache 2.0 license.
	 * 
	 * @see http://developer.android.com/reference/android/hardware/SensorEvent.html
	 */
	private float[] highPass(float x, float y, float z) {
		float[] filteredValues = new float[3];

		gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x;
		gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y;
		gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z;

		filteredValues[0] = x - gravity[0];
		filteredValues[1] = y - gravity[1];
		filteredValues[2] = z - gravity[2];

		return filteredValues;
	}

}
