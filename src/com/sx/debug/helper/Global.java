package com.sx.debug.helper;

import java.util.Random;

public class Global {
	public static Random rd = new Random();
	public final static String APP_LOG_TAG = "mDebug"; 
	
	/** generate an integer between a range */
	public static int randInt(int min, int max) {
		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rd.nextInt((max - min) + 1) + min;

		return randomNum;
	}
}
