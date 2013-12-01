package edu.neu.mhealth.debug.helper;

import java.util.Observable;

public class MotionEventListener extends Observable {
	
	private MotionMetrics motion;

	public MotionEventListener() {
		motion = new MotionMetrics(0, 0);
	}
	
	public void notifyMotion(int x, int y) {
		motion.setMotionX(x);
		motion.setMotionY(y);
		setChanged();
		notifyObservers(motion);
	}
}
