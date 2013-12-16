package com.sx.debug.opencv;

import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import com.sx.debug.opengl.OpenGLBugManager;
import com.sx.debug.sensor.MotionEventListener;
import com.sx.debug.sensor.MovingAverage;


public class OpticalFLowDetector {
	private static final int squareMetric = 200;
	private static final int motionThX = 200;
	private static final int motionThY = 200;

	public int imageOpenCvWidth;
	public int imageOpenCvHeight;
	
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
	
	private MotionEventListener motionEventListener;

	
	public OpticalFLowDetector(int width, int height) {
		this.imageOpenCvWidth = width;
		this.imageOpenCvHeight = height;
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
		filterX = new MovingAverage(5);
		filterY = new MovingAverage(5);
		
		motionEventListener = new MotionEventListener();
		motionEventListener.addObserver(OpenGLBugManager.getOpenGLBugManager());
	}
	
	public void process(Mat mRgba) {
		Rect optFlowRect = new Rect();
		optFlowRect.x = imageOpenCvWidth / 2 - squareMetric / 2;
		optFlowRect.y = imageOpenCvHeight / 2 - squareMetric / 2;
		optFlowRect.width = squareMetric;
		optFlowRect.height = squareMetric;

		optFlowMatRgba = mRgba.submat(optFlowRect);

		if (mMOP2PtsPrev.rows() == 0) {
			Imgproc.cvtColor(optFlowMatRgba, mOpFlowCurr, Imgproc.COLOR_RGBA2GRAY);
			mOpFlowCurr.copyTo(mOpFlowPrev);

			Imgproc.goodFeaturesToTrack(mOpFlowPrev, mMOPopFlowPrev, 50, 0.01, 20);
			mMOP2PtsPrev.fromArray(mMOPopFlowPrev.toArray());
			mMOP2PtsPrev.copyTo(mMOP2PtsSafe);
		} else {

			mOpFlowCurr.copyTo(mOpFlowPrev);
			Imgproc.cvtColor(optFlowMatRgba, mOpFlowCurr, Imgproc.COLOR_RGBA2GRAY);

			Imgproc.goodFeaturesToTrack(mOpFlowCurr, mMOPopFlowCurr, 50, 0.01, 20);
			mMOP2PtsCurr.fromArray(mMOPopFlowCurr.toArray());
			mMOP2PtsSafe.copyTo(mMOP2PtsPrev);
			mMOP2PtsCurr.copyTo(mMOP2PtsSafe);

			Video.calcOpticalFlowPyrLK(mOpFlowPrev, mOpFlowCurr, mMOP2PtsPrev, mMOP2PtsCurr, status, err);

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

//					Core.circle(mRgba, pt, 5, new Scalar(255,255,255));

					dis_X_uf += pt.x - pt2.x;
					dis_Y_uf += pt.y - pt2.y;
				}
			}


			filterX.pushValue((int) dis_X_uf);
			filterY.pushValue((int) dis_Y_uf);

			float dis_X = filterX.getValue();
			float dis_Y = filterY.getValue();

			if (dis_X > 0 && dis_X < motionThX) {
				dis_X = 0;
			}
			if (dis_X < 0 && dis_X > (-1 * motionThX)) {
				dis_X = 0;
			}
			if (dis_Y > 0 && dis_Y < motionThY) {
				dis_Y = 0;
			}
			if (dis_Y < 0 && dis_Y > (-1 * motionThY)) {
				dis_Y = 0;
			}
			motionEventListener.notifyMotion(dis_X, dis_Y);
//			motionEventListener.notifyMotion((float)dis_X_uf, (float)dis_Y_uf);
		}

	}
}
