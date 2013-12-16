package com.sx.debug.helper;

import android.content.Context;
import android.preference.PreferenceManager;

public class Prefs {
	public static final String KEY_SPRAY_TAPPED = "spraytapped";
	public static final String KEY_TUTORIALED = "tutorialed";
	public static final String KEY_FIRST_TIME_PLAY = "firstplay";
	public static final String KEY_OBJ_FILE_SAVED = "objfilesave";
	
	public static final boolean KEY_SPRAY_TAPPED_DEF = false;
	public static final boolean KEY_TUTORIALED_DEF = false;
	public static final boolean KEY_FIRST_TIME_PLAY_DEF = true;
	public static final boolean KEY_OBJ_FILE_SAVED_DEF = false;

	public static boolean getSprayTapped(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_SPRAY_TAPPED, KEY_SPRAY_TAPPED_DEF);
	}

	public static void setSprayTapped(Context context, boolean result) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_SPRAY_TAPPED, result).commit();
	}

	public static boolean getTutorialed(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_TUTORIALED, KEY_TUTORIALED_DEF);
	}

	public static void setTutorialed(Context context, boolean result) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_TUTORIALED, result).commit();
	}
	
	public static boolean getFirstTimePlay(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_FIRST_TIME_PLAY, KEY_FIRST_TIME_PLAY_DEF);
	}

	public static void setFirstTimePlay(Context context, boolean result) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_FIRST_TIME_PLAY, result).commit();
	}
	
	public static boolean getObjFileSaved(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_OBJ_FILE_SAVED, KEY_OBJ_FILE_SAVED_DEF);
	}
	
	public static void setObjFileSaved(Context context, boolean result) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_OBJ_FILE_SAVED, result).commit();
	}
	

	
}
