package com.sx.debug.sensor;

public class MotionMetrics {
	private float motionX;
	private float motionY;
	
	public MotionMetrics(float x, float y) {
		motionX = x;
		motionY = y;
	}

	public float getMotionX() {
		return motionX;
	}

	public void setMotionX(float motionX) {
		this.motionX = motionX;
	}

	public float getMotionY() {
		return motionY;
	}

	public void setMotionY(float motionY) {
		this.motionY = motionY;
	}
}
