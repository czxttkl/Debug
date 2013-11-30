package edu.neu.mhealth.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class ColorDetector {
	// Maximum contour area in percent for contours filtering
	private static double mMaxContourArea = 0.15;

	// Lower and Upper bounds for range checking in HSV color space for shoes
	private Scalar mShoeLowerBound;
	private Scalar mShoeUpperBound;

	// Lower and Upper bounds for range checking in HSV color space for floor
	private Scalar mFloorLowerBound;
	private Scalar mFloorUpperBound;

	// Color radius for range checking in HSV color space
	private Scalar mColorRadius;
//	private Mat mSpectrum;
	
	private MatOfPoint[] mFloorContours;
	private MatOfPoint[] mShoesContours;

	// Cache
	Mat mPyrDownMat;
	Mat mHsvMat;
	Mat mShoeMask;
	Mat mShoeDilatedMask;
	Mat mShoeHierarchy;
	Mat mFloorMask;
	Mat mFloorDilatedMask;
	Mat mFloorHierarchy;

	// Record screen size
	int width;
	int height;
	long screenArea;

	public ColorDetector(int width, int height) {
		mShoeLowerBound = new Scalar(0);
		mShoeUpperBound = new Scalar(0);
		mFloorLowerBound = new Scalar(0);
		mFloorUpperBound = new Scalar(0);

		// Color radius for range checking in HSV color space
		mColorRadius = new Scalar(25, 50, 50, 0);
		
		mFloorContours = new MatOfPoint[1];
		mShoesContours = new MatOfPoint[2];

		// Cache
		mPyrDownMat = new Mat();
		mHsvMat = new Mat();
		mShoeMask = new Mat();
		mShoeDilatedMask = new Mat();
		mShoeHierarchy = new Mat();
		mFloorMask = new Mat();
		mFloorDilatedMask = new Mat();
		mFloorHierarchy = new Mat();

		this.width = width;
		this.height = height;
		screenArea = width * height;
	}

	public void setColorRadius(Scalar radius) {
		mColorRadius = radius;
	}

	public void setFloorHsvColor(Scalar floorHsvColor) {
		double minH = (floorHsvColor.val[0] >= mColorRadius.val[0]) ? floorHsvColor.val[0] - mColorRadius.val[0] : 0;
		double maxH = (floorHsvColor.val[0] + mColorRadius.val[0] <= 255) ? floorHsvColor.val[0] + mColorRadius.val[0] : 255;
		
		mFloorLowerBound.val[0] = minH;
		mFloorUpperBound.val[0] = maxH;

		mFloorLowerBound.val[1] = floorHsvColor.val[1] - mColorRadius.val[1];
		mFloorUpperBound.val[1] = floorHsvColor.val[1] + mColorRadius.val[1];

		mFloorLowerBound.val[2] = floorHsvColor.val[2] - mColorRadius.val[2];
		mFloorUpperBound.val[2] = floorHsvColor.val[2] + mColorRadius.val[2];

		mFloorLowerBound.val[3] = 0;
		mFloorUpperBound.val[3] = 255;
	}
	
	public void setShoeHsvColor(Scalar shoeHsvColor) {
		double minH = (shoeHsvColor.val[0] >= mColorRadius.val[0]) ? shoeHsvColor.val[0] - mColorRadius.val[0] : 0;
		double maxH = (shoeHsvColor.val[0] + mColorRadius.val[0] <= 255) ? shoeHsvColor.val[0] + mColorRadius.val[0] : 255;

		mShoeLowerBound.val[0] = minH;
		mShoeUpperBound.val[0] = maxH;

		mShoeLowerBound.val[1] = shoeHsvColor.val[1] - mColorRadius.val[1];
		mShoeUpperBound.val[1] = shoeHsvColor.val[1] + mColorRadius.val[1];

		mShoeLowerBound.val[2] = shoeHsvColor.val[2] - mColorRadius.val[2];
		mShoeUpperBound.val[2] = shoeHsvColor.val[2] + mColorRadius.val[2];

		mShoeLowerBound.val[3] = 0;
		mShoeUpperBound.val[3] = 255;
	}

	public void setMinContourArea(double area) {
		mMaxContourArea = area;
	}

	public void process(Mat rgbaImage, int openCvMode) {
		// Find max contour area
		double maxArea = 1;
		double secondMaxArea = 0;
		List<MatOfPoint> shoesContours;
		
		Imgproc.pyrDown(rgbaImage, mPyrDownMat);
		// Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);

		Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);

		// Iterator<MatOfPoint> each = contours.iterator();
		// while (each.hasNext()) {
		// MatOfPoint wrapper = each.next();
		// double area = Imgproc.contourArea(wrapper);
		// if (area > maxArea)
		// maxArea = area;
		// }
		switch (openCvMode) {
		case CameraActivity.MODE_FLOOR_COLOR_PICKED:
			List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
			Core.inRange(mHsvMat, mFloorLowerBound, mFloorUpperBound, mFloorMask);
			Imgproc.dilate(mFloorMask, mFloorDilatedMask, new Mat());
			Imgproc.findContours(mFloorDilatedMask, mContours, mFloorHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
			for (MatOfPoint mContour : mContours) {
				double area = Imgproc.contourArea(mContour);
				if (area > maxArea) {
					maxArea = area;
					Core.multiply(mContour, new Scalar(2, 2), mContour);
					mFloorContours[0] = mContour;
				}
			}
			break;

		case CameraActivity.MODE_SHOE_COLOR_PICKED:
			
			Core.inRange(mHsvMat, mShoeLowerBound, mShoeUpperBound, mShoeMask);
			Imgproc.dilate(mShoeMask, mShoeDilatedMask, new Mat());

			shoesContours = new ArrayList<MatOfPoint>();

			Imgproc.findContours(mShoeDilatedMask, shoesContours, mShoeHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
			Arrays.fill(mShoesContours, null);
			
			for (MatOfPoint mContour : shoesContours) {
				double area = Imgproc.contourArea(mContour);
				// Log.d("mDebug", "czx contour area:" + area);
				// Fake area if it is too large
				if (openCvMode == CameraActivity.MODE_TUTORIAL_1) {
					if (area > screenArea * mMaxContourArea)
						continue;
				}

				if (area > maxArea) {
					maxArea = area;
					Core.multiply(mContour, new Scalar(2, 2), mContour);
					mShoesContours[0] = mContour;
					continue;
				}
				if (area > secondMaxArea) {
					secondMaxArea = area;
					Core.multiply(mContour, new Scalar(2, 2), mContour);
					mShoesContours[1] = mContour;
					continue;
				}
			}

			break;

		case CameraActivity.MODE_TUTORIAL_1:

			Core.inRange(mHsvMat, mShoeLowerBound, mShoeUpperBound, mShoeMask);
			Imgproc.dilate(mShoeMask, mShoeDilatedMask, new Mat());

			shoesContours = new ArrayList<MatOfPoint>();

			Imgproc.findContours(mShoeDilatedMask, shoesContours, mShoeHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
			Arrays.fill(mShoesContours, null);
			Arrays.fill(mShoesContours, null);
			for (MatOfPoint mshoesContour : shoesContours) {
				double area = Imgproc.contourArea(mshoesContour);
				// Fake area if it is too large for sticky notesd
				if (openCvMode == CameraActivity.MODE_TUTORIAL_1) {
					if (area > screenArea * mMaxContourArea)
						continue;
				}

				if (area > maxArea) {
					maxArea = area;
					Core.multiply(mshoesContour, new Scalar(2, 2), mshoesContour);
					mShoesContours[0] = mshoesContour;
					continue;
				}
				if (area > secondMaxArea) {
					secondMaxArea = area;
					Core.multiply(mshoesContour, new Scalar(2, 2), mshoesContour);
					mShoesContours[1] = mshoesContour;
					continue;
				}
			}
			break;
		default:

		}
		// // Filter contours by area and resize to fit the original image size
		// mContours.clear();
		// each = contours.iterator();
		// while (each.hasNext()) {
		// MatOfPoint contour = each.next();
		// if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
		// Core.multiply(contour, new Scalar(4,4), contour);
		// mContours.add(contour);
		// }
		// }
	}

	// public List<MatOfPoint> getContours() {
	public List<MatOfPoint> getShoesContours() {
		return new LinkedList<MatOfPoint>(Arrays.asList(mShoesContours));
	}
	
	public List<MatOfPoint> getFloorContours() {
		return Arrays.asList(mFloorContours);
	}
}
