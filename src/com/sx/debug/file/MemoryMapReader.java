package com.sx.debug.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;

import com.sx.debug.helper.Global;


import android.R.integer;
import android.os.Environment;
import android.util.Log;

public class MemoryMapReader {
	
	public static FloatBuffer mapToFloatBuffer(String fileName) throws IOException {
		FileInputStream fis = new FileInputStream(fileName);
		FileChannel fileChannel = fis.getChannel();
		long length = fileChannel.size();
		FloatBuffer mFloatBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, length).order(ByteOrder.nativeOrder()).asFloatBuffer();  
		return mFloatBuffer;
	}

}
