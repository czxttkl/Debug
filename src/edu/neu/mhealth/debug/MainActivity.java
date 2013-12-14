package edu.neu.mhealth.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.video.Video;

import edu.neu.mhealth.debug.helper.Global;
import edu.neu.mhealth.debug.helper.InitRenderTask;
import edu.neu.mhealth.debug.helper.JumpBug;
import edu.neu.mhealth.debug.helper.ModeManager;
import edu.neu.mhealth.debug.helper.Prefs;
import edu.neu.mhealth.debug.helper.ModeManager.AccEventModeManager;
import edu.neu.mhealth.debug.opencv.ColorDetector;
import edu.neu.mhealth.debug.opencv.OpticalFLowDetector;
import edu.neu.mhealth.debug.opengl.OpenGLBug;
import edu.neu.mhealth.debug.opengl.OpenGLBugManager;
import edu.neu.mhealth.debug.opengl.OpenGLFire;
import edu.neu.mhealth.debug.opengl.OpenGLRenderer;
import edu.neu.mhealth.debug.sensor.LinearAccEventListener;
import edu.neu.mhealth.debug.sensor.MotionEventListener;
import edu.neu.mhealth.debug.sensor.MovingAverage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements OnTouchListener, CvCameraViewListener2, SensorEventListener {

	// / Layout
	private FrameLayout mFrameLayout;

	// Color pick layout related
	RelativeLayout mColorPickLayout;
	View mColorPickBottomBar;
	View mColorPickInstructionsView;
	View mColorPickHelpNotif;
	View mColorPickHelpConfirmButt1;
	View mColorPickHelpConfirmButt2;
	View mColorPickCloseButton;
	public View mColorPickCameraButton;
	public TextView mColorPickHelpNotifTextView;

	// Main menu related
	ImageView mMainMenuTitle;
	View mMainMenuButtonListView;

	// About screen
	View mAboutView;
	TextView mAboutText;

	// In game layout
	TextView mScoreText;
	ImageView mSprayImageView;
	TextView mHelperTextView;
	ImageView mHelperImageView;

	Animation fadeOutAnimation6s;
	Animation fadeOutAnimation3s;
	Animation fadeInAnimation;

	public int score = 0;
	public int lastTimeUseSprayScore = 0;

	RelativeLayout mTutorial1InstructionLayout;
	RelativeLayout mTutorial2InstructionLayout;
	RelativeLayout mTutorial3InstructionLayout;
	LinearLayout mRealGameInstructionLayout;
	RelativeLayout mInGameLayout;

	// / OpenGL
	public MyGLSurfaceView mGLSurfaceView;
	public OpenGLRenderer mOpenGLRenderer;
	public Handler mHandler = new Handler();

	// / For Game Flow Control
	public boolean shoeColorPicked = false;
	private boolean floorColorPicked = false;
	public boolean rendererInited = false;

	org.opencv.core.Point crosshairHeftmost;
	org.opencv.core.Point crosshairRightmost;
	org.opencv.core.Point crosshairUpmost;
	org.opencv.core.Point crosshairDownmost;
	org.opencv.core.Point destinationTarget;
	private Scalar mShoeColorPickRgba;
	private Scalar mShoeColorPickHsv;
	private Scalar mFloorColorPickRgba;
	private Scalar mFloorColorPickHsv;

	// / For color detection
	Mat colorPickAreaHsv;
	private Mat mRgba;
	private Mat mGray;

	private CameraView mOpenCvCameraView;
	private JumpBug jumpBug;

	public int screenOpenGLWidth;
	public int screenOpenGLHeight;

	public int imageOpenCvWidth;
	public int imageOpenCvHeight;

	public double openCVGLRatioX;
	public double openCVGLRatioY;

	private Scalar colorRed;
	private Scalar colorBlue;
	private Scalar colorGray;

	Rect colorPickAreaShoe;
	Rect colorPickAreaFloor;
	ColorDetector mColorBlobDetector;
	OpticalFLowDetector mOpticalFLowDetector;
	List<MatOfPoint> detectedShoesContours;
	List<MatOfPoint> detectedFloorContours;

	// / For jumping detection and tilt detection
	private SensorManager sensorManager;
	private Sensor sensorLinearAcc;
	private Sensor sensorAcc;
	private LinearAccEventListener linearAccEventListener;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(Global.APP_LOG_TAG, "OpenCV loaded successfully");
				restoreOrCreateJavaCameraView();
				restoreOrCreateGLSurfaceView();
				restoreOrCreateAboutScreen();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(Global.APP_LOG_TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		screenOpenGLHeight = metrics.heightPixels;
		screenOpenGLWidth = metrics.widthPixels;

		mFrameLayout = new FrameLayout(this);
		setContentView(mFrameLayout);

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		OpenGLBugManager.getOpenGLBugManager().setCameraActivityInstance(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		mFrameLayout.removeAllViews();
		rendererInited = false;

		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();

		sensorManager.unregisterListener(linearAccEventListener);
		ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_INITIAL);
		ModeManager.AccEventModeManager.getAccEventModeManager().setCurrentMode(AccEventModeManager.MODE_DEFAULT);

		if (mOpenGLRenderer != null) {
			if (mOpenGLRenderer.mFireList != null) {
				mOpenGLRenderer.mFireList.clear();
			}
		}

		cleanResource();
	}

	@Override
	public void onResume() {
		super.onResume();
		initResource();
		// jumpBug = new JumpBug(this);
		sensorLinearAcc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		linearAccEventListener = new LinearAccEventListener(this);
		sensorManager.registerListener(linearAccEventListener, sensorLinearAcc, SensorManager.SENSOR_DELAY_FASTEST);
		linearAccEventListener.addObserver(OpenGLBugManager.getOpenGLBugManager());

		sensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, sensorAcc, SensorManager.SENSOR_DELAY_FASTEST);

		// ModeManager.getModeManager().addObserver(jumpBug);
		ModeManager.getModeManager().addObserver(OpenGLBugManager.getOpenGLBugManager());

		// OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
		if (OpenCVLoader.initDebug()) {
			Log.i(Global.APP_LOG_TAG, "OpenCV loaded successfully");
			restoreOrCreateJavaCameraView();
			restoreOrCreateGLSurfaceView();
			restoreOrCreateAboutScreen();
		}
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
			mOpenCvCameraView = null;
		}
		OpenGLBugManager.finish();
		mGLSurfaceView = null;
		mOpenGLRenderer = null;
	}

	private void cleanResource() {
		mColorPickBottomBar = null;
		mColorPickInstructionsView = null;
		mColorPickHelpNotif = null;
		mColorPickHelpConfirmButt1 = null;
		mColorPickHelpConfirmButt2 = null;
		mColorPickCloseButton = null;
		mColorPickCameraButton = null;
		mColorPickHelpNotifTextView = null;
		mColorPickLayout = null;
		mMainMenuTitle = null;
		mMainMenuButtonListView = null;
		mAboutView = null;
		mAboutText = null;
		mScoreText = null;
		mSprayImageView = null;

		fadeOutAnimation6s = null;
		fadeOutAnimation3s = null;
		fadeInAnimation = null;

		mTutorial1InstructionLayout = null;
		mTutorial2InstructionLayout = null;
		mTutorial3InstructionLayout = null;
		mRealGameInstructionLayout = null;
		mInGameLayout = null;

		mHandler = null;
	}

	public void onCameraViewStarted(int width, int height) {

		imageOpenCvWidth = width;
		imageOpenCvHeight = height;

		openCVGLRatioX = (double) imageOpenCvWidth / screenOpenGLWidth;
		openCVGLRatioY = (double) imageOpenCvHeight / screenOpenGLHeight;

		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mGray = new Mat(height, width, CvType.CV_8UC1);

		colorRed = new Scalar(255, 0, 0);
		colorBlue = new Scalar(51, 181, 229);
		colorGray = new Scalar(192, 192, 192);

		colorPickAreaHsv = new Mat();
		crosshairHeftmost = new org.opencv.core.Point(width / 2 - 50, height / 2);
		crosshairRightmost = new org.opencv.core.Point(width / 2 + 50, height / 2);
		crosshairUpmost = new org.opencv.core.Point(width / 2, height / 2 - 50);
		crosshairDownmost = new org.opencv.core.Point(width / 2, height / 2 + 50);

		colorPickAreaShoe = new Rect();
		colorPickAreaShoe.x = width / 2 - 5;
		colorPickAreaShoe.y = height / 2 - 5;
		colorPickAreaShoe.width = 10;
		colorPickAreaShoe.height = 10;

		colorPickAreaFloor = new Rect();
		colorPickAreaFloor.x = width / 2 - 75;
		colorPickAreaFloor.y = height / 2 - 75;
		colorPickAreaFloor.width = 150;
		colorPickAreaFloor.height = 150;

		mColorBlobDetector = new ColorDetector(width, height);
		mOpticalFLowDetector = new OpticalFLowDetector(width, height);
	}

	public void onCameraViewStopped() {
		mRgba.release();
		mGray.release();
		colorPickAreaHsv.release();

		mColorBlobDetector = null;
		mOpticalFLowDetector = null;
		colorRed = null;
		colorBlue = null;
		colorGray = null;
		detectedShoesContours = null;
		detectedFloorContours = null;
		colorPickAreaShoe = null;
		colorPickAreaFloor = null;
		crosshairHeftmost = null;
		crosshairRightmost = null;
		crosshairUpmost = null;
		crosshairDownmost = null;
		destinationTarget = null;
	}

	public boolean onTouch(View v, MotionEvent event) {

		Log.e(Global.APP_LOG_TAG, "touch coordinate: " + event.getX() + "  " + event.getY());

		// int x = configureView.getFloorPosition().x;
		// int y = configureView.getFloorPosition().y;

		return false;
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();

		int gameMode = ModeManager.getModeManager().getCurrentMode();
		switch (gameMode) {
		case ModeManager.MODE_COLOR_PICK_CROSSHAIR:

			// Pick shoes' color
			if (!shoeColorPicked) {
				Mat colorPickAreaRgba = mRgba.submat(colorPickAreaShoe);
				Imgproc.cvtColor(colorPickAreaRgba, colorPickAreaHsv, Imgproc.COLOR_RGB2HSV_FULL);
				int pointCount = colorPickAreaShoe.width * colorPickAreaShoe.height;

				// Calculate average color of shoes' color pick region
				mShoeColorPickHsv = Core.sumElems(colorPickAreaHsv);
				for (int i = 0; i < mShoeColorPickHsv.val.length; i++)
					mShoeColorPickHsv.val[i] /= pointCount;
				mShoeColorPickRgba = converScalarHsv2Rgba(mShoeColorPickHsv);
				Mat colorLabel = mRgba.submat(44, 108, 8, 72);
				colorLabel.setTo(mShoeColorPickRgba);
			} else {
				Mat colorPickAreaRgba = mRgba.submat(colorPickAreaFloor);
				Imgproc.cvtColor(colorPickAreaRgba, colorPickAreaHsv, Imgproc.COLOR_RGB2HSV_FULL);
				int pointCount = colorPickAreaFloor.width * colorPickAreaFloor.height;

				mFloorColorPickHsv = Core.sumElems(colorPickAreaHsv);
				for (int i = 0; i < mFloorColorPickHsv.val.length; i++)
					mFloorColorPickHsv.val[i] /= pointCount;
				mFloorColorPickRgba = converScalarHsv2Rgba(mFloorColorPickHsv);
				Mat colorLabel = mRgba.submat(44, 108, 8, 72);
				colorLabel.setTo(mFloorColorPickRgba);
			}
			Core.line(mRgba, crosshairHeftmost, crosshairRightmost, colorBlue, 10);
			Core.line(mRgba, crosshairUpmost, crosshairDownmost, colorBlue, 10);
			break;

		case ModeManager.MODE_SHOE_COLOR_PICKED:
			mColorBlobDetector.process(mRgba, false, true);
			detectedShoesContours = mColorBlobDetector.getShoesContours();
			// Log.e(TAG, "Contours count: " + contours.size());
			if (detectedShoesContours == null)
				return mRgba;
			detectedShoesContours.removeAll(Collections.singleton(null));
			Imgproc.drawContours(mRgba, detectedShoesContours, -1, colorRed, 6);
			break;

		case ModeManager.MODE_COLOR_PICK_HOLD_WRONGLY:
			Core.line(mRgba, crosshairHeftmost, crosshairRightmost, colorGray, 10);
			Core.line(mRgba, crosshairUpmost, crosshairDownmost, colorGray, 10);
			break;

		case ModeManager.MODE_FLOOR_COLOR_PICKED:
			mColorBlobDetector.process(mRgba, true, false);
			detectedFloorContours = mColorBlobDetector.getFloorContours();
			if (detectedFloorContours == null)
				return mRgba;
			detectedFloorContours.removeAll(Collections.singleton(null));
			Imgproc.drawContours(mRgba, detectedFloorContours, -1, colorRed, 4);
			break;

		case ModeManager.MODE_TUTORIAL_1:
		case ModeManager.MODE_TUTORIAL_2:
		case ModeManager.MODE_REAL_GAME:
			mColorBlobDetector.process(mRgba, true, true);
			detectedShoesContours = mColorBlobDetector.getShoesContours();
			detectedShoesContours.removeAll(Collections.singleton(null));
			setRendererContourMassCenter();

			detectedFloorContours = mColorBlobDetector.getFloorContours();
			if (detectedFloorContours == null)
				return mRgba;
			detectedFloorContours.removeAll(Collections.singleton(null));
			// Imgproc.drawContours(mRgba, detectedFloorContours, -1, colorRed, 4);
			// if (destinationTarget != null)
			// Core.circle(mRgba, destinationTarget, 10, colorRed, -1);

			mOpticalFLowDetector.process(mRgba);

			break;

		case ModeManager.MODE_BEFORE_TUTORIAL_1:
			mColorBlobDetector.process(mRgba, false, true);
			detectedShoesContours = mColorBlobDetector.getShoesContours();
			// if (detectedShoesContours == null)
			// return mRgba;
			detectedShoesContours.removeAll(Collections.singleton(null));
			setRendererContourMassCenter();

		default:
			break;
		}
		//

		return mRgba;
	}

	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

		return new Scalar(pointMatRgba.get(0, 0));
	}

	/** Initialize blackbackground drawable resource */
	private void initResource() {
		fadeOutAnimation6s = AnimationUtils.loadAnimation(this, R.anim.fade_out_6);
		fadeOutAnimation6s.setFillAfter(true);
		fadeOutAnimation3s = AnimationUtils.loadAnimation(this, R.anim.fade_out_3);
		fadeOutAnimation3s.setFillAfter(true);
		fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		fadeInAnimation.setFillAfter(true);
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

		mColorPickLayout.setBackgroundColor(getResources().getColor(R.color.transparent));

		// Set opencvmode to crosshair mode, so opencv will draw a crosshair in
		// the center of image
		ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_COLOR_PICK_CROSSHAIR);
		ModeManager.AccEventModeManager.getAccEventModeManager().setCurrentMode(AccEventModeManager.MODE_COLOR_PICK_CROSSHAIR);
	}

	/**
	 * Restore or create SurfaceView for bugs. This method is called after OpenCV library is loaded successfully and must be called after restoreOrCreateJavaCameraView is called so that CameraView
	 * would not overlap GLSurfaceView.
	 */
	private void restoreOrCreateGLSurfaceView() {
		if (mGLSurfaceView == null) {
			mGLSurfaceView = new MyGLSurfaceView(getApplicationContext());
			new InitRenderTask(this).execute();
		} else {
			mFrameLayout.addView(mGLSurfaceView);
			ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_MAIN_MENU);
			mGLSurfaceView.setZOrderMediaOverlay(true);
			mGLSurfaceView.setZOrderOnTop(true);
			rendererInited = true;
		}
	}

	/**
	 * This method is executed after init render work, genrated from restoreOrCreateGLSurfaceView, has been done.
	 */
	public void restoreOrCreateGLSurfaceView2() {
		if (mGLSurfaceView != null) {
			mOpenGLRenderer = mGLSurfaceView.mRenderer;
			rendererInited = true;
			ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_MAIN_MENU);
			mFrameLayout.addView(mGLSurfaceView);
			mGLSurfaceView.setZOrderMediaOverlay(true);
			mGLSurfaceView.setZOrderOnTop(true);
		}
	}

	/**
	 * Restore or create SurfaceView for opencv CameraView. This method is called after OpenCV library is loaded successfully and must be called before restoreOrCreateGLSurfaceView is called so that
	 * CameraView would not overlap GLSurfaceView.
	 */
	private void restoreOrCreateJavaCameraView() {
		if (mOpenCvCameraView == null) {
			mOpenCvCameraView = new CameraView(getApplicationContext());
			mOpenCvCameraView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			// mOpenCvCameraView.setMaxFrameSize(500, 500);
			mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
			mOpenCvCameraView.setCvCameraViewListener(this);
			// mOpenCvCameraView.enableFpsMeter();
			mFrameLayout.addView(mOpenCvCameraView);
			mOpenCvCameraView.enableView();
		} else {
			mFrameLayout.addView(mOpenCvCameraView);
			mOpenCvCameraView.enableView();
		}
	}

	/**
	 * Restore or create Main Menu button/title view. This method is called after clicking start button in about screen.
	 */
	private void restoreOrCreateMainMenu() {
		mMainMenuTitle = new ImageView(this);
		mMainMenuTitle.setImageResource(R.drawable.main_menu_title2);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(screenOpenGLWidth / 3, screenOpenGLWidth / 8);
		lp.setMargins(screenOpenGLWidth / 5, screenOpenGLHeight / 10, 0, 1);
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
		if (Prefs.getFirstTimePlay(getApplicationContext())) {
			LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
			mAboutView = layoutInflater.inflate(R.layout.about_screen, null);
			mFrameLayout.addView(mAboutView);

			mAboutText = (TextView) findViewById(R.id.about_text);
			mAboutText.setText(Html.fromHtml(getString(R.string.about_text_debug)));
			mAboutText.setMovementMethod(LinkMovementMethod.getInstance());
			mAboutText.setLinkTextColor(getResources().getColor(R.color.holo_light_blue));

			Prefs.setFirstTimePlay(getApplicationContext(), false);
		} else {
			restoreOrCreateMainMenu();
		}
	}

	public double[] findBugNextDest() {
		int randomWidth;
		int randomHeight;
		int result;
		do {
			// We assume that the screenPixelWidth is the same with the opengl
			// coordinate
			int minX = imageOpenCvWidth / 4;
			int maxX = 3 * imageOpenCvWidth / 4;
			randomWidth = Global.randInt(minX, maxX);

			// We only find the destination in the upper part of the frame
			int minY = (int) (0 + OpenGLBug.radius * openCVGLRatioY);
			int maxY = 2 * imageOpenCvHeight / 3;
			randomHeight = Global.randInt(minY, maxY);

			// +1: outside the contour -1: inside the contour 0:lies on the
			// edge;
			result = isPointInFloor(randomWidth, randomHeight);
		} while (result != 1);

		destinationTarget = new org.opencv.core.Point(randomWidth, randomHeight);
		// Convert coordinates between opencv and opengl
		double resultDestX = (double) randomWidth / imageOpenCvWidth;
		// Revert y-axis ratio between opencv and opengl
		double resultDestY = 1 - (double) randomHeight / imageOpenCvHeight;
		double[] resultArray = { resultDestX, resultDestY };
		return resultArray;
	}

	public int isPointInFloor(int openCvWidth, int openCvHeight) {
		return isPointInFloor(openCvWidth, openCvHeight, false);
	}

	public int isPointInFloor(int openCvWidth, int openCvHeight, boolean calculateDist) {
		org.opencv.core.Point pt = new org.opencv.core.Point(openCvWidth, openCvHeight);
		MatOfPoint2f detectedFloorContour2f = new MatOfPoint2f();
		if ((detectedFloorContours!= null) && (detectedFloorContours.size() > 0)) {
			detectedFloorContours.get(0).convertTo(detectedFloorContour2f, CvType.CV_32FC2);
		}
		int result = (int) Imgproc.pointPolygonTest(detectedFloorContour2f, pt, calculateDist);
		return result;
	}

	public void addScore(int diff) {
		score = score + diff;
		// Log.d(Global.APP_LOG_TAG, "update score:" + score);
		// Log.d(Global.APP_LOG_TAG, "gamescorelayout height:" + mGameScoreLayout.getHeight());

		updateScoreUI();
		updateSpray();
		int gameMode = ModeManager.getModeManager().getCurrentMode();
		switch (gameMode) {
		case ModeManager.MODE_TUTORIAL_1:
			if (score == 5) {
				mHandler.postDelayed(mTutorial2InstructionUpdator, 1000);
			}
			break;

		case ModeManager.MODE_TUTORIAL_2:
			break;
		default:
			break;
		}

	}

	private void setRendererContourMassCenter() {
		if (ModeManager.getModeManager().getCurrentMode() != ModeManager.MODE_INITIAL) {
			List<Moments> mu = new ArrayList<Moments>(detectedShoesContours.size());
			List<OpenGLFire> mFireList = new ArrayList<OpenGLFire>(detectedShoesContours.size());
			for (int i = 0; i < detectedShoesContours.size(); i++) {
				Moments detectedMoment = Imgproc.moments(detectedShoesContours.get(i), false);
				mu.add(detectedMoment);
				double ratioX = detectedMoment.get_m10() / (detectedMoment.get_m00() * imageOpenCvWidth);
				// Reverse the y axis because opencv uses y-down-axis while
				// opengl
				// uses y-up-axis
				double ratioY = 1 - detectedMoment.get_m01() / (detectedMoment.get_m00() * imageOpenCvHeight);
				int x = (int) ratioX * imageOpenCvWidth;
				int y = (int) ratioY * imageOpenCvHeight;
				Core.circle(mRgba, new org.opencv.core.Point(x, y), 4, colorRed);
				mFireList.add(new OpenGLFire(ratioX, ratioY));
			}
			mOpenGLRenderer.mFireList = mFireList;
		}
	}

	public void onClickAboutStartButton(View v) {
		mFrameLayout.removeView(mAboutView);
		restoreOrCreateMainMenu();
	}

	private void updateScoreUI() {
		mScoreText.setText(String.valueOf(score));
	}

	private void updateSpray() {
		if (score - lastTimeUseSprayScore == 5) {
			lastTimeUseSprayScore = score;
			mSprayImageView.setAnimation(fadeInAnimation);
			mSprayImageView.setEnabled(true);

			mSprayImageView.setAlpha(1f);
		}
	}

	public void onClickMainMenuStartGame(View v) {
		if (!rendererInited)
			return;
		mFrameLayout.removeView(mMainMenuTitle);
		mFrameLayout.removeView(mMainMenuButtonListView);

		// OpenGL shouldn't render anything right after clicking start game.
		ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_INITIAL);

		// Inflates the Overlay Layout to be displayed above the Camera View
		LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		mColorPickLayout = (RelativeLayout) layoutInflater.inflate(R.layout.color_pick_overlay, null, false);
		mColorPickLayout.setBackgroundColor(getResources().getColor(R.color.pointeight_black));

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
	 * Button Camera clicked. Marker's color is picked after this method is called.
	 */
	public void onClickColorPickCameraButton(View v) {
		if (!shoeColorPicked) {
			shoeColorPicked = true;
			mColorBlobDetector.setShoeHsvColor(mShoeColorPickHsv);
			ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_SHOE_COLOR_PICKED);
			mColorPickHelpNotifTextView.setVisibility(View.GONE);
			mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_confirm_shoe);
			mColorPickHelpNotifTextView.setTextColor(Color.WHITE);
			mColorPickHelpNotifTextView.setVisibility(View.VISIBLE);
			mColorPickHelpConfirmButt1.setVisibility(View.VISIBLE);
			mColorPickBottomBar.setVisibility(View.GONE);
		} else {
			floorColorPicked = true;
			mColorBlobDetector.setFloorHsvColor(mFloorColorPickHsv);
			ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_FLOOR_COLOR_PICKED);
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
		mColorPickLayout.setBackgroundColor(getResources().getColor(R.color.pointeight_black));

		// Goes back to the Color Pick Add rMode
		initializeInstructionMode();

		// Set mode to default
		ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_INITIAL);

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
		ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_MAIN_MENU);
	}

	/** Shoe color pick confirm cancel button clicked */
	public void onClickShoeColorPickConfirmCancel(View v) {
		mColorPickBottomBar.setVisibility(View.VISIBLE);
		mColorPickHelpNotifTextView.setVisibility(View.GONE);
		mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_shoe);
		mColorPickHelpNotifTextView.setTextColor(Color.WHITE);
		mColorPickHelpNotifTextView.setVisibility(View.VISIBLE);
		mColorPickHelpConfirmButt1.setVisibility(View.GONE);
		ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_COLOR_PICK_CROSSHAIR);
		ModeManager.AccEventModeManager.getAccEventModeManager().setCurrentMode(AccEventModeManager.MODE_COLOR_PICK_CROSSHAIR);

		// Set not picked shoes' color yet
		shoeColorPicked = false;
	}

	/** Shoe color pick confirm ok button clicked */
	public void onClickShoeColorPickConfirmOk(View v) {
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
		ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_COLOR_PICK_CROSSHAIR);
		ModeManager.AccEventModeManager.getAccEventModeManager().setCurrentMode(AccEventModeManager.MODE_COLOR_PICK_CROSSHAIR);
	}

	/** Floor color pick confirm ok button clicked */
	public void onClickFloorColorPickConfirmOk(View v) {
		mFrameLayout.removeView(mColorPickLayout);
		ModeManager.AccEventModeManager.getAccEventModeManager().setCurrentMode(AccEventModeManager.MODE_DEFAULT);
		// Inflates the game score layout
		LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		mInGameLayout = (RelativeLayout) layoutInflater.inflate(R.layout.in_game, null, false);
		mFrameLayout.addView(mInGameLayout);
		mScoreText = (TextView) findViewById(R.id.score_text);
		mSprayImageView = (ImageView) findViewById(R.id.spray);
		mHelperImageView = (ImageView) findViewById(R.id.helper_image);
		mHelperTextView = (TextView) findViewById(R.id.helper_text);

		if (Prefs.getTutorialed(getApplicationContext())) {
			ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_REAL_GAME);
		} else {
			ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_BEFORE_TUTORIAL_1);
			mHandler.postDelayed(mTutorial1InstructionUpdator, 2000);
		}

	}

	public void onClickTutorial1InstructionOk(View v) {
		mFrameLayout.removeView(mTutorial1InstructionLayout);
		ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_TUTORIAL_1);
		mTutorial1InstructionLayout = null;
	}

	public void onClickTutorial2InstructionOk(View v) {
		mFrameLayout.removeView(mTutorial2InstructionLayout);
		ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_TUTORIAL_2);
		mTutorial2InstructionLayout = null;
	}

	public void onClickSpray(View v) {
		mSprayImageView.startAnimation(fadeOutAnimation6s);
		mSprayImageView.setEnabled(false);
		ModeManager.AccEventModeManager.getAccEventModeManager().setCurrentMode(AccEventModeManager.MODE_SPRAY_SHAKE);
		mHelperImageView.setImageResource(R.drawable.shake_helper);
		mHelperTextView.setText(R.string.shake_help_text);
		mHelperImageView.setAlpha(1f);
		mHelperTextView.setAlpha(1f);
		mHelperTextView.startAnimation(fadeOutAnimation3s);
		mHelperImageView.startAnimation(fadeOutAnimation3s);
		mHandler.postDelayed(mShakeHelperRemoverRunnable, 3000);
	}

	protected Runnable mShakeHelperRemoverRunnable = new Runnable() {
		@Override
		public void run() {
			mHandler.postDelayed(mJumpHelperRunnable, 300);
		}
	};

	protected Runnable mJumpHelperRunnable = new Runnable() {
		@Override
		public void run() {
			ModeManager.AccEventModeManager.getAccEventModeManager().setCurrentMode(AccEventModeManager.MODE_SPRAY_JUMP);
			mHelperImageView.setImageResource(R.drawable.jump_helper);
			mHelperTextView.setText(R.string.jump_help_text);
			mHelperTextView.startAnimation(fadeOutAnimation3s);
			mHelperImageView.startAnimation(fadeOutAnimation3s);
			mHandler.postDelayed(mJumpHelperRemoverRunnable, 3000);
		}
	};

	protected Runnable mJumpHelperRemoverRunnable = new Runnable() {
		@Override
		public void run() {
			ModeManager.AccEventModeManager.getAccEventModeManager().setCurrentMode(AccEventModeManager.MODE_DEFAULT);
			OpenGLBugManager.getOpenGLBugManager().unfreezeBugs();
		}
	};

	protected Runnable mTutorial1InstructionUpdator = new Runnable() {
		@Override
		public void run() {
			// Inflates the Overlay Layout to be displayed above the Camera View
			LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
			mTutorial1InstructionLayout = (RelativeLayout) layoutInflater.inflate(R.layout.tutorial1_instruction, null, false);

			mFrameLayout.addView(mTutorial1InstructionLayout);

			// Set mode to default
			ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_INITIAL);
			// Clear fire list to prevent fire rendering
			mOpenGLRenderer.mFireList.clear();
		}
	};

	protected Runnable mTutorial2InstructionUpdator = new Runnable() {
		@Override
		public void run() {
			// Inflates the Overlay Layout to be displayed above the Camera View
			LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
			mTutorial2InstructionLayout = (RelativeLayout) layoutInflater.inflate(R.layout.tutorial2_instruction, null, false);

			mFrameLayout.addView(mTutorial2InstructionLayout);

			// Set mode to default
			ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_INITIAL);
			// Clear fire list to prevent fire rendering
			mOpenGLRenderer.mFireList.clear();
		}
	};

	public Runnable mStartRealGame = new Runnable() {

		@Override
		public void run() {
			Prefs.setTutorialed(getApplicationContext(), true);
			// LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
			// mRealGameInstructionLayout = (LinearLayout) layoutInflater.inflate(R.layout.real_game_instruction, null, false);
			//
			// mFrameLayout.addView(mRealGameInstructionLayout);
			// fadeOutAnimation.setDuration(3000);
			// mRealGameInstructionLayout.startAnimation(fadeOutAnimation);
			// mHandler.postDelayed(mRealGameRemoverRunnable, 3000);

		}

	};

	public Runnable mRealGameRemoverRunnable = new Runnable() {

		@Override
		public void run() {
			// mFrameLayout.removeView(mRealGameInstructionLayout);
			// mRealGameInstructionLayout = null;
		}

	};

	private long lastShakeUpdateTime = -1;
	private float lastX = -1.0f, lastY = -1.0f, lastZ = -1.0f;

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (Sensor.TYPE_ACCELEROMETER == event.sensor.getType()) {
			// Log.d(TAG, "accelerometer z:" + arg0.values[2]);
			if (mColorPickHelpNotifTextView == null)
				return;
			int gameMode = ModeManager.AccEventModeManager.getAccEventModeManager().getCurrentMode();
			switch (gameMode) {
			case AccEventModeManager.MODE_COLOR_PICK_CROSSHAIR:
				if (event.values[2] < 8.5f) {
					mColorPickHelpNotifTextView.setVisibility(View.GONE);
					mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_horizon);
					mColorPickHelpNotifTextView.setTextColor(Color.RED);
					mColorPickHelpNotifTextView.setVisibility(View.VISIBLE);
					ModeManager.AccEventModeManager.getAccEventModeManager().setCurrentMode(AccEventModeManager.MODE_COLOR_PICK_HOLD_WRONGLY);
					ModeManager.getModeManager().setCurrentMode(ModeManager.MODE_COLOR_PICK_HOLD_WRONGLY);
					mColorPickCameraButton.setEnabled(false);
				}
				break;

			case AccEventModeManager.MODE_COLOR_PICK_HOLD_WRONGLY:
				if (event.values[2] > 8.5f) {
					mColorPickHelpNotifTextView.setVisibility(View.GONE);
					if (!shoeColorPicked) {
						mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_shoe);
					} else {
						mColorPickHelpNotifTextView.setText(R.string.color_pick_help_notif_floor);
					}
					mColorPickHelpNotifTextView.setTextColor(Color.WHITE);
					mColorPickHelpNotifTextView.setVisibility(View.VISIBLE);
					ModeManager.AccEventModeManager.getAccEventModeManager().setCurrentMode(AccEventModeManager.MODE_COLOR_PICK_CROSSHAIR);
					ModeManager.getModeManager().setCurrentMode(ModeManager.getModeManager().getPreviousMode());
					mColorPickCameraButton.setEnabled(true);
				}

			case AccEventModeManager.MODE_SPRAY_SHAKE:
				if (lastShakeUpdateTime == -1) {
					lastShakeUpdateTime = System.currentTimeMillis();
					float[] xyz = event.values;
					float x = xyz[0];
					float y = xyz[1];
					float z = xyz[2];
					lastX = x;
					lastY = y;
					lastZ = z;
					return;
				} else {
					long currentTime = System.currentTimeMillis();
					if (currentTime - lastShakeUpdateTime > 500) {
						long diffTime = currentTime - lastShakeUpdateTime;
						float[] xyz = event.values;
						float x = xyz[0];
						float y = xyz[1];
						float z = xyz[2];
						float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000;
						// Log.d("shuffleboard", "shaked:" + speed + " x:" + x + " y:" + y + " z:" + z + " diffTime:" + diffTime);
						if (speed > 100) {
							Log.d(Global.APP_LOG_TAG, "one shake");
							OpenGLBugManager.getOpenGLBugManager().freezeBug();
						}
						lastX = x;
						lastY = y;
						lastZ = z;
						lastShakeUpdateTime = currentTime;
					}
				}
				break;

			default:
				// Do nothing
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

}
