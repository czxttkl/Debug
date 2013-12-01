package edu.neu.mhealth.debug.helper;

public class MotionMetrics {
	private int motionX;
	private int motionY;
	
	public MotionMetrics(int x, int y) {
		motionX = x;
		motionY = y;
	}

	public int getMotionX() {
		return motionX;
	}

	public void setMotionX(int motionX) {
		this.motionX = motionX;
	}

	public int getMotionY() {
		return motionY;
	}

	public void setMotionY(int motionY) {
		this.motionY = motionY;
	}
}
