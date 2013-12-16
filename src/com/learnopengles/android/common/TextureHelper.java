package com.learnopengles.android.common;

import com.sx.debug.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class TextureHelper {
	public static int loadTexture(final Context context, final int resourceId, int activeTextureNumber) {

		final int[] textureHandle = new int[1];

		GLES20.glGenTextures(1, textureHandle, 0);

		if (textureHandle[0] != 0) {
			// Set the active texture unit to texture unit #
			GLES20.glActiveTexture(activeTextureNumber);

			// Set decode option
			final BitmapFactory.Options options = new BitmapFactory.Options();
			// No pre-scaling
			options.inScaled = false;

			// Read in the resource
			final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

			// Bind to the texture in OpenGL
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

			// Set filtering
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

			// Load the bitmap into the bound texture.
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

			// Recycle the bitmap, since its data has been loaded into OpenGL.
			bitmap.recycle();
		}

		if (textureHandle[0] == 0) {
			throw new RuntimeException("Error loading texture.");
		}

		return textureHandle[0];
	}

	public static int[] loadTextures(final Context context, final int[] resourceIds, int[] activeTextureNumbers) {
		final int[] textureHandles = new int[5];
		
		GLES20.glGenTextures(5, textureHandles, 0);
		
		for (int i = 0; i < 5; i++) {
			// Set the active texture unit to texture unit #
			GLES20.glActiveTexture(activeTextureNumbers[i]);

			// Set decode option
			final BitmapFactory.Options options = new BitmapFactory.Options();
			// No pre-scaling
			options.inScaled = false;

			// Read in the resource
			final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceIds[i], options);

			// Bind to the texture in OpenGL
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[i]);

			// Set filtering
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

			// Load the bitmap into the bound texture.
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

			// Recycle the bitmap, since its data has been loaded into OpenGL.
			bitmap.recycle();
		}
		
		return textureHandles;
	}
}
