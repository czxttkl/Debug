package com.sx.debug.sensor;

import android.util.Log;

public class MovingAverage {

	private static final String TAG = "MovingAverage";
	private float circularBuffer[];
	private float avg;
	private int circularIndex;
	private int count;

	public MovingAverage(int k) {
		circularBuffer = new float[k];
		count = 0;
		circularIndex = 0;
		avg = 0;
	}

	public float getValue() {
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < circularBuffer.length; i++) {
			stringBuilder.append(circularBuffer[i] + " ");
		}
		System.out.println("Value before average:  " + stringBuilder);
//		Log.e(TAG, "Value after average: " + avg);
		return avg;
	}

	public void pushValue(float x) {
		if (count++ == 0) {
			primeBuffer(x);
		}
		circularBuffer[circularIndex] = x;
		float sum = 0.0f;
		for (int i = 0; i < circularBuffer.length; i++) {
			sum += circularBuffer[i];
		}
		avg = sum / circularBuffer.length;
		circularIndex = nextIndex(circularIndex);
	}

	public int getCount() {
		return count;
	}

	private void primeBuffer(float val) {
		for (int i = 0; i < circularBuffer.length; i++) {
			circularBuffer[i] = val;
		}
		avg = val;
	}

	private int nextIndex(int curIndex) {
		if (curIndex + 1 >= circularBuffer.length) {
			return 0;
		}
		return curIndex + 1;
	}
}
