package com.sx.debug.opengl;

public class OpenGLFire {
	/**
	 * Because the units of width are different between OpenCV and OpenGL, we
	 * record ratio for the position according to the screen (OpenGL coordinate)
	 */
	public double ratioX;

	/**
	 * Because the units of height are different between OpenCV and OpenGL, we
	 * record ratio for the position according to the screen (OpenGL coordinate)
	 */
	public double ratioY;

	public OpenGLFire(double ratioX, double ratioY) {
		this.ratioX = ratioX;
		this.ratioY = ratioY;
	}
}
