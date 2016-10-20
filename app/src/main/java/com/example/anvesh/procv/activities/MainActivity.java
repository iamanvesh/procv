package com.example.anvesh.procv.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.anvesh.procv.R;
import com.example.anvesh.procv.intfs.CameraEvents;
import com.example.anvesh.procv.utils.BitmapCache;
import com.example.anvesh.procv.utils.ImgTools;
import com.example.anvesh.procv.views.CustomCameraView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {
    private final String TAG = "MainActivity";
    private CustomCameraView mOpenCvCameraView;
    private boolean safeToTakePicture = false;
    private boolean isCaptureMessageShown = false;
    // TODO: Add permission request dialogs
    private final int REQUEST_CAMERA_PERMISSION = 1;
    private String folderName;

    private BaseLoaderCallback mBaseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    Log.d(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private CameraEvents cameraEventsCallback = new CameraEvents() {
        @Override
        public void readyToTakePicture() {
            safeToTakePicture = true;
        }

        @Override
        public void onPictureCaptured(Bitmap imageBitmap) {
            // cache the bitmap
            File imageFile = ImgTools.getImgFile(folderName);
            BitmapCache.getInstance().putBitmap(imageFile, imageBitmap);

            // pass the image file path to ImagePreviewActivity
            Intent intent = new Intent(MainActivity.this, ImagePreviewActivity.class);
            intent.putExtra("imageFilePath", imageFile.getAbsolutePath());
            startActivity(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CustomCameraView) findViewById(R.id.opencvCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);
        mOpenCvCameraView.setCameraEventsCallback(cameraEventsCallback);
        mOpenCvCameraView.setResolution(getHighestResolutionInAspectRatio4_3());

        String customFolderName = getIntent().getStringExtra("folderName");
        folderName = (customFolderName == null || customFolderName.isEmpty() ||
                            customFolderName.length() == 0) ? getString(R.string.app_name) : customFolderName;
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_13, this, mBaseLoaderCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        safeToTakePicture = false;
        isCaptureMessageShown = false;
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Toast.makeText(MainActivity.this, width + "x" + height, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraViewStopped() {}

    /**
     * The frame returned from this method is shown in the preview
     * Draw squares over the markers in this method
     * Check the areas of each marker and if they are nearly equal
     * call mOpenCvCameraView.takePicture(fileName)
     * @param inputFrame the frame caught from the lens
     * @return the frame we want to show on the screen
     */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if(safeToTakePicture && !isCaptureMessageShown) {
            isCaptureMessageShown = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Touch to capture image!", Toast.LENGTH_SHORT).show();
                }
            });
        }
        return mOpenCvCameraView.processFrame(inputFrame.rgba());
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(safeToTakePicture) {
            mOpenCvCameraView.takePicture();
        }
        return true;
    }

    private Camera.Size getHighestResolutionInAspectRatio4_3() {
        Camera mCamera = Camera.open();
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> supportedSizes = params.getSupportedPreviewSizes();
        mCamera.release();
        final float ASPECT_RATIO_4_3 = 1.33333f;
        final float MARGIN_ERROR = 0.0001f;
        for(Camera.Size size : supportedSizes) {
            float currRatio = (float) size.width / size.height;
            if(Math.abs(currRatio - ASPECT_RATIO_4_3) < MARGIN_ERROR) {
                return size;
            }
        }
        return null;
    }
}
