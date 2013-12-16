package com.sx.debug.sensor;

import java.util.Observable;

public class MotionEventListener extends Observable {
	
	private MotionMetrics motion;

	public MotionEventListener() {
		motion = new MotionMetrics(0, 0);
	}
	
	public void notifyMotion(float x, float y) {
		motion.setMotionX(x);
		motion.setMotionY(y);
		setChanged();
		notifyObservers(motion);
	}
}
