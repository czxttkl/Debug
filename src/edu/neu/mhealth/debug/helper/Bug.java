package edu.neu.mhealth.debug.helper;

import org.opencv.core.Point;
import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public class Bug extends ImageButton {

	private int ID;
	private int initalX = 0;
	private int initalY = 0;
	private int offsetX = 0;
	private int offsetY = 0;
	private int bugWidth = 50;
	private int bugHeight = 50;
	private int limitX;
	private int limitY;
	
	private int accX = 0;
	private int accY = 0;

	public Bug(Context context) {
		super(context);
		accX = 15;
		accY = 20;
	}

	public Bug(Context context, int xacc, int yacc) {
		super(context);
		accX = xacc;
		accY = yacc;
		this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		this.setBackgroundColor(Color.GREEN);
//		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
//		layoutParams.setMargins(0, 0, 0, 0);
//		this.setLayoutParams(layoutParams);
		this.setPadding(0, 0, 0, 0);
//		this.setLayoutParams(new FrameLayout.LayoutParams(50, 50));
	}

	public void setOffset(int xoffset, int yoffset) {
		offsetX = xoffset;
		offsetY = yoffset;
	}
	
	public void setLimit(int xlimit, int ylimit) {
		limitX = xlimit;
		limitY = ylimit;
	}
	public void crawl() {
		
		this.offsetLeftAndRight(accX);
		this.offsetTopAndBottom(accY);
		int[] loc = new int[2];
		this.getLocationInWindow(loc);
		if(loc[0] > limitX || loc[0] < 0) {
			accX *= -1;
		}
		if (loc[1] > limitY || loc[1] < 0) {
			accY *= - 1;
		}
	}
	
	public Point getPosition() {
		
		int[] loc = new int[2];
		this.getLocationInWindow(loc);
		Point position = new Point(loc[0] - offsetX + bugWidth/2 , loc[1] - offsetY + bugHeight/2);
		return position;
	}
}
