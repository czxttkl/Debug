package com.sx.debug.helper;

import java.io.File;
import java.io.IOException;

import com.sx.debug.MainActivity;
import com.sx.debug.MyGLSurfaceView;
import com.sx.debug.file.FileHelper;

import android.os.AsyncTask;
import android.os.Environment;

public class InitRenderTask extends AsyncTask<Object, Integer, Boolean> {
	MainActivity mMainActivity;

	public InitRenderTask(MainActivity mCameraActivity) {
		this.mMainActivity = mCameraActivity;
	}

	@Override
	protected Boolean doInBackground(Object... params) {
		if (!Prefs.getObjFileSaved(mMainActivity)) {
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File(sdCard.getAbsolutePath() + "/debug/");
			dir.mkdirs();

			// Move file to sd card at the first time so that we could use memory map in the future
			try {
				FileHelper.moveAssetsToSdCard(mMainActivity.getAssets().open("memorymap_vertices_little_endian"), dir.getAbsolutePath() + "/memorymap_vertices_little_endian");
				FileHelper.moveAssetsToSdCard(mMainActivity.getAssets().open("memorymap_texture_little_endian"), dir.getAbsolutePath() + "/memorymap_texture_little_endian");
				FileHelper.moveAssetsToSdCard(mMainActivity.getAssets().open("memorymap_normal_little_endian"), dir.getAbsolutePath() + "/memorymap_normal_little_endian");

				Prefs.setObjFileSaved(mMainActivity, true);
			} catch (IOException e) {

			}
		}

		mMainActivity.mGLSurfaceView.setMyRenderer();
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		mMainActivity.restoreOrCreateGLSurfaceView2();
	}
}
