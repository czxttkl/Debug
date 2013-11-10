package edu.neu.mhealth.debug;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import edu.neu.mhealth.debug.helper.Global;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.WindowManager;

public class CameraActivity extends Activity implements CvCameraViewListener2 {

	/*Basic Variables*/
	private final String TAG = Global.APP_LOG_TAG;
	private final int ACCUMULATOR_THRESHOLD = 50;
	private final int MIN_LINE_LENGTH = 100;
	private final int MAX_LINE_GAP = 10;
	private final int HYSTERESIS_THRESHOLD1 = 50;
	private final int HYSTERESIS_THRESHOLD2 = 400;
	private final int HOUGH_LINE_COUNT = 5;
	
	/*OpenCv Variables*/
	private Mat mRgba;
	private Mat mGray;
	private Mat lines;
	private CameraBridgeViewBase mOpenCvCameraView;
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS: {
					Log.i(TAG, "OpenCV loaded successfully");
					mOpenCvCameraView.enableView();
				}
				break;
				default: {
					super.onManagerConnected(status);
				}
				break;
			}
		}
	};
	
	/*
	 *   Activity Callbacks
	 *   
	 *   */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_camera);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HellpOpenCvView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
		}
	}

	
	
	/*
	 *   Opencv Callbacks
	 *   
	 *   */
	@Override
	public void onCameraViewStarted(int width, int height) {
		lines = new Mat();
	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();
		Mat cannyMat = new Mat();
		Imgproc.Canny(mGray, cannyMat, HYSTERESIS_THRESHOLD1, HYSTERESIS_THRESHOLD2, 3, false);
		Imgproc.HoughLinesP(cannyMat, lines, 1, Math.PI/180, ACCUMULATOR_THRESHOLD, MIN_LINE_LENGTH, MAX_LINE_GAP);
		
		for (int x = 0; x < lines.cols() && x < HOUGH_LINE_COUNT; x++) {
	          double[] vec = lines.get(0, x);
	          if(vec!=null) {
	        	  double x1 = vec[0], 
	        			  y1 = vec[1],
	        			  x2 = vec[2],
	        			  y2 = vec[3];
	        	  Point start = new Point(x1, y1);
	        	  Point end = new Point(x2, y2);
	        	  Core.line(mRgba, start, end, new Scalar(255,0,0), 3);
	          }
	    }
		
		return mRgba;
	}

	
}
