package com.sx.debug.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.R.integer;

public class FileHelper {
	
	public static void moveAssetsToSdCard(InputStream assetInputStream, String filePathName) throws IOException {
		FileOutputStream fos;
		fos = new FileOutputStream(filePathName);
		copyFile(assetInputStream, fos);
		assetInputStream.close();
		assetInputStream = null;
		fos.flush();
		fos.close();
		fos = null;
	}
	
	private static void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer))!=-1) {
			out.write(buffer, 0, read);
		}
	}
}
