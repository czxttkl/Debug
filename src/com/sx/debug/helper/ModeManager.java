package com.sx.debug.helper;

import java.util.Observable;
import java.util.Observer;
import android.util.Log;

public class ModeManager {

	private static final String TAG = "ModeManager";
	/** In this mode, neither opencv or opengl works */
	public static final int MODE_INITIAL = 0;
	public static final int MODE_MAIN_MENU = 122;
	public static final int MODE_COLOR_PICK_CROSSHAIR = 123;
	public static final int MODE_SHOE_COLOR_PICKED = 124;
	public static final int MODE_COLOR_PICK_HOLD_WRONGLY = 125;
	public static final int MODE_FLOOR_COLOR_PICKED = 126;
	public static final int MODE_TUTORIAL_1 = 127;
	public static final int MODE_TUTORIAL_2 = 128;
	public static final int MODE_TUTORIAL_3 = 129;
	public static final int MODE_BEFORE_TUTORIAL_1 = 130;
	public static final int MODE_REAL_GAME = 131;
	
	private static ModeManager modeManager;
	private ModeEventListener eventListener;
	private int currentMode;
	private int previousMode;
	
	private ModeManager() {
		currentMode = -1;
		previousMode = -1;
		eventListener = new ModeEventListener();
	}
	
	public static ModeManager getModeManager() {
		
		if (modeManager == null) {
			modeManager = new ModeManager();
		}
		
		return modeManager;
	}

	public int getCurrentMode() {
		return currentMode;
	}

	public void setCurrentMode(int currentMode) {
		setPreviousMode(this.currentMode);
		this.currentMode = currentMode;
		Log.e(TAG, "mode manager set mode");
		this.eventListener.notifyModeUpdate(currentMode);
	}

	public int getPreviousMode() {
		return previousMode;
	}

	public void setPreviousMode(int previousMode) {
		this.previousMode = previousMode;
	}
	
	public void addObserver(Observer ob) {
		eventListener.addObserver(ob);
	}
	
	public class ModeEventListener extends Observable {
		
		public void notifyModeUpdate(int currentMode) {
			setChanged();
			notifyObservers(currentMode);
		}
	}

	public static class AccEventModeManager {
		public static final int MODE_DEFAULT = 0;
		public static final int MODE_SPRAY_SHAKE = 1;
		public static final int MODE_SPRAY_JUMP = 2;
		public static final int MODE_COLOR_PICK_CROSSHAIR = 3;
		public static final int MODE_COLOR_PICK_HOLD_WRONGLY = 4;
		private int mode = 0;
		public static AccEventModeManager mAccEventModeManager;

		private AccEventModeManager() {

		}

		public static AccEventModeManager getAccEventModeManager() {
			if (mAccEventModeManager == null) {
				mAccEventModeManager = new AccEventModeManager();
			}
			return mAccEventModeManager;
		}

		public int getCurrentMode() {
			return mode;
		}

		public void setCurrentMode(int newMode) {
			mode = newMode;
		}
	}
}
