/**
 * 
 * @author "Shang Ma"
 *
 */
package edu.neu.mhealth.debug.helper;

import java.util.Observable;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.widget.Toast;

public class AccEventListener extends Observable implements SensorEventListener {
	
	private static final String TAG = "AccEventListener";
    private static final float ALPHA = 0.8f;
    
    private static final int state_invalid = -1;
    private static final int state_initial = 0;
    private static final int state_first = 1;
    private static final int state_second = 2;
    private static final int state_third = 3;
    private static final int state_forth = 4;    
    private static final int state_fifth = 5;
    
    private Context mContext;
    private int state;
    private float[] gravity;

    public AccEventListener(Context context) {
        gravity = new float[3];
        state = state_invalid;
        mContext = context;
    }
    
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		float[] values = event.values.clone();
		values = highPass(values[0], values[1], values[2]);
		
//		Log.e(TAG, "z axis value: " + values[2]);
		if (state == state_invalid) {
			if (values[2] < 1 && values[2] > -1) {
				state = state_initial;
			}
		}
		if (state == state_initial) {
			if (values[2] < -2 && values[2] > -4) {
				state = state_first;
			}
		}
		
		if (state == state_first) {
			if (values[2] < 6 && values[2] > 4) {
				state = state_second;
			}
		}

		if (state == state_second) {
			if (values[2] < -7 && values[2] > -10) {
				state = state_third;
			}
		}

		if (state == state_third) {
			if (values[2] < 6 && values[2] > 4) {
				state = state_forth;
			}
		}

		if (state == state_forth) {
			if (values[2] < 2 && values[2] > -2) {
				state = state_fifth;
			}
		}

		if (state == state_fifth) {
			Log.e(TAG, "should be notified!");
			setChanged();
			notifyObservers();
			state = state_invalid;
		}		
		
//		Log.i(TAG, "current state: " + state);
	}
	
    /**
     * This method derived from the Android documentation and is available under
     * the Apache 2.0 license.
     * 
     * @see http://developer.android.com/reference/android/hardware/SensorEvent.html
     */
    private float[] highPass(float x, float y, float z)
    {
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
