package edu.neu.mhealth.debug;

import java.util.Arrays;
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
import org.opencv.video.Video;

import edu.neu.mhealth.debug.helper.AccEventListener;
import edu.neu.mhealth.debug.helper.Bug;
import edu.neu.mhealth.debug.helper.BugManager;
import edu.neu.mhealth.debug.helper.JumpBug;
import edu.neu.mhealth.debug.helper.MotionEventListener;
import edu.neu.mhealth.debug.helper.MovingAverage;

import android.app.Activity;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class MainActivity extends Activity implements OnTouchListener,
		CvCameraViewListener2 {
	private static final String TAG = "OCVSample::Activity";
	
	/// OpenCV basic
	private List<android.hardware.Camera.Size> resolutions;
	/// For color detection
	private boolean mIsColorSelected = false;
	private Mat mRgba;
	private Mat mGray;
	private Mat tempGray;
	private Scalar mBlobColorRgba;
	private Scalar mBlobColorHsv;
	private ColorDetector mDetector;
	private Mat mSpectrum;
	private Size SPECTRUM_SIZE;
	private Scalar CONTOUR_COLOR;

	private CameraView mOpenCvCameraView;
	private ConfigureView configureView;
	private BugManager bugManager;
	private Bug bug1;
	private JumpBug jumpBug;
	private int initalX = 0;
	private int initalY = 0;
	private int steps = 0;
	private int xOffset = 0;
	private int yOffset = 0;

	private int screenWidth;
	private int screenHeight;

	private int imageWidth;
	private int imageHeight;
	
	private Scalar colorGreen;
	private Scalar colorRed;
	private Scalar colorBlue;
	private Scalar colorWhite;

	/// For optical flow

	private List<MatOfPoint> contourFloor;
	private List<MatOfPoint> contourShoe;


	private Mat mOpFlowCurr;
	private Mat mOpFlowPrev;
	private MatOfPoint mMOPopFlowCurr;
	private MatOfPoint mMOPopFlowPrev;
	private MatOfPoint2f mMOP2PtsCurr;
	private MatOfPoint2f mMOP2PtsPrev;
	private MatOfPoint2f mMOP2PtsSafe;
	private List<Point> cornersPrev;
	private List<Point> cornersCurr;
	private MatOfByte status;
	private MatOfFloat err;
	private List<Byte> byteStatus;
	private MovingAverage filterX;
	private MovingAverage filterY;


	Mat optFlowMatRgba;
	Mat optFlowMatGray;

	private static final int rectWidth = 500;
	private static final int rectHeight = 250;
	private static final int squareMetric = 200;
	
	private static final int motionThX = 100;
	private static final int motionThY = 100;
	
	/// For jumping detection
	private SensorManager sensorManager;
	private Sensor sensorACC;
	private AccEventListener accEventListener;
	
	/// For direction / motion detection
	private MotionEventListener motionEventListener;
	

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				mOpenCvCameraView.setOnTouchListener(MainActivity.this);
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public MainActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		screenHeight = metrics.heightPixels;
		screenWidth = metrics.widthPixels;

		FrameLayout layout = new FrameLayout(this);

		mOpenCvCameraView = new CameraView(this);
		mOpenCvCameraView.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.enableFpsMeter();
		
		configureView = new ConfigureView(this, screenWidth, screenHeight);

		bug1 = new Bug(this, 20, 30);
		bug1.setImageDrawable(getResources().getDrawable(R.drawable.bug));

		bugManager = BugManager.getBugManager();
		bugManager.addBug(bug1);

		layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		layout.addView(mOpenCvCameraView);
		layout.addView(configureView);
		// layout.addView(bug1);

		setContentView(layout);
		filterX = new MovingAverage(5);
		filterY = new MovingAverage(5);

		Log.e(TAG, "screen size: " + screenHeight + " " + screenWidth);
		Log.e(TAG, "camera size: " + mOpenCvCameraView.getHeight() + " " + mOpenCvCameraView.getWidth());
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
		sensorManager.unregisterListener(accEventListener);
	}

	@Override
	public void onResume() {
		super.onResume();
		
		sensorACC = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		accEventListener = new AccEventListener(this.getApplicationContext());
		jumpBug = new JumpBug(getApplicationContext());
		accEventListener.addObserver(jumpBug);
		
		motionEventListener = new MotionEventListener();
		motionEventListener.addObserver(jumpBug);
		
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mGray = new Mat(height, width, CvType.CV_8UC1);
		mSpectrum = new Mat();
		mBlobColorRgba = new Scalar(255);
		mBlobColorHsv = new Scalar(255);
		SPECTRUM_SIZE = new Size(200, 64);
		CONTOUR_COLOR = new Scalar(255, 0, 0, 255);

		mOpFlowCurr = new Mat();
		mOpFlowPrev = new Mat();
		optFlowMatRgba = new Mat();
		optFlowMatGray = new Mat();
		mMOPopFlowPrev = new MatOfPoint();
		mMOPopFlowCurr = new MatOfPoint();
		mMOP2PtsCurr = new MatOfPoint2f();
		mMOP2PtsPrev = new MatOfPoint2f();
		mMOP2PtsSafe = new MatOfPoint2f();
		status = new MatOfByte();
		err = new MatOfFloat();

		colorRed = new Scalar(255, 0, 0);
		colorGreen = new Scalar(0, 255, 0);
		colorBlue = new Scalar(0, 0, 255);
		colorWhite = new Scalar(255, 255, 255);
		
		resolutions = mOpenCvCameraView.getResolutionList();
//		for (android.hardware.Camera.Size resolution : resolutions) {
//			Log.i(TAG, "supported size: " + resolution.width + "  " + resolution.height);
//		}
		mOpenCvCameraView.setResolution(resolutions.get(5));
		
	}

	public void onCameraViewStopped() {
		mRgba.release();
		mGray.release();
	}

	public boolean onTouch(View v, MotionEvent event) {

		Log.e(TAG, "touch coordinate: " + event.getX() + "  " + event.getY());

		int x = configureView.getFloorPosition().x;
		int y = configureView.getFloorPosition().y;

		mIsColorSelected = true;

		return false; 
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();
	
		imageWidth = mRgba.cols();
		imageHeight = mRgba.rows();

		xOffset = (mOpenCvCameraView.getWidth() - imageWidth) / 2;
		yOffset = (mOpenCvCameraView.getHeight() - imageHeight) / 2;

		bug1.setLimit(mOpenCvCameraView.getWidth(),
				mOpenCvCameraView.getHeight());
		bug1.setOffset(xOffset, yOffset);
		
//		Log.i(TAG, "viewport size: " + mOpenCvCameraView.getWidth() + " " + mOpenCvCameraView.getHeight());
//		Log.i(TAG, "Image size : " + mRgba.rows() + " "  + mRgba.cols());

		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				configureView.invalidate();
			}
		});
		
		sensorManager.registerListener(accEventListener, sensorACC, SensorManager.SENSOR_DELAY_FASTEST);	

		if (mIsColorSelected) {
	
			int x = configureView.getFloorPosition().x - xOffset;
			int y = configureView.getFloorPosition().y - yOffset;
			Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");
			contourFloor = findObjectAt(x, y);
			Imgproc.drawContours(mRgba, contourFloor, -1, CONTOUR_COLOR);

			x = configureView.getShoesPosition().x - xOffset;
			y = configureView.getShoesPosition().y - yOffset;

			contourShoe = findObjectAt(x, y);
			Imgproc.drawContours(mRgba, contourShoe, -1, CONTOUR_COLOR);

			configureView.disableDrawing();

			steps++;
			if (steps > 10) {
				this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						bugManager.moveBugs();
					}
				});

				steps = 0;
			}

			bugShoeCollisionCheck();
		}

		Rect optFlowRect = new Rect();
		optFlowRect.x = imageWidth / 2 - squareMetric / 2;
		optFlowRect.y = imageHeight / 2 - squareMetric / 2;
		optFlowRect.width = squareMetric;
		optFlowRect.height = squareMetric;

		optFlowMatRgba = mRgba.submat(optFlowRect);

		if (mMOP2PtsPrev.rows() == 0) {
			Imgproc.cvtColor(optFlowMatRgba, mOpFlowCurr,
					Imgproc.COLOR_RGBA2GRAY);
			mOpFlowCurr.copyTo(mOpFlowPrev);

			Imgproc.goodFeaturesToTrack(mOpFlowPrev, mMOPopFlowPrev, 50, 0.01,
					20);
			mMOP2PtsPrev.fromArray(mMOPopFlowPrev.toArray());
			mMOP2PtsPrev.copyTo(mMOP2PtsSafe);
		} else {

			mOpFlowCurr.copyTo(mOpFlowPrev);
			Imgproc.cvtColor(optFlowMatRgba, mOpFlowCurr,
					Imgproc.COLOR_RGBA2GRAY);

			Imgproc.goodFeaturesToTrack(mOpFlowCurr, mMOPopFlowCurr, 50, 0.01,
					20);
			mMOP2PtsCurr.fromArray(mMOPopFlowCurr.toArray());
			mMOP2PtsSafe.copyTo(mMOP2PtsPrev);
			mMOP2PtsCurr.copyTo(mMOP2PtsSafe);

			Video.calcOpticalFlowPyrLK(mOpFlowPrev, mOpFlowCurr, mMOP2PtsPrev,
					mMOP2PtsCurr, status, err);

			cornersPrev = mMOP2PtsPrev.toList();
			cornersCurr = mMOP2PtsCurr.toList();
			byteStatus = status.toList();

			double dis_X_uf = 0;
			double dis_Y_uf = 0;

			for (int i = 0; i < byteStatus.size() - 1; i++) {
				if (byteStatus.get(i) == 1) {
					Point pt = cornersCurr.get(i);
					Point pt2 = cornersPrev.get(i);

					pt.x += optFlowRect.x;
					pt.y += optFlowRect.y;

					pt2.x += optFlowRect.x;
					pt2.y += optFlowRect.y;

					Core.circle(mRgba, pt, 5, colorRed);

					dis_X_uf += pt.x - pt2.x;
					dis_Y_uf += pt.y - pt2.y;
				}
			}
			
			if ( dis_X_uf > 0 && dis_X_uf < motionThX) {
				dis_X_uf = 0;
			}
			if ( dis_X_uf < 0 && dis_X_uf > (-1*motionThX)) {
				dis_X_uf = 0;
			}
			if ( dis_Y_uf > 0 && dis_Y_uf < motionThY) {
				dis_Y_uf = 0;
			}
			if ( dis_Y_uf < 0 && dis_Y_uf > (-1*motionThY)) {
				dis_Y_uf = 0;
			}
			
			filterX.pushValue((int)dis_X_uf);
			filterY.pushValue((int)dis_Y_uf);
			
			int dis_X = filterX.getValue();
			int dis_Y = filterY.getValue();
			
//			motionEventListener.notifyMotion(dis_X, dis_Y);

			Log.e(TAG, "distance motion: "+ dis_X + " " + dis_Y);
			
			if (dis_X >= motionThX && dis_Y >= motionThY) {
//				Log.e(TAG, "direction assigned: forward left");
			}

			if (dis_X <= (-1*motionThX) && dis_Y <= (-1*motionThY)) {
//				Log.e(TAG, "direction assigned: backward right");
			}

			if (dis_X >= motionThX & dis_Y <= (-1*motionThY)) {
//				Log.e(TAG, "direction assigned: backward left");
			}
			if (dis_X <= (-1*motionThX) & dis_Y >= motionThY) {
//				Log.e(TAG, "direction assigned: forward right");
			}
		}
		
		return mRgba;
	}

	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL,
				4);

		return new Scalar(pointMatRgba.get(0, 0));
	}

	private List<MatOfPoint> findObjectAt(int x, int y) {

		int cols = mRgba.cols();
		int rows = mRgba.rows();

		Log.i(TAG, "Cols and rows: (" + cols + ", " + rows + ")");

		if ((x < 0) || (y < 0))
			return null;
		if (x > cols)
			x = cols;
		if (y > rows)
			y = rows;

		Rect touchedRect = new Rect();

		touchedRect.x = (x > 4) ? x - 4 : 0;
		touchedRect.y = (y > 4) ? y - 4 : 0;

		touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols
				- touchedRect.x;
		touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows
				- touchedRect.y;

		Mat touchedRegionRgba = mRgba.submat(touchedRect);

		Mat touchedRegionHsv = new Mat();
		Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv,
				Imgproc.COLOR_RGB2HSV_FULL);

		// Calculate average color of touched region
		mBlobColorHsv = Core.sumElems(touchedRegionHsv);
		int pointCount = touchedRect.width * touchedRect.height;
		for (int i = 0; i < mBlobColorHsv.val.length; i++)
			mBlobColorHsv.val[i] /= pointCount;

		mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

//		Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", "
//				+ mBlobColorRgba.val[1] + ", " + mBlobColorRgba.val[2] + ", "
//				+ mBlobColorRgba.val[3] + ")");

		mDetector = new ColorDetector(800, 640);
		mDetector.setShoeHsvColor(mBlobColorHsv);

//		Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

		touchedRegionRgba.release();
		touchedRegionHsv.release();


		mDetector.process(mRgba, 111);
		


		// Mat colorLabel = mRgba.submat(4, 68, 4, 68);
		// colorLabel.setTo(mBlobColorRgba);
		//
		// Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 +
		// mSpectrum.cols());
		// mSpectrum.copyTo(spectrumLabel);

		return mDetector.getShoesContours();

	}

	private void bugShoeCollisionCheck() {

		Log.e(TAG,
				"bug position: " + bug1.getPosition().x + "   "
						+ bug1.getPosition().y);

		if (contourShoe.size() > 0) {
			MatOfPoint2f shoeContour2f = new MatOfPoint2f();
			contourShoe.get(0).convertTo(shoeContour2f, CvType.CV_32FC2);
			double bugDistToShoe = Imgproc.pointPolygonTest(shoeContour2f,
					bug1.getPosition(), true);
			Log.e(TAG, "bug distance to shoe: " + bugDistToShoe
					+ " contour num: " + contourShoe.size());
		}

		if (contourFloor.size() > 0) {
			MatOfPoint2f floorContour2f = new MatOfPoint2f();
			contourFloor.get(0).convertTo(floorContour2f, CvType.CV_32FC2);
			double bugDistToFloor = Imgproc.pointPolygonTest(floorContour2f,
					bug1.getPosition(), true);
			Log.e(TAG, "bug distance to floor: " + bugDistToFloor
					+ " contour num: " + contourFloor.size());
		}

	}
}
