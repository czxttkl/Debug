package com.sx.debug.opencv;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ImageProcessor {

	private static ImageProcessor imgProcessor;

	private static int mWidth;
	private static int mHeight;
	private Mat mIntermediateRGBMat;
	private Mat mIntermediateGreyMat;
	private Mat mPyrDownMat = new Mat();
	
	public static ImageProcessor getImageProcessor(int width, int height) {
		
		mWidth = width;
		mHeight = height;
		
		if (imgProcessor == null) {
			imgProcessor = new ImageProcessor();
		}
		
		return imgProcessor;
	}
	private ImageProcessor() {
		mIntermediateGreyMat = new Mat(mHeight, mWidth, CvType.CV_8UC1);
		mIntermediateRGBMat = new Mat(mHeight, mWidth, CvType.CV_8UC4);
	}
	
	public Mat preProcess(Mat originImage) {
		
        Imgproc.pyrDown(originImage, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
        
        
		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,
				new Size(3, 3));
		for (int i = 0; i < 10; i++) {
			Imgproc.erode(originImage, mIntermediateGreyMat, kernel, new Point(-1, -1), 3);
			Imgproc.dilate(mIntermediateGreyMat, mIntermediateGreyMat, kernel, new Point(-1, -1), 1);
		}

		return mIntermediateGreyMat;
	}
}
