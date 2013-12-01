package edu.neu.mhealth.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Scanner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.learnopengles.android.common.RawResourceReader;
import com.learnopengles.android.common.ShaderHelper;
import com.learnopengles.android.common.TextureHelper;

import edu.neu.mhealth.debug.opengl.OpenGLBug;
import edu.neu.mhealth.debug.opengl.OpenGLFire;

/**
 * This class implements our custom renderer. Note that the GL10 parameter
 * passed in is unused for OpenGL ES 2.0 renderers -- the static class GLES20 is
 * used instead.
 */
public class OpenGlRenderer implements GLSurfaceView.Renderer {
	/** Used for debug logs. */
	private static final String TAG = "mDebug";

	private final Context mActivityContext;
	private final CameraActivity mCameraActivityInstance;

	/** The default scale ratio for 3d model */
	private final float SCALE_RATIO = 12.0f;

	/**
	 * Store the model matrix. This matrix is used to move models from object
	 * space (where each model can be thought of being located at the center of
	 * the universe) to world space.
	 */
	private float[] mModelMatrix = new float[16];

	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix
	 * transforms world space to eye space; it positions things relative to our
	 * eye.
	 */
	private float[] mViewMatrix = new float[16];

	/**
	 * Store the projection matrix. This is used to project the scene onto a 2D
	 * viewport.
	 */
	private float[] mProjectionMatrix = new float[16];

	/**
	 * Allocate storage for the final combined matrix. This will be passed into
	 * the shader program.
	 */
	private float[] mMVPMatrix = new float[16];

	/**
	 * Stores a copy of the model matrix specifically for the light position.
	 */
	private float[] mLightModelMatrix = new float[16];

	/** Store our bug model data in a float buffer. */
	private final FloatBuffer mBugPositions;
	private final FloatBuffer mBugColors;
	private final FloatBuffer mBugNormals;
	private final FloatBuffer mBugTextureCoordinates;

	/** Store our bug model data in a float buffer. */
	private final FloatBuffer mFirePositions;
	private final FloatBuffer mFireColors;
	private final FloatBuffer mFireNormals;
	private final FloatBuffer mFireTextureCoordinates;

	/** This will be used to pass in the transformation matrix. */
	private int mMVPMatrixHandle;

	/** This will be used to pass in the modelview matrix. */
	private int mMVMatrixHandle;

	/** This will be used to pass in the light position. */
	private int mLightPosHandle;

	/** This will be used to pass in the texture. */
	private int mTextureUniformHandle;

	/** This will be used to pass in bug model position information. */
	private int mBugPositionHandle;

	/** This will be used to pass in bug model color information. */
	private int mBugColorHandle;

	/** This will be used to pass in bug model normal information. */
	private int mBugNormalHandle;

	/** This will be used to pass in bug model texture coordinate information. */
	private int mBugTextureCoordinateHandle;

	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;

	/** Size of the position data in elements. */
	private final int mPositionDataSize = 3;

	/** Size of the color data in elements. */
	private final int mColorDataSize = 4;

	/** Size of the normal data in elements. */
	private final int mNormalDataSize = 3;

	/** Size of the texture coordinate data in elements. */
	private final int mTextureCoordinateDataSize = 2;

	/**
	 * Used to hold a light centered on the origin in model space. We need a 4th
	 * coordinate so we can get translations to work when we multiply this by
	 * our transformation matrices.
	 */
	private final float[] mLightPosInModelSpace = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };

	/**
	 * Used to hold the current position of the light in world space (after
	 * transformation via model matrix).
	 */
	private final float[] mLightPosInWorldSpace = new float[4];

	/**
	 * Used to hold the transformed position of the light in eye space (after
	 * transformation via modelview matrix)
	 */
	private final float[] mLightPosInEyeSpace = new float[4];

	/** This is a handle to our bug shading program. */
	private int mBugProgramHandle;

	/** This is a handle to our light point program. */
	private int mLightProgramHandle;

	/** This is a handle to our bug shading program. */
	private int mFireProgramHandle;

	/** This will be used to pass in fire model position information. */
	private int mFirePositionHandle;

	/** This will be used to pass in fire model color information. */
	private int mFireColorHandle;

	/** This will be used to pass in fire model normal information. */
	private int mFireNormalHandle;

	/** This will be used to pass in fire model texture coordinate information. */
	private int mFireTextureCoordinateHandle;

	/** This is a handle to our bug's texture data. */
	private int mBugTextureDataHandle;

	/** This is a handle to our fire's texture data 1. */
	private int mFireTextureDataHandle1;

	/** This is a handle to our fire's texture data 2. */
	private int mFireTextureDataHandle2;

	/** This is a handle to our fire's texture data 3. */
	private int mFireTextureDataHandle3;

	/** This is a handle to our fire's texture data 4. */
	private int mFireTextureDataHandle4;

	/** This is a handle to our fire's texture data 5. */
	private int mFireTextureDataHandle5;

	/** Hold all the fire flames that should be rendered */
	public List<OpenGLFire> mFireList = new ArrayList<OpenGLFire>();

	/** Random instance */
	public Random rd;

	/** The width of screen, in pixels */
	private int screenOpenGLWidth;

	/** The height of screen, in pixels */
	private int screenOpenGLHeight;

	/** Hold all the bugs that should be counted and calculated in an arraylist */
	private List<OpenGLBug> mBugList = new ArrayList<OpenGLBug>();

	// /** Record the last time we update the speed/position of the bug in
	// opengl */
	// private long lastRefreshBugTime = -1;

	// /** To simulate the reality, bug should halt sometimes */
	// private boolean bugShouldPause = false;

	/** Hold the lines that bugs should be avoided from */
	public ArrayList<BorderLine> borderLineList;

	/** Initialize the model data */
	public OpenGlRenderer(final Context activityContext) {
		this.mActivityContext = activityContext;
		this.mCameraActivityInstance = (CameraActivity) mActivityContext;
		// Initialize the buffers.
		int i = 0;
		BufferedReader buff;
		long now = System.currentTimeMillis();
		float[] cubePositionData = new float[3 * 5580];
		try {
			buff = new BufferedReader(new InputStreamReader(mActivityContext.getAssets().open("vertices")));
			i = 0;
			String tmp = buff.readLine();
			while (tmp != null) {
				if (tmp.equals("")) {
					tmp = buff.readLine();
					continue;
				}
				cubePositionData[i++] = Float.parseFloat(tmp);
				tmp = buff.readLine();
			}
			buff.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mBugPositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBugPositions.put(cubePositionData).position(0);

		float[] cubeColorData = new float[4 * 5580];
		Arrays.fill(cubeColorData, 1.0f);
		mBugColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBugColors.put(cubeColorData).position(0);

		float[] cubeNormalData = new float[3 * 5580];
		try {
			buff = new BufferedReader(new InputStreamReader(mActivityContext.getAssets().open("normal")));
			i = 0;
			String tmp = buff.readLine();
			while (tmp != null) {
				if (tmp.equals("")) {
					tmp = buff.readLine();
					continue;
				}
				cubeNormalData[i++] = Float.parseFloat(tmp);
				tmp = buff.readLine();
			}
			buff.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mBugNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBugNormals.put(cubeNormalData).position(0);

		float[] cubeTextureCoordinateData = new float[2 * 5580];
		try {
			buff = new BufferedReader(new InputStreamReader(mActivityContext.getAssets().open("texture")));
			i = 0;
			String tmp = buff.readLine();
			while (tmp != null) {
				if (tmp.equals("")) {
					tmp = buff.readLine();
					continue;
				}
				cubeTextureCoordinateData[i++] = Float.parseFloat(tmp);
				tmp = buff.readLine();
			}
			buff.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mBugTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBugTextureCoordinates.put(cubeTextureCoordinateData).position(0);
		long cost = System.currentTimeMillis() - now;
		Log.d(TAG, "loading time:" + cost);

		rd = new Random();

		// Load fire data
		final float[] firePositionData = {
				// Front face
				-1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f };

		final float[] fireColorData = {
				// Front face (all white)
				1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };

		final float[] fireNormalData = {
				// Front face
				0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f };

		final float[] fireTextureCoordinateData = {
				// Front face
				0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f };

		// Initialize the buffers.
		mFirePositions = ByteBuffer.allocateDirect(firePositionData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mFirePositions.put(firePositionData).position(0);

		mFireColors = ByteBuffer.allocateDirect(fireColorData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mFireColors.put(fireColorData).position(0);

		mFireNormals = ByteBuffer.allocateDirect(fireNormalData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mFireNormals.put(fireNormalData).position(0);

		mFireTextureCoordinates = ByteBuffer.allocateDirect(fireTextureCoordinateData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mFireTextureCoordinates.put(fireTextureCoordinateData).position(0);

	}

	protected String getVertexShader(int resId) {
		return RawResourceReader.readTextFileFromRawResource(mActivityContext, resId);
	}

	protected String getFragmentShader(int resId) {
		return RawResourceReader.readTextFileFromRawResource(mActivityContext, resId);
	}

	// Position the eye in front of the origin.
	float eyeX = 0.0f;
	float eyeY = 0.0f;
	float eyeZ = 0.0f;

	// We are looking toward the distance
	float lookX = 0.0f;
	float lookY = 0.0f;
	float lookZ = -100.0f;

	// Set our up vector. This is where our head would be pointing were we
	// holding the camera.
	float upX = 0.0f;
	float upY = 1.0f;
	float upZ = 0.0f;

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		Log.d(TAG, "surface screated");
		// Set the background clear color to black.
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// Use culling to remove back faces.
		GLES20.glEnable(GLES20.GL_CULL_FACE);

		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		// The below glEnable() call is a holdover from OpenGL ES 1, and is not
		// needed in OpenGL ES 2.
		// Enable texture mapping
		// GLES20.glEnable(GLES20.GL_TEXTURE_2D);

		// Set the view matrix. This matrix can be said to represent the camera
		// position.
		// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination
		// of a model and
		// view matrix. In OpenGL 2, we can keep track of these matrices
		// separately if we choose.

		final String bugVertexShader = getVertexShader(R.raw.per_pixel_vertex_shader);
		final String bugFragmentShader = getFragmentShader(R.raw.per_pixel_fragment_shader);

		final int bugVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, bugVertexShader);
		final int bugFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, bugFragmentShader);

		mBugProgramHandle = ShaderHelper.createAndLinkProgram(bugVertexShaderHandle, bugFragmentShaderHandle, new String[] { "a_Position", "a_Color", "a_Normal", "a_TexCoordinate" });

		// Load the bug's texture
		mBugTextureDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.ladybug, GLES20.GL_TEXTURE0);

		// Define a simple shader program for our point.
		final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_vertex_shader);
		final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_fragment_shader);

		final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
		final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
		mLightProgramHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle, new String[] { "a_Position" });

		// Define fire program
		final String fireVertexShader = getVertexShader(R.raw.per_pixel_vertex_shader);
		final String fireFragmentShader = getFragmentShader(R.raw.per_pixel_fragment_shader_simple);

		final int fireVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, fireVertexShader);
		final int fireFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fireFragmentShader);

		mFireProgramHandle = ShaderHelper.createAndLinkProgram(fireVertexShaderHandle, fireFragmentShaderHandle, new String[] { "a_Position", "a_Color", "a_Normal", "a_TexCoordinate" });

		// Load the fire's texture
		int[] fireResourcesIds = new int[] { R.drawable.fire_1, R.drawable.fire_2, R.drawable.fire_3, R.drawable.fire_4, R.drawable.fire_5 };
		int[] activeTextureNums = new int[] { GLES20.GL_TEXTURE1, GLES20.GL_TEXTURE2, GLES20.GL_TEXTURE3, GLES20.GL_TEXTURE4, GLES20.GL_TEXTURE5 };
		int[] fireTextureHandlesArr = new int[5];
		fireTextureHandlesArr = TextureHelper.loadTextures(mActivityContext, fireResourcesIds, activeTextureNums);

		mFireTextureDataHandle1 = fireTextureHandlesArr[0];
		mFireTextureDataHandle2 = fireTextureHandlesArr[1];
		mFireTextureDataHandle3 = fireTextureHandlesArr[2];
		mFireTextureDataHandle4 = fireTextureHandlesArr[3];
		mFireTextureDataHandle5 = fireTextureHandlesArr[4];
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		// Set the OpenGL viewport to the same size as the surface.
		GLES20.glViewport(0, 0, width, height);
		screenOpenGLWidth = width;
		screenOpenGLHeight = height;

		// Set OpenGLBug's scale
		OpenGLBug.radius = (int) (screenOpenGLWidth / (SCALE_RATIO * 2));

		eyeX = screenOpenGLWidth / 2;
		eyeY = screenOpenGLHeight / 2;
		lookX = screenOpenGLWidth / 2;
		lookY = screenOpenGLHeight / 2;

		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

		final float left = -width / 2;
		final float right = width / 2;
		final float bottom = -height / 2;
		final float top = height / 2;
		final float near = 0.001f;
		final float far = 3000.0f;

		Matrix.orthoM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
	}

	public volatile float globalRotateDegree = 0;
//	public float distanceX;
//	public float distanceY;

	/** This variable will be incremented every frame. */
	public int fireDrawCount = 1;

	/** This indicates which mode the game is now at */
	public int openGlMode = 0;

	public final int MODE_MAIN_MENU = 1;
	public final int MODE_TUTORIAL_1 = 2;
	public final int MODE_DEFAULT = 0;

	@Override
	public void onDrawFrame(GL10 glUnused) {
		// Clear background to transparency
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		// Calculate current milliseconds
		long now = SystemClock.uptimeMillis() % 10000L;
		// Calculate rotate degree for some use
		// float angleInDegrees = (360.0f / 10000.0f) * ((int) now);

		switch (openGlMode) {

		// Main Menu mode
		case MODE_MAIN_MENU:
			// Set our per-vertex bug program.
			GLES20.glUseProgram(mBugProgramHandle);

			// Load the res handles that will be used in drawing bugs.
			loadOpenGLBugResHandles(mBugProgramHandle);

			// Tell the texture uniform sampler to use this texture in the
			// shader by binding to texture unit 0.
			GLES20.glUniform1i(mTextureUniformHandle, 0);

			// We only want one bug in the main menu
			prepareMainMenuBug();

			// Draw bugs
			drawBugs();

			// Draw a point to indicate the light.
			GLES20.glUseProgram(mLightProgramHandle);

			// // Draw light
			// Matrix.setIdentityM(mLightModelMatrix, 0);
			// Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0,
			// mLightPosInWorldSpace, 0);
			// Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0,
			// mLightPosInModelSpace, 0);
			// drawLight();
			break;

		// Tutorial 1 mode
		case MODE_TUTORIAL_1:
			// if (mFireList.size() == 0)
			// return;

			// Set our per-vertex fire program.
			GLES20.glUseProgram(mFireProgramHandle);

			// Load the res handles that will be used in drawing fire.
			loadOpenGLFireResHandles(mFireProgramHandle);

			// Calculate which fire texture should be binded.
			int fireTextureNum = fireDrawCount / 10 + 1;
			fireDrawCount++;
			if (fireDrawCount > 49)
				fireDrawCount = 1;

			// Tell the texture uniform sampler to use this texture in the
			// shader by binding to texture unit fireTextureNum.
			GLES20.glUniform1i(mTextureUniformHandle, fireTextureNum);

			// Draw fires
			drawFires();

			// Set our per-vertex bug program.
			GLES20.glUseProgram(mBugProgramHandle);

			// Load the res handles that will be used in drawing bugs.
			loadOpenGLBugResHandles(mBugProgramHandle);

			// Tell the texture uniform sampler to use this texture in the
			// shader by binding to texture unit 0.
			GLES20.glUniform1i(mTextureUniformHandle, 0);

			// Draw bugs
			drawBugs();

			// Log.d(TAG, "czx mBugList size : " + mBugList.size());
			break;

		default:
			// Don't render anything
		}

		// upX = (float)
		// (Math.abs(Math.tan(Math.toRadians(globalRotateDegree)))) ;
		//
		// if (globalRotateDegree >= 180 && globalRotateDegree <360) {
		// upX = - upX;
		// }
		//
		// if (globalRotateDegree >=90 && globalRotateDegree <270) {
		// upY = -1.0f;
		// } else {
		// upY = 1.0f;
		// }

		// switch(fire) {
		// case 1:
		// GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
		// break;
		// case 2:
		// GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle1);
		// break;
		// case 3:
		// GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle2);
		// break;
		// }
		// fire = (fire + 1) % 3;

		// Calculate position of the light. Rotate and then push into th
		// distance.

	}

	/**
	 * Load the res handles that will be used in drawing bugs.
	 * 
	 * @param handle
	 *            The handle that you want to bind
	 */
	private void loadOpenGLBugResHandles(int handle) {
		mMVMatrixHandle = GLES20.glGetUniformLocation(handle, "u_MVMatrix");
		mMVPMatrixHandle = GLES20.glGetUniformLocation(handle, "u_MVPMatrix");
		mTextureUniformHandle = GLES20.glGetUniformLocation(handle, "u_Texture");
		mLightPosHandle = GLES20.glGetUniformLocation(handle, "u_LightPos");

		// Set program handles for bug drawing.
		mBugPositionHandle = GLES20.glGetAttribLocation(handle, "a_Position");
		mBugColorHandle = GLES20.glGetAttribLocation(handle, "a_Color");
		mBugNormalHandle = GLES20.glGetAttribLocation(handle, "a_Normal");
		mBugTextureCoordinateHandle = GLES20.glGetAttribLocation(handle, "a_TexCoordinate");
	}

	/** Draws bugs in mBugList */
	private void drawBugs() {
		ListIterator<OpenGLBug> mOpenGLBugListIterator = mBugList.listIterator();
		while (mOpenGLBugListIterator.hasNext()) {
			OpenGLBug mOpenGLBug = mOpenGLBugListIterator.next();

			// Load the model matrix as identity matrix
			Matrix.setIdentityM(mModelMatrix, 0);

			// Our projection is ortho.
			Matrix.translateM(mModelMatrix, 0, mOpenGLBug.x, mOpenGLBug.y, -500.0f);

			// Rotate to bug's speed direction
			Matrix.rotateM(mModelMatrix, 0, headRotate(mOpenGLBug.speedX, mOpenGLBug.speedY), 0, 0, -1.0f);
			// Rotate in order to make the bug's back facing us.
			Matrix.rotateM(mModelMatrix, 0, 180, 0.0f, 1.0f, 0.0f);
			Matrix.rotateM(mModelMatrix, 0, 90, -1.0f, 0.0f, 0.0f);

			// The original 3d obj model is too small. So we scale it by
			// screenWidth/SCALE_RATIO times. That's also why we set
			// OpenGLBug.radius to screenOpenGLWidth/(SCALE_RATIO * 2)
			Matrix.scaleM(mModelMatrix, 0, screenOpenGLWidth / mOpenGLBug.scaleRatio, screenOpenGLWidth / mOpenGLBug.scaleRatio, 100f);

			// Pass in the position information
			mBugPositions.position(0);
			GLES20.glVertexAttribPointer(mBugPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, 0, mBugPositions);

			GLES20.glEnableVertexAttribArray(mBugPositionHandle);

			// Pass in the color information
			mBugColors.position(0);
			GLES20.glVertexAttribPointer(mBugColorHandle, mColorDataSize, GLES20.GL_FLOAT, false, 0, mBugColors);

			GLES20.glEnableVertexAttribArray(mBugColorHandle);

			// Pass in the normal information
			mBugNormals.position(0);
			GLES20.glVertexAttribPointer(mBugNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 0, mBugNormals);

			GLES20.glEnableVertexAttribArray(mBugNormalHandle);

			// Pass in the texture coordinate information
			mBugTextureCoordinates.position(0);
			GLES20.glVertexAttribPointer(mBugTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 0, mBugTextureCoordinates);

			GLES20.glEnableVertexAttribArray(mBugTextureCoordinateHandle);

			// This multiplies the view matrix by the model matrix, and stores
			// the
			// result in the MVP matrix
			// (which currently contains model * view).
			Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

			// Pass in the modelview matrix.
			GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

			// This multiplies the modelview matrix by the projection matrix,
			// and
			// stores the result in the MVP matrix
			// (which now contains model * view * projection).
			Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

			// Pass in the combined matrix.
			GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

			// Pass in the light position in eye space.
			GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

			// Draw the cube.
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 5580);

			// Rerfresh the bug status
			mOpenGLBug = refreshBug(mOpenGLBug, mOpenGLBugListIterator);

			// // If the bug is marked as "shouldBeRemoved", it should be
			// removed from ArrayList
			// if (mOpenGLBug.shouldBeRemoved) {
			// mOpenGLBugIterator.remove();
			// }
		}

		// Iterator<OpenGLBug> mOpenGLBugIterator = mBugList.iterator();
		// while (mOpenGLBugIterator.hasNext() ) {
		// OpenGLBug mOpenGLBug = mOpenGLBugIterator.next();
		// mOpenGLBugIterator.remo
		// }
		//
	}

	/**
	 * Load the res handles that will be used in drawing fires.
	 * 
	 * @param handle
	 *            The handle that you want to bind
	 */
	private void loadOpenGLFireResHandles(int handle) {
		mMVMatrixHandle = GLES20.glGetUniformLocation(handle, "u_MVMatrix");
		mMVPMatrixHandle = GLES20.glGetUniformLocation(handle, "u_MVPMatrix");
		mTextureUniformHandle = GLES20.glGetUniformLocation(handle, "u_Texture");
		mLightPosHandle = GLES20.glGetUniformLocation(handle, "u_LightPos");

		// Set program handles for bug drawing.
		mFirePositionHandle = GLES20.glGetAttribLocation(handle, "a_Position");
		mFireColorHandle = GLES20.glGetAttribLocation(handle, "a_Color");
		mFireNormalHandle = GLES20.glGetAttribLocation(handle, "a_Normal");
		mFireTextureCoordinateHandle = GLES20.glGetAttribLocation(handle, "a_TexCoordinate");
	}

	/** Draw fires in mFireList */
	private void drawFires() {
		for (OpenGLFire mOpenGLFire : mFireList) {
			// Load the model matrix as identity matrix
			Matrix.setIdentityM(mModelMatrix, 0);

			// Our projection is ortho.
			Matrix.translateM(mModelMatrix, 0, (float) mOpenGLFire.ratioX * screenOpenGLWidth, (float) mOpenGLFire.ratioY * screenOpenGLHeight, -500.0f);

			// The original coordinate model is too small. So we scale it by
			// screenWidth/SCALE_RATIO
			// times.
			Matrix.scaleM(mModelMatrix, 0, screenOpenGLWidth / SCALE_RATIO, screenOpenGLWidth / SCALE_RATIO, 100f);

			// Pass in the position information
			mFirePositions.position(0);
			GLES20.glVertexAttribPointer(mFirePositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, 0, mFirePositions);

			GLES20.glEnableVertexAttribArray(mFirePositionHandle);

			// Pass in the color information
			mFireColors.position(0);
			GLES20.glVertexAttribPointer(mFireColorHandle, mColorDataSize, GLES20.GL_FLOAT, false, 0, mFireColors);

			GLES20.glEnableVertexAttribArray(mFireColorHandle);

			// Pass in the normal information
			mFireNormals.position(0);
			GLES20.glVertexAttribPointer(mFireNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 0, mFireNormals);

			GLES20.glEnableVertexAttribArray(mFireNormalHandle);

			// Pass in the texture coordinate information
			mFireTextureCoordinates.position(0);
			GLES20.glVertexAttribPointer(mFireTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 0, mFireTextureCoordinates);

			GLES20.glEnableVertexAttribArray(mFireTextureCoordinateHandle);

			// This multiplies the view matrix by the model matrix, and stores
			// the
			// result in the MVP matrix
			// (which currently contains model * view).
			Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

			// Pass in the modelview matrix.
			GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

			// This multiplies the modelview matrix by the projection matrix,
			// and
			// stores the result in the MVP matrix
			// (which now contains model * view * projection).
			Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

			// Pass in the combined matrix.
			GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

			// Pass in the light position in eye space.
			GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

			// Draw the cube.
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
		}
	}

	/**
	 * Draws a point representing the position of the light.
	 */
	private void drawLight() {
		final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mLightProgramHandle, "u_MVPMatrix");
		final int pointPositionHandle = GLES20.glGetAttribLocation(mLightProgramHandle, "a_Position");

		// Pass in the position.
		GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

		// Since we are not using a buffer object, disable vertex arrays for
		// this attribute.
		GLES20.glDisableVertexAttribArray(pointPositionHandle);

		// Pass in the transformation matrix.
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
		GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Draw the point.
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
	}

	/* Bug rendering */
	/** Determine the bug's head rotation */
	private float headRotate(int speedX, int speedY) {
		float rotateDegree = 0;
		double rotateRadian;
		if (speedX == 0) {
			if (speedY < 0)
				rotateRadian = 0;
			else
				rotateRadian = Math.PI / 2;
		} else {
			rotateRadian = Math.atan(Math.abs((double) speedY / speedX));
		}

		rotateDegree = (float) (Math.toDegrees(rotateRadian));

		if (speedX > 0 && speedY < 0) {
			rotateDegree = 90 + rotateDegree;
		}
		if (speedX > 0 && speedY > 0) {
			rotateDegree = 90 - rotateDegree;
		}
		if (speedX < 0 && speedY < 0) {
			rotateDegree = 90 + rotateDegree;
			rotateDegree = -rotateDegree;
		}
		if (speedX < 0 && speedY > 0) {
			rotateDegree = 90 - rotateDegree;
			rotateDegree = -rotateDegree;
		}
		return rotateDegree;
	}

	private OpenGLBug refreshBug(OpenGLBug bug, ListIterator<OpenGLBug> mOpenGLBugIterator) {
		// That means CameraActivity hasn't initialized the border lines.
		// if (borderLineList == null)
		// return bug;
		int polarityX;
		int polarityY;
		int tmpX;
		int tmpY;
		switch (openGlMode) {
		case MODE_MAIN_MENU:
			polarityX = bug.speedX >= 0 ? 1 : -1;
			polarityY = bug.speedY >= 0 ? 1 : -1;
			tmpX = bug.x + bug.speedX + OpenGLBug.relativeSpeedX;
			tmpY = bug.y + bug.speedY + OpenGLBug.relativeSpeedY;

			if (bug.shouldPause) {
				if (System.currentTimeMillis() - bug.lastRefreshTime > 2000) {
					bug.shouldPause = false;
				}
				return bug;
			}
			if (rd.nextInt(100) > 98) {
				bug.shouldPause = true;
				return bug;
			}

			if (tmpX + polarityX * OpenGLBug.radius > screenOpenGLWidth || tmpX + polarityX * OpenGLBug.radius < 0) {
				bug.speedX = -bug.speedX;
				tmpX = bug.x;
			}
			if (tmpY + polarityY * OpenGLBug.radius > screenOpenGLHeight || tmpY + polarityY * OpenGLBug.radius < 0) {
				bug.speedY = -bug.speedY;
				tmpY = bug.y;
			}

			bug.x = tmpX;
			bug.y = tmpY;
			bug.lastRefreshTime = System.currentTimeMillis();
			break;
		case MODE_TUTORIAL_1:
			tmpX = bug.x + bug.speedX + OpenGLBug.relativeSpeedX;
			tmpY = bug.y + bug.speedY + OpenGLBug.relativeSpeedY;

			// If the bug runs out of the boundary. we remove it
			if (isBugOutOfBoundary(tmpX, tmpY)) {
				// Mark the bug shouldBeRemoved
				mOpenGLBugIterator.remove();
				return bug;
			} else {
				// If the bug runs out of the screen, we generate a new one
				if (isBugOutOfScreen(tmpX, tmpY)) {
					if (mBugList.size() < 3) {
						OpenGLBug mTutorial1Bug = generateTutorial1Bug();
						mOpenGLBugIterator.add(mTutorial1Bug);
					}
				} else {
					// If the bug is still in the screen AND it is not burning
					if (!bug.burning) {
						if (ifFireHitsBug(tmpX, tmpY)) {
//							Log.d(TAG, "czx openglbug radius:" + OpenGLBug.radius + " dist:" + distFromBugToFire);
							bug.burning = true;
							bug.shouldPause = true;
							updateScore(bug.type);
						}
					}
				}
				
				// If the bug is burning to the end, remove it and also add a new bug
				if (bug.burning) {
					bug.burningStepCounter++;
					if (bug.burningStepCounter == OpenGLBug.BURNING_STEP) {
						mOpenGLBugIterator.remove();
						if (mBugList.size() < 3) {
							OpenGLBug mTutorial1Bug = generateTutorial1Bug();
							mOpenGLBugIterator.add(mTutorial1Bug);
						}
						return bug;
					}
				}
				
				if (bug.bouncing) {
					bug.bounceStepCounter++;
					// If bounce step counter hits OpenGLBug.BOUNCING_STEP, we
					// clear the bouncing information.
					// We wouldn't update its (x,y) info. However we would keep
					// its (speedX, speedY)info because that matters with head
					// rotation
					if (bug.bounceStepCounter == OpenGLBug.BOUNCING_STEP) {
						bug.bouncing = false;
						bug.bounceStepCounter = 0;
						bug.shouldPause = true;
					}
				}

				// We need to update the bug if it is in the valid region and
				// shouldPause==false.
				if (!bug.shouldPause) {
					bug.x = tmpX;
					bug.y = tmpY;
					bug.lastRefreshTime = System.currentTimeMillis();
				} else {
					if (!bug.burning && System.currentTimeMillis() - bug.lastRefreshTime > rd.nextInt(10000)) {
						bug.shouldPause = false;
					}
				}

			}

			break;
		default:

		}
		return bug;
	}

	/**
	 * In the main menu, there is only one bug. This method will either return a
	 * bug that has been created in mBugList, or create a new bug in mListBug if
	 * it is empty or has more than one bug in it.
	 */
	private void prepareMainMenuBug() {
		if (mBugList.size() != 1) {
			mBugList.clear();
			generateMainMenuBug();
		}
	}

	/** Generate one main menu bug if necessary */
	private void generateMainMenuBug() {
		if (mBugList.size() == 0) {
			int randomHeight = rd.nextInt(screenOpenGLHeight / 3);
			OpenGLBug menuBug = new OpenGLBug(OpenGLBug.TYPE_MENUBUG, screenOpenGLWidth - OpenGLBug.radius, screenOpenGLHeight / 4 + randomHeight, -1, 1, SCALE_RATIO);
			mBugList.add(menuBug);
		}
	}

	/** Generate one tutorial 1 bug */
	private OpenGLBug generateTutorial1Bug() {
		// bugs will show up from left or right flank side. So the width is
		// either 0+radius OR screenwidht-radius
		int randomHeight = randInt(OpenGLBug.radius, screenOpenGLHeight / 2);
		int randomWidth;
		if (rd.nextInt(2) == 0) {
			randomWidth = OpenGLBug.radius;
		} else {
			randomWidth = screenOpenGLWidth - OpenGLBug.radius;
		}
		// Make sure that abs(speedX) and abs(speedY) is at least 1
		int[] destination = findBugNextDest();
		int xDiff = destination[0] - randomWidth;
		int yDiff = destination[1] - randomHeight;
		int speedX;
		int speedY;

		if (xDiff == 0) {
			speedX = 0;
		} else {
			speedX = xDiff > OpenGLBug.BOUNCING_STEP ? xDiff / OpenGLBug.BOUNCING_STEP : xDiff / Math.abs(xDiff);
		}

		if (yDiff == 0) {
			speedY = 0;
		} else {
			speedY = yDiff > OpenGLBug.BOUNCING_STEP ? yDiff / OpenGLBug.BOUNCING_STEP : yDiff / Math.abs(yDiff);
		}

		OpenGLBug tutorial1Bug = new OpenGLBug(OpenGLBug.TYPE_FIREBUG, randomWidth, randomHeight, speedX, speedY, SCALE_RATIO, true, destination[0], destination[1], 0);
		return tutorial1Bug;
	}

	/** In the tutorial 1, there is only one bug. */
	public void prepareForTutorial1() {
		mBugList.clear();
		OpenGLBug mTutorial1Bug = generateTutorial1Bug();
		mBugList.add(mTutorial1Bug);
	}

	/** Return the bug's next destination (in opengl coordinate) */
	private int[] findBugNextDest() {
		double[] resultArray = mCameraActivityInstance.findBugNextDest();
		int[] destination = new int[2];
		destination[0] = (int) (screenOpenGLWidth * resultArray[0]);
		destination[1] = (int) (screenOpenGLHeight * resultArray[1]);
		return destination;
	}

	/**
	 * Check if the bug is now out of the rectangle(-screenOpenGLWidth,
	 * 2*screenOpenGLHeight, 3*screenOpenGLWidth, 3*screenOpenGLHeight)
	 */
	private boolean isBugOutOfBoundary(int tmpX, int tmpY) {
		if (tmpX > screenOpenGLWidth * 2 || tmpX < -screenOpenGLWidth || tmpY > 2 * screenOpenGLHeight || tmpY < -screenOpenGLHeight)
			return true;
		else
			return false;
	}

	/**
	 * Check if the bug is out of the rectangle(0, screenOpenGLHeight,
	 * screenOpenGLWidth, screenOpenGLHeight)
	 */
	private boolean isBugOutOfScreen(int tmpX, int tmpY) {
		if (tmpX > screenOpenGLWidth || tmpX < 0 || tmpY > screenOpenGLHeight || tmpY < 0)
			return true;
		else
			return false;
	}

	private void updateScore(int bugType) {
		switch(bugType) {
		case OpenGLBug.TYPE_FIREBUG:
			mCameraActivityInstance.updateScore(1);
			break;
		default:
			break;
		}
	}
	
	/** generate an integer between a range */
	public int randInt(int min, int max) {
		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rd.nextInt((max - min) + 1) + min;

		return randomNum;
	}
	
	/** Determine whether the bug is burned by return the max distance between the bug and two fire flames */
	private boolean ifFireHitsBug(int tmpX, int tmpY) {
		for (OpenGLFire mOpenGLFire : mFireList) {
			long xDiff = tmpX - (int)(mOpenGLFire.ratioX * screenOpenGLWidth);
			long yDiff = tmpY - (int)(mOpenGLFire.ratioY * screenOpenGLHeight);
			double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
			if (distance < 4 * OpenGLBug.radius) {
				Log.d(TAG, "czx hits:" + distance);
				return true;
			}
		}
		return false;
	}
}
