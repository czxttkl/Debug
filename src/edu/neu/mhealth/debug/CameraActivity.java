package edu.neu.mhealth.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import edu.neu.mhealth.debug.helper.Global;
import edu.neu.mhealth.debug.helper.InitRenderTask;
import edu.neu.mhealth.debug.opengl.OpenGLBug;
import edu.neu.mhealth.debug.opengl.OpenGLFire;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CameraActivity extends Activity implements CvCameraViewListener2, SensorEventListener {

	/* Basic Variables */
	/** Debug Tag */
	private final String TAG = Global.APP_LOG_TAG;
	/**
	 * The game activity's framelayout. Use this to handle
	 * adding/removingsurfaceviews
	 */

	/** Record how many bugs the user has killed */
	public int score = 0;

	FrameLayout mFrameLayout;

	/**
	 * The layout for color picking. Including instructions and confirm buttons.
	 */
	RelativeLayout mColorPickLayout;

	/** Used for semi-transparent black background */
	ImageView mMainMenuBackground;

	/** The "Debug" title appearing on the main menu, inflated by main_menu.xml */
	ImageView mMainMenuTitle;

	/** Hold the buttons in the main menu screen */
	View mMainMenuButtonListView;

	/** The whole about screen view. It will be inflated by about_screen.xml */
	View mAboutView;

	/** The text in the about screen */
	TextView mAboutText;

	/** Used for semi-transparent black background */
	Drawable blackBackground;

	/** Screen width in pixels */
	public int screenPixelWidth;

	/** Screen height in pixels */
	public int screenPixelHeight;

	/** Screen width in opencv rows */
	public int screenOpenCvWidth;

	/** Screen height in opencv rows */
	public int screenOpenCvHeight;

	/** X axis unit ratio for OpenCv/OpenGL */
	public double openCVGLRatioX;

	/** Y axis unit ratio for OpenCv/OpenGL */
	public double openCVGLRatioY;

	/* OpenCv Variables */
	private CameraBridgeViewBase mOpenCvCameraView;
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				restoreOrCreateJavaCameraView();
				restoreOrCreateGLSurfaceView();
				restoreOrCreateAboutScreen();
				// restoreOrCreateMainMenu();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}

	};

	/* OpenGl Variables */
	/** OpenGL surface view for displaying bugs */
	public MyGLSurfaceView mGLSurfaceView;

	/* Sensor Variables */
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	protected final Handler mHandler = new Handler();

	// private final long PACE_TWO_OPPOSITE_PEAK_INTERVAL = 2000;

	/*
	 * Activity Callbacks
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mFrameLayout = new FrameLayout(this);
		setContentView(mFrameLayout);
		storeScreenDimensions();
		initSensors();
		initBlackBackground();
		// initAnimations();
	}

	@Override
	protected void onPause() {
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
			saveAndRemoveSurfaceViews();
		}
		pauseSensors();
		super.onPause();
	}

	@Override
	protected void onResume() {
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
		resumeSensors();
		super.onResume();
	}

	View mColorPickBottomBar;
	View mColorPickInstructionsView;
	View mColorPickHelpNotif;
	View mColorPickHelpConfirmButt1;
	View mColorPickHelpConfirmButt2;
	View mColorPickCloseButton;
	View mColorPickCameraButton;
	TextView mColorPickHelpNotifTextView;

	public void onClickAboutStartButton(View v) {
		mFrameLayout.removeView(mAboutView);
		restoreOrCreateMainMenu();
	}

	public void onClickMainMenuStartGame(View v) {
		mFrameLayout.removeView(mMainMenuTitle);
		mFrameLayout.removeView(mMainMenuButtonListView);
		mFrameLayout.removeView(mMainMenuBackground);

		// OpenGL shouldn't render anything right after clicking start game.
		mGLSurfaceView.mRenderer.openGlMode = mGLSurfaceView.mRenderer.MODE_DEFAULT;

		// Inflates the Overlay Layout to be displayed above the Camera View
		LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		mColorPickLayout = (RelativeLayout) layoutInflater.inflate(R.layout.color_pick_overlay, null, false);

		// Set background to semi-transparent black
		blackBackground.setAlpha(200);
		mColorPickLayout.setBackgroundDrawable(blackBackground);

		mFrameLayout.addView(mColorPickLayout);
		// Gets a reference to the bottom navigation bar
		mColorPickBottomBar = mColorPickLayout.findViewById(R.id.bottom_bar);

		// Gets a reference to the instructions view
		mColorPickInstructionsView = mColorPickLayout.findViewById(R.id.instructions);

		// Gets a reference to the help notification viewstub
		mColorPickHelpNotif = mColorPickLayout.findViewById(R.id.overlay_color_pick_help);

		// Gets a reference to the color pick confirm buttons1 viewstub (shoe
		// color pick confirm buttons)
		mColorPickHelpConfirmButt1 = mColorPickLayout.findViewById(R.id.overlay_color_pick_confirm_button1);

		// Gets a reference to the color pick confirm buttons2 viewstub (floor
		// color pick confirm buttons)
		mColorPickHelpConfirmButt2 = mColorPickLayout.findViewById(R.id.overlay_color_pick_confirm_button2);

		// Gets a reference to the CloseBuildTargetMode button
		mColorPickCloseButton = mColorPickLayout.findViewById(R.id.close_button);

		// Gets a reference to the Camera button
		mColorPickCameraButton = mColorPickLayout.findViewById(R.id.camera_button);

		initializeInstructionMode();
	}

	public void onClickMainMenuExit(View v) {
		finish();
	}

	public void onClickMainMenuSettings(View v) {

	}

	/**
	 * Button Camera clicked. Marker's color is picked after this method is
	 * called.
	 */
	public void onClickColorPickCameraButton(View v) {
		if (!shoeColorPicked) {
			shoeColorPicked = true;
			mColorBlobDetector.setShoeHsvColor(mShoeColorPickHsv);
			openCvMode = MODE_SHOE_COLOR_PICKED;
			mColorPickHelpNotifTextView.setVisibility(View.GONE);
			mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_confirm_shoe);
			mColorPickHelpNotifTextView.setTextColor(Color.WHITE);
			mColorPickHelpNotifTextView.setVisibility(View.VISIBLE);
			mColorPickHelpConfirmButt1.setVisibility(View.VISIBLE);
			mColorPickBottomBar.setVisibility(View.GONE);
		} else {
			floorColorPicked = true;
			mColorBlobDetector.setFloorHsvColor(mFloorColorPickHsv);
			openCvMode = MODE_FLOOR_COLOR_PICKED;
			mColorPickHelpNotifTextView.setVisibility(View.GONE);
			mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_confirm_floor);
			mColorPickHelpNotifTextView.setTextColor(Color.WHITE);
			mColorPickHelpNotifTextView.setVisibility(View.VISIBLE);
			mColorPickHelpConfirmButt2.setVisibility(View.VISIBLE);
			mColorPickBottomBar.setVisibility(View.GONE);
		}

	}

	/** Button Close/Cancel clicked in the process of color picking */
	public void onClickColorPickCameraClose(View v) {
		// Set background to semi-transparent black
		blackBackground.setAlpha(200);
		mColorPickLayout.setBackgroundDrawable(blackBackground);

		// Goes back to the Color Pick Add rMode
		initializeInstructionMode();

		// Set mode to default
		openCvMode = 0;

		// Set not picked shoes&floors' color yet
		shoeColorPicked = false;
		floorColorPicked = false;
	}

	/** Button Add clicked - This will cause the instruction interfaces shows. */
	public void onClickColorPickAddButton(View v) {
		// Shows the instructions view and returns
		mColorPickInstructionsView.setVisibility(View.VISIBLE);
	}

	/** Instructions button OK clicked */
	public void onClickInstructionsOnOk(View v) {
		// Hides the instructions view
		mColorPickInstructionsView.setVisibility(View.GONE);
		// To enter the ColorPickCameraMode
		initializeColorPickCameraMode();
	}

	/** Instructions button Cancel clicked */
	public void onClickInstructionsOnCancel(View v) {
		// Hides the instructions view without
		// updating the instructions flag
		// mColorPickInstructionsView.setVisibility(View.GONE);
		// mColorPickNewTargetButton.setEnabled(true);
		mFrameLayout.removeView(mColorPickLayout);
		restoreOrCreateMainMenu();
		mGLSurfaceView.mRenderer.openGlMode = mGLSurfaceView.mRenderer.MODE_MAIN_MENU;
	}

	/** Shoe color pick confirm cancel button clicked */
	public void onClickShoeColorPickConfirmCancel(View v) {
		mColorPickBottomBar.setVisibility(View.VISIBLE);
		mColorPickHelpNotifTextView.setVisibility(View.GONE);
		mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_shoe);
		mColorPickHelpNotifTextView.setTextColor(Color.WHITE);
		mColorPickHelpNotifTextView.setVisibility(View.VISIBLE);
		mColorPickHelpConfirmButt1.setVisibility(View.GONE);
		openCvMode = MODE_COLOR_PICK_CROSSHAIR;
		// Set not picked shoes' color yet
		shoeColorPicked = false;
	}

	/** Shoe color pick confirm ok button clicked */
	public void onClickShoeColorPickConfirmOk(View v) {
		openCvMode = MODE_COLOR_PICK_CROSSHAIR;
		mColorPickHelpConfirmButt1.setVisibility(View.GONE);
		// To enter the ColorPickCameraMode
		initializeColorPickCameraMode();
	}

	/** Floor color pick confirm cancel button clicked */
	public void onClickFloorColorPickConfirmCancel(View v) {
		mColorPickBottomBar.setVisibility(View.VISIBLE);
		mColorPickHelpNotifTextView.setVisibility(View.GONE);
		mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_floor);
		mColorPickHelpNotifTextView.setTextColor(Color.WHITE);
		mColorPickHelpNotifTextView.setVisibility(View.VISIBLE);
		mColorPickHelpConfirmButt2.setVisibility(View.GONE);
		openCvMode = MODE_COLOR_PICK_CROSSHAIR;
	}

	/** Floor color pick confirm ok button clicked */
	public void onClickFloorColorPickConfirmOk(View v) {
		mFrameLayout.removeView(mColorPickLayout);
		openCvMode = MODE_TUTORIAL_1;
		mGLSurfaceView.mRenderer.prepareForTutorial1();
		mGLSurfaceView.mRenderer.openGlMode = mGLSurfaceView.mRenderer.MODE_TUTORIAL_1;
	}

	/** Initialize Color Pick Add mode views */
	private void initializeInstructionMode() {
		// Shows the bottom bar with the new Target Button
		mColorPickBottomBar.setVisibility(View.GONE);
		// Hides the target build controls
		mColorPickHelpNotif.setVisibility(View.GONE);
		mColorPickCameraButton.setVisibility(View.GONE);
		mColorPickCloseButton.setVisibility(View.GONE);
		mColorPickInstructionsView.setVisibility(View.VISIBLE);
	}

	/** Initialize Color Pick Camera mode views */
	private void initializeColorPickCameraMode() {
		// Shows the bottom bar with the Build target options
		mColorPickHelpNotif.setVisibility(View.VISIBLE);
		mColorPickBottomBar.setVisibility(View.VISIBLE);
		mColorPickCameraButton.setVisibility(View.VISIBLE);
		mColorPickCloseButton.setVisibility(View.VISIBLE);

		// Gets a reference to the help notification textview
		mColorPickHelpNotifTextView = (TextView) mColorPickLayout.findViewById(R.id.color_pick_help_notif_text);

		// If shoes' color hasn't been picked, show color_pick_help_notif_shoe
		if (!shoeColorPicked) {
			mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_shoe);
		} else {
			// If shoes' color has been picked, show color_pick_help_notif_floor
			mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_floor);
		}

		// Set background to transparent
		blackBackground.setAlpha(0);
		mColorPickLayout.setBackgroundDrawable(blackBackground);

		// Set opencvmode to crosshair mode, so opencv will draw a crosshair in
		// the center of image
		openCvMode = MODE_COLOR_PICK_CROSSHAIR;
	}

	/*
	 * Opencv Callbacks
	 */
	Mat mGray;
	Mat mRgba;
	Mat colorPickAreaHsv;
	Scalar grayColor;
	Scalar blueColor;
	Scalar redColor;
	org.opencv.core.Point crosshairHeftmost;
	org.opencv.core.Point crosshairRightmost;
	org.opencv.core.Point crosshairUpmost;
	org.opencv.core.Point crosshairDownmost;
	private Scalar mShoeColorPickRgba;
	private Scalar mShoeColorPickHsv;
	private Scalar mFloorColorPickRgba;
	private Scalar mFloorColorPickHsv;
	int openCvMode;
	public static final int MODE_COLOR_PICK_CROSSHAIR = 123;
	public static final int MODE_SHOE_COLOR_PICKED = 124;
	public static final int MODE_COLOR_PICK_HOLD_WRONGLY = 125;
	public static final int MODE_FLOOR_COLOR_PICKED = 126;
	public static final int MODE_TUTORIAL_1 = 127;
	private boolean shoeColorPicked = false;
	private boolean floorColorPicked = false;
	Rect colorPickArea;
	ColorDetector mColorBlobDetector;
	List<MatOfPoint> detectedShoesContours;
	List<MatOfPoint> detectedFloorContours;

	@Override
	public void onCameraViewStarted(int width, int height) {
		// Set the openCv dimension
		screenOpenCvWidth = width;
		screenOpenCvHeight = height;
		// We assume screenPixel dimensions are the same with opengl coordinate
		openCVGLRatioX = (double) screenOpenCvWidth / screenPixelWidth;
		openCVGLRatioY = (double) screenOpenCvHeight / screenPixelHeight;

		mGray = new Mat();
		mRgba = new Mat();
		colorPickAreaHsv = new Mat();
		grayColor = new Scalar(192, 192, 192);
		blueColor = new Scalar(51, 181, 229);
		redColor = new Scalar(255, 0, 0);
		crosshairHeftmost = new org.opencv.core.Point(width / 2 - 50, height / 2);
		crosshairRightmost = new org.opencv.core.Point(width / 2 + 50, height / 2);
		crosshairUpmost = new org.opencv.core.Point(width / 2, height / 2 - 50);
		crosshairDownmost = new org.opencv.core.Point(width / 2, height / 2 + 50);
		colorPickArea = new Rect();
		colorPickArea.x = width / 2 - 5;
		colorPickArea.y = height / 2 - 5;
		colorPickArea.width = 10;
		colorPickArea.height = 10;
		mColorBlobDetector = new ColorDetector(width, height);
	}

	@Override
	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mGray = inputFrame.gray();
		mRgba = inputFrame.rgba();
		// Log.d(TAG, "resolution real:" + mRgba.cols() + ":" + mRgba.rows());
		switch (openCvMode) {

		case MODE_COLOR_PICK_CROSSHAIR:
			Mat colorPickAreaRgba = mRgba.submat(colorPickArea);
			Imgproc.cvtColor(colorPickAreaRgba, colorPickAreaHsv, Imgproc.COLOR_RGB2HSV_FULL);
			int pointCount = colorPickArea.width * colorPickArea.height;

			// Pick shoes' color
			if (!shoeColorPicked) {
				// Calculate average color of shoes' color pick region
				mShoeColorPickHsv = Core.sumElems(colorPickAreaHsv);
				for (int i = 0; i < mShoeColorPickHsv.val.length; i++)
					mShoeColorPickHsv.val[i] /= pointCount;
				mShoeColorPickRgba = converScalarHsv2Rgba(mShoeColorPickHsv);
				Mat colorLabel = mRgba.submat(44, 108, 8, 72);
				colorLabel.setTo(mShoeColorPickRgba);
			} else {
				mFloorColorPickHsv = Core.sumElems(colorPickAreaHsv);
				for (int i = 0; i < mFloorColorPickHsv.val.length; i++)
					mFloorColorPickHsv.val[i] /= pointCount;
				mFloorColorPickRgba = converScalarHsv2Rgba(mFloorColorPickHsv);
				Mat colorLabel = mRgba.submat(44, 108, 8, 72);
				colorLabel.setTo(mFloorColorPickRgba);
			}
			Core.line(mRgba, crosshairHeftmost, crosshairRightmost, blueColor, 10);
			Core.line(mRgba, crosshairUpmost, crosshairDownmost, blueColor, 10);
			break;

		case MODE_SHOE_COLOR_PICKED:
			mColorBlobDetector.process(mRgba, openCvMode);
			detectedShoesContours = mColorBlobDetector.getShoesContours();
			// Log.e(TAG, "Contours count: " + contours.size());
			if (detectedShoesContours == null)
				return mRgba;
			detectedShoesContours.removeAll(Collections.singleton(null));
			Imgproc.drawContours(mRgba, detectedShoesContours, -1, redColor, 6);
			break;

		case MODE_COLOR_PICK_HOLD_WRONGLY:
			Core.line(mRgba, crosshairHeftmost, crosshairRightmost, grayColor, 10);
			Core.line(mRgba, crosshairUpmost, crosshairDownmost, grayColor, 10);
			break;

		case MODE_FLOOR_COLOR_PICKED:
			mColorBlobDetector.process(mRgba, openCvMode);
			detectedFloorContours = mColorBlobDetector.getFloorContours();
			if (detectedFloorContours == null)
				return mRgba;
			detectedFloorContours.removeAll(Collections.singleton(null));
			Imgproc.drawContours(mRgba, detectedFloorContours, -1, redColor, 4);

		case MODE_TUTORIAL_1:
			mColorBlobDetector.process(mRgba, openCvMode);
			detectedShoesContours = mColorBlobDetector.getShoesContours();
			// if (detectedShoesContours == null)
			// return mRgba;
			detectedShoesContours.removeAll(Collections.singleton(null));
			setRendererContourMassCenter();

			detectedFloorContours = mColorBlobDetector.getFloorContours();
			detectedFloorContours.removeAll(Collections.singleton(null));
			Imgproc.drawContours(mRgba, detectedFloorContours, -1, redColor, 4);
			break;

		default:
			// Do no process here
		}
		return mRgba;
	}

	/** convert hsv color to rgba color in Scalar class */
	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
		return new Scalar(pointMatRgba.get(0, 0));
	}

	/**
	 * Determine the bug's next destination that it should bounce to. This
	 * method will return a double array. array[0] is the x-axis ratio of
	 * destination. array[1] is the y-axis ratio of destination. The ratio is
	 * according to the opengl coordinate.
	 */
	public double[] findBugNextDest() {
		int randomWidth;
		int randomHeight;
		int result;
		do {
			// We assume that the screenPixelWidth is the same with the opengl
			// coordinate
			int minX = screenOpenCvWidth / 4;
			int maxX = 3 * screenOpenCvWidth / 4;
			randomWidth = mGLSurfaceView.mRenderer.randInt(minX, maxX);

			// We only find the destination in the upper part of the frame
			int minY = (int) (0 + OpenGLBug.radius * openCVGLRatioY);
			int maxY = 2 * screenOpenCvHeight / 3;
			randomHeight = mGLSurfaceView.mRenderer.randInt(minY, maxY);

			// +1: outside the contour -1: inside the contour 0:lies on the
			// edge;
			result = ifPointIsInFloor(randomWidth, randomHeight);
		} while (result != 1);

		// Convert coordinates between opencv and opengl
		double resultDestX = (double) randomWidth / screenOpenCvWidth;
		// Revert y-axis ratio between opencv and opengl
		double resultDestY = 1 - (double) randomHeight / screenOpenCvHeight;
		Log.d(TAG, "finddesti:" + resultDestX + "," + resultDestY);
		double[] resultArray = { resultDestX, resultDestY };
		return resultArray;
	}

	/**
	 * Return if the point is in the floor's contour +1: outside the contour -1:
	 * inside the contour 0:lies on the edge;
	 * 
	 * @param openCvWidth
	 *            The width in the opencv screen
	 * @param openCvHeight
	 *            The height in the opencv screen
	 */
	public int ifPointIsInFloor(int openCvWidth, int openCvHeight) {
		org.opencv.core.Point pt = new org.opencv.core.Point(openCvWidth, openCvHeight);
		MatOfPoint2f detectedFloorContour2f = new MatOfPoint2f();
		detectedFloorContours.get(0).convertTo(detectedFloorContour2f, CvType.CV_32FC2);
		int result = (int) Imgproc.pointPolygonTest(detectedFloorContour2f, pt, false);
		return result;
	}

	/**
	 * This method is called in opencv so that we could know the location of
	 * shoes
	 */
	private void setRendererContourMassCenter() {
		List<Moments> mu = new ArrayList<Moments>(detectedShoesContours.size());
		List<OpenGLFire> mFireList = new ArrayList<OpenGLFire>(detectedShoesContours.size());
		for (int i = 0; i < detectedShoesContours.size(); i++) {
			Moments detectedMoment = Imgproc.moments(detectedShoesContours.get(i), false);
			mu.add(detectedMoment);
			double ratioX = detectedMoment.get_m10() / (detectedMoment.get_m00() * screenOpenCvWidth);
			// Reverse the y axis because opencv uses y-down-axis while opengl
			// uses y-up-axis
			double ratioY = 1 - detectedMoment.get_m01() / (detectedMoment.get_m00() * screenOpenCvHeight);
			int x = (int) ratioX * screenOpenCvWidth;
			int y = (int) ratioY * screenOpenCvHeight;
			Core.circle(mRgba, new org.opencv.core.Point(x, y), 4, redColor);
			mFireList.add(new OpenGLFire(ratioX, ratioY));
		}
		mGLSurfaceView.mRenderer.mFireList = mFireList;
	}

	/**
	 * Save/Restore States
	 */
	private void saveAndRemoveSurfaceViews() {
		mFrameLayout.removeAllViewsInLayout();
	}

	/**
	 * Restore or create SurfaceView for bugs. This method is called after
	 * OpenCV library is loaded successfully and must be called after
	 * restoreOrCreateJavaCameraView is called so that CameraView would not
	 * overlap GLSurfaceView.
	 */
	private void restoreOrCreateGLSurfaceView() {
		mGLSurfaceView = new MyGLSurfaceView(this);
		new InitRenderTask(this).execute();
	}

	/**
	 * This method is executed after init render work, genrated from
	 * restoreOrCreateGLSurfaceView, has been done.
	 */
	public void restoreOrCreateGLSurfaceView2() {
		if (mGLSurfaceView != null) {
			mGLSurfaceView.mRenderer.openGlMode = mGLSurfaceView.mRenderer.MODE_MAIN_MENU;
			mFrameLayout.addView(mGLSurfaceView);
			mGLSurfaceView.setZOrderMediaOverlay(true);
			mGLSurfaceView.setZOrderOnTop(true);
		}
	}

	/**
	 * Restore or create SurfaceView for opencv CameraView. This method is
	 * called after OpenCV library is loaded successfully and must be called
	 * before restoreOrCreateGLSurfaceView is called so that CameraView would
	 * not overlap GLSurfaceView.
	 */
	private void restoreOrCreateJavaCameraView() {
		mOpenCvCameraView = new CameraView(this);
		mOpenCvCameraView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		mOpenCvCameraView.setMaxFrameSize(800, 800);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.enableFpsMeter();
		mFrameLayout.addView(mOpenCvCameraView);
		mOpenCvCameraView.enableView();
	}

	/**
	 * Restore or create Main Menu button/title view. This method is called
	 * after clicking start button in about screen.
	 */
	private void restoreOrCreateMainMenu() {
		mMainMenuBackground = new ImageView(this);
		mMainMenuBackground.setImageResource(R.drawable.black_bg);
		mMainMenuBackground.setAlpha(0f);
		mMainMenuBackground.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mMainMenuBackground.setScaleType(ImageView.ScaleType.FIT_XY);
		mFrameLayout.addView(mMainMenuBackground);

		mMainMenuTitle = new ImageView(this);
		mMainMenuTitle.setImageResource(R.drawable.main_menu_title2);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(screenPixelWidth / 3, screenPixelWidth / 8);
		lp.setMargins(screenPixelWidth / 5, screenPixelHeight / 10, 0, 1);
		mMainMenuTitle.setLayoutParams(lp);
		mFrameLayout.addView(mMainMenuTitle);

		LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		mMainMenuButtonListView = layoutInflater.inflate(R.layout.main_menu, null);
		FrameLayout.LayoutParams lp1 = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		lp1.setMargins(0, 300, 0, 0);
		mMainMenuButtonListView.setLayoutParams(lp1);
		mFrameLayout.addView(mMainMenuButtonListView);
	}

	/**
	 * Restore or create about screen. (including adding text from strings.xml)
	 */
	private void restoreOrCreateAboutScreen() {
		LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		mAboutView = layoutInflater.inflate(R.layout.about_screen, null);
		Resources res = getResources();
		Drawable background = res.getDrawable(R.drawable.black_bg);
		background.setAlpha(200);
		mAboutView.setBackgroundDrawable(background);
		mFrameLayout.addView(mAboutView);

		mAboutText = (TextView) findViewById(R.id.about_text);
		mAboutText.setText(Html.fromHtml(getString(R.string.about_text)));
		mAboutText.setMovementMethod(LinkMovementMethod.getInstance());
		// Setups the link color
		mAboutText.setLinkTextColor(getResources().getColor(R.color.holo_light_blue));
	}

	/*
	 * Sensor methods
	 */
	private void initSensors() {
		// sensor manager
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	private void resumeSensors() {
		if (mAccelerometer != null) {
			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
		}
	}

	private void pauseSensors() {
		if (mAccelerometer != null) {
			mSensorManager.unregisterListener(this);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent arg0) {
		if (Sensor.TYPE_ACCELEROMETER == arg0.sensor.getType()) {
			// Log.d(TAG, "accelerometer z:" + arg0.values[2]);
			switch (openCvMode) {
			case MODE_COLOR_PICK_CROSSHAIR:
				if (arg0.values[2] < 8.5f) {
					mColorPickHelpNotifTextView.setVisibility(View.GONE);
					mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_horizon);
					mColorPickHelpNotifTextView.setTextColor(Color.RED);
					mColorPickHelpNotifTextView.setVisibility(View.VISIBLE);
					openCvMode = MODE_COLOR_PICK_HOLD_WRONGLY;
					mColorPickCameraButton.setEnabled(false);
				}
				break;
			case MODE_COLOR_PICK_HOLD_WRONGLY:
				if (arg0.values[2] > 8.5f) {
					mColorPickHelpNotifTextView.setVisibility(View.GONE);
					if (!shoeColorPicked) {
						mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_shoe);
					} else {
						mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_floor);
					}
					mColorPickHelpNotifTextView.setTextColor(Color.WHITE);
					mColorPickHelpNotifTextView.setVisibility(View.VISIBLE);
					openCvMode = MODE_COLOR_PICK_CROSSHAIR;
					mColorPickCameraButton.setEnabled(true);
				}
			default:
				// Do nothing
			}
		}

	}

	/** Initialize blackbackground drawable resource */
	private void initBlackBackground() {
		Resources res = getResources();
		blackBackground = res.getDrawable(R.drawable.black_bg);
	}

	/** Store screenwidth and screenheight */
	private void storeScreenDimensions() {
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		screenPixelWidth = size.x;
		screenPixelHeight = size.y;
	}

	/** Update the score and also call updateScoreUI to update the UI */
	public void updateScore(int diff) {
		score = score + diff;
		updateScoreUI();
	}

	/** Update the score UI */
	private void updateScoreUI() {

	}

}
