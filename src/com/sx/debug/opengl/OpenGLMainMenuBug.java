package com.sx.debug.opengl;

import java.util.ListIterator;

import com.sx.debug.helper.Global;


public class OpenGLMainMenuBug extends OpenGLBug{

	public OpenGLMainMenuBug(int x, int y, int speedX, int speedY, float scaleRatio) {
		super(x, y, speedX, speedY, scaleRatio);
	}
	
	public OpenGLMainMenuBug(int x, int y, int speedX, int speedY, float scaleRatio, boolean bouncing, int bounceDestX, int bounceDestY, int bounceStepCounter) {
		super(x, y, speedX, speedY, scaleRatio, bouncing, bounceDestX, bounceDestY, bounceStepCounter);
	}
	
	
	@Override
	public void refresh(ListIterator<OpenGLBug> listIterator) { 
		int tmpX;
		int tmpY;
		int polarityX;
		int polarityY;
		polarityX = speedX >= 0 ? 1 : -1;
		polarityY = speedY >= 0 ? 1 : -1;
		tmpX = x + speedX;
		tmpY = y + speedY;

		if (shouldPause) {
			if (System.currentTimeMillis() - lastRefreshTime > 2000) {
				shouldPause = false;
			}
		}
		if (Global.rd.nextInt(100) > 98) {
			shouldPause = true;
		}

		if (tmpX + polarityX * OpenGLBug.radius > OpenGLBugManager.getOpenGLBugManager().getOpenGlWidth() || tmpX + polarityX * OpenGLBug.radius < 0) {
			speedX = -speedX;
			tmpX = x;
		}
		if (tmpY + polarityY * OpenGLBug.radius > OpenGLBugManager.getOpenGLBugManager().getOpenGlHeight() || tmpY + polarityY * OpenGLBug.radius < 0) {
			speedY = -speedY;
			tmpY = y;
		}

		x = tmpX;
		y = tmpY;
		lastRefreshTime = System.currentTimeMillis();
	}

}
