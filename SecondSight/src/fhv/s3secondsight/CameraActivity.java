package fhv.s3secondsight;

import java.io.File;
import java.io.IOException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.NativeCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import fhv.s3secondsight.filters.*;
import fhv.s3secondsight.filters.convolution.*;
import fhv.s3secondsight.filters.curve.*;
import fhv.s3secondsight.filters.mixer.*;
import fhv.s3secondsight.filters.ar.*;

public class CameraActivity extends FragmentActivity implements CvCameraViewListener2 {
	
	private static final String TAG = "MainActivity";
	private static final String STATE_CAMERA_INDEX = "cameraIndex";
	private int mCameraIndex;
	private boolean mIsCameraFrontFacing;
	private int mNumCameras;
	private CameraBridgeViewBase mCameraView;
	private boolean mIsPhotoPending;
	private Mat mBgr;
	private boolean mIsMenuLocked;
	
	private static final String STATE_IMAGE_DETECTION_FILTER_INDEX = "imageDetectionFilterIndex";
	private static final String STATE_CURVE_FILTER_INDEX = "curveFilterIndex";
	private static final String STATE_MIXER_FILTER_INDEX = "mixerFilterIndex";
	private static final String STATE_CONVOLUTION_FILTER_INDEX = "convolutionFilterIndex";
	private Filter[] mImageDetectionFilters;
	private Filter[] mCurveFilters;
	private Filter[] mMixerFilters;
	private Filter[] mConvolutionFilters;
	private int mImageDetectionFilterIndex;
	private int mCurveFilterIndex;
	private int mMixerFilterIndex;
	private int mConvolutionFilterIndex;
	
	private BaseLoaderCallback mLoaderCallback =
			new BaseLoaderCallback(this){
		@Override
		public void onManagerConnected(final int status){
			switch(status){
				case LoaderCallbackInterface.SUCCESS:
					Log.d(TAG, "OpenCV loaded successfully");
					mCameraView.enableView();
					mBgr = new Mat();
					
					final Filter starryNight;
					try {
						starryNight = new ImageDetectionFilter(CameraActivity.this, R.drawable.starry_night);
					} catch (IOException e) {
						Log.e(TAG, "Failed to load drawable: starry_night");
						e.printStackTrace();
						break;
					}
					
					final Filter akbarHunting;
					try {
						akbarHunting = new ImageDetectionFilter(CameraActivity.this, R.drawable.akbar_hunting_with_cheetahs);
					} catch (IOException e) {
						Log.e(TAG, "Failed to load drawable: akbar hunting");
						e.printStackTrace();
						break;
					}
					
					mImageDetectionFilters = new Filter[] {
							new NoneFilter(),
							starryNight, 
							akbarHunting
					};
					mCurveFilters = new Filter[] {
							new NoneFilter(),
							new PortraCurveFilter(),
							new ProviaCurveFilter(),
							new VelviaCurveFilter(),
							new CrossProcessCurveFilter()
					};
					mMixerFilters = new Filter[] {
							new NoneFilter(),
							new RecolorRCFilter(),
							new RecolorRGVFilter(),
							new RecolorCMVFilter()
					};
					mConvolutionFilters = new Filter[] {
						new NoneFilter(),
						new StrokeEdgesFilter()
					};
					break;
				default:
					super.onManagerConnected(status);
					break;
			}
		}
	};

	@SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Window window = getWindow();
        window.addFlags(
        		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        if (savedInstanceState != null){
        	mCameraIndex = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
        	mImageDetectionFilterIndex = savedInstanceState.getInt(STATE_IMAGE_DETECTION_FILTER_INDEX, 0);
        	mCurveFilterIndex = savedInstanceState.getInt(STATE_CURVE_FILTER_INDEX, 0);
        	mMixerFilterIndex = savedInstanceState.getInt(STATE_MIXER_FILTER_INDEX, 0);
        	mConvolutionFilterIndex = savedInstanceState.getInt(STATE_CONVOLUTION_FILTER_INDEX, 0);
        } else {
        	mCameraIndex = 0;
        	mImageDetectionFilterIndex = 0;
        	mCurveFilterIndex = 0;
        	mMixerFilterIndex = 0;
        	mConvolutionFilterIndex = 0;
        }
        
        if (Build.VERSION.SDK_INT >=
        		Build.VERSION_CODES.GINGERBREAD) {
        	CameraInfo cameraInfo = new CameraInfo();
        	Camera.getCameraInfo(mCameraIndex, cameraInfo);
        	mIsCameraFrontFacing = 
        			(cameraInfo.facing == 
        			CameraInfo.CAMERA_FACING_FRONT);
        	mNumCameras = Camera.getNumberOfCameras();
        } else {
        	mIsCameraFrontFacing = false;
        	mNumCameras = 1;
        }
        
        mCameraView = new NativeCameraView(this, mCameraIndex);
        mCameraView.setCvCameraViewListener(this);
        setContentView(mCameraView);
    }
	
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);
		savedInstanceState.putInt(STATE_IMAGE_DETECTION_FILTER_INDEX, mImageDetectionFilterIndex);
		savedInstanceState.putInt(STATE_CURVE_FILTER_INDEX, mCurveFilterIndex);
		savedInstanceState.putInt(STATE_MIXER_FILTER_INDEX, mMixerFilterIndex);
		savedInstanceState.putInt(STATE_CONVOLUTION_FILTER_INDEX, mConvolutionFilterIndex);
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onPause() {
		if (mCameraView != null) {
			mCameraView.disableView();
		}
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
		mIsMenuLocked = false;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mCameraView != null){
			mCameraView.disableView();
		}
	}


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_camera, menu);
        if (mNumCameras < 2){
        	menu.removeItem(R.id.menu_next_camera);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
    	
    	if (mIsMenuLocked) {
    		return true;
    	}
    	
    	switch (item.getItemId()) {
    	case R.id.menu_next_image_detection_filter:
    		mImageDetectionFilterIndex++;
    		if (mImageDetectionFilterIndex == mImageDetectionFilters.length) {
    			mImageDetectionFilterIndex = 0;
    		}
    		return true;
    	case R.id.menu_next_curve_filter:
    		mCurveFilterIndex++;
    		if (mCurveFilterIndex == mCurveFilters.length) {
    			mCurveFilterIndex = 0;
    		}
    		return true;
    	case R.id.menu_next_mixer_filter:
    		mMixerFilterIndex++;
    		if (mMixerFilterIndex == mMixerFilters.length) {
    			mMixerFilterIndex = 0;
    		}
    		return true;
    	case R.id.menu_next_convolution_filter:
    		mConvolutionFilterIndex++;
    		if (mConvolutionFilterIndex == mConvolutionFilters.length) {
    			mConvolutionFilterIndex = 0;
    		}
    		return true;
    	case R.id.menu_next_camera:
    		mIsMenuLocked = true;
    		
    		mCameraIndex++;
    		if (mCameraIndex == mNumCameras) {
    			mCameraIndex = 0;
    		}
    		recreate();
    		return true;
    		
    	case R.id.menu_take_photo:
    		mIsMenuLocked = true;
    		
    		mIsPhotoPending = true;
    		return true;
    		
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    @Override
    public void onCameraViewStarted(final int width, final int heigth) {
    }
    
    @Override
    public void onCameraViewStopped() {
    }
    
    @Override
    public Mat onCameraFrame(final CvCameraViewFrame inputFrame) {
    	final Mat rgba = inputFrame .rgba();
    	
    	if (mImageDetectionFilters != null) {
    		mImageDetectionFilters[mImageDetectionFilterIndex].apply(rgba, rgba);
    	}
    	if (mCurveFilters != null) {
    		mCurveFilters[mCurveFilterIndex].apply(rgba, rgba);
    	}
    	if (mMixerFilters != null) {
    		mMixerFilters[mMixerFilterIndex].apply(rgba, rgba);
    	}
    	if (mConvolutionFilters != null) {
    		mConvolutionFilters[mConvolutionFilterIndex].apply(rgba, rgba);
    	}
    	
    	if (mIsPhotoPending) {
    		mIsPhotoPending = false;
    		takePhoto(rgba);
    	}
    	
    	if (mIsCameraFrontFacing) {
    		Core.flip(rgba, rgba, 1);
    	}
    	
    	return rgba;
    }
    
    private void takePhoto(final Mat rgba) {
    	final long currentTimeMillis = System.currentTimeMillis();
    	final String appName = getString(R.string.app_name);
    	final String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
    	final String albumPath = galleryPath + "/" + appName;
    	final String photoPath = albumPath + "/" + currentTimeMillis + ".png";
    	final ContentValues values = new ContentValues();
    	values.put(MediaStore.MediaColumns.DATA, photoPath);
    	values.put(Images.Media.MIME_TYPE, LabActivity.PHOTO_MIME_TYPE);
    	values.put(Images.Media.TITLE, appName);
    	values.put(Images.Media.DESCRIPTION, appName);
    	values.put(Images.Media.DATE_TAKEN, currentTimeMillis);
    	
    	File album = new File(albumPath);
    	if (!album.isDirectory() && !album.mkdirs()) {
    		Log.e(TAG, "Failed to create album directory at " + albumPath);
    		onTakePhotoFailed();
    		return;
    	}
    	
    	Imgproc.cvtColor(rgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
    	if (!Highgui.imwrite(photoPath, mBgr)) {
    		Log.e(TAG, "Failed to save photo to " + photoPath);
    		onTakePhotoFailed();
    	}
    	Log.d(TAG, "Photo saved successfully to " + photoPath);
    	
    	Uri uri;
    	
    	try {
    		uri = getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
    	} catch (final Exception e) {
    		Log.e(TAG, "FAiled to insert photo into MediaStore");
    		e.printStackTrace();
    		
    		File photo = new File(photoPath);
    		if (!photo.delete()) {
    			Log.e(TAG, "Failed to delete non-inserted photo");
    		}
    		
    		onTakePhotoFailed();
    		return;
    	}
    	
    	final Intent intent = new Intent(this, LabActivity.class);
    	intent.putExtra(LabActivity.EXTRA_PHOTO_URI, uri);
    	intent.putExtra(LabActivity.EXTRA_PHOTO_DATA_PATH, photoPath);
    	startActivity(intent);
    }
    
    private void onTakePhotoFailed() {
    	mIsMenuLocked = false;
    	
    	final String errorMessage = getString(R.string.photo_error_message);
    	runOnUiThread(new Runnable() {
    		@Override
    		public void run() {
    			Toast.makeText(CameraActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
    		}
    	});
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_camera, container, false);
            return rootView;
        }
    }

}
