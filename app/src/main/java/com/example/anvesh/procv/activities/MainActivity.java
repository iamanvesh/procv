package com.example.anvesh.procv.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {
    private final String TAG = "MainActivity";
    private CustomCameraView mOpenCvCameraView;
    private boolean readyToTakePicture = false;
    private boolean isCaptureMessageShown = false;
    private boolean isFocusMessageShown = false;
    // TODO: Add permission request dialogs
    private final int REQUEST_CAMERA_PERMISSION = 1;
    private final int REQUEST_SDCARD_PERMISSION = 2;
    private final int REQUEST_CAMERA_SDCARD_PERMISSIONS = 3;
    private final int GENERIC_PERMISSION_REQUEST = 4;
    private String folderName;

    private BaseLoaderCallback mBaseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: { Log.d(TAG, "OpenCV loaded successfully");
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
            readyToTakePicture = true;
        }

        @Override
        public void onPictureCaptured() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,
                            "Capturing image.. Please wait.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onBitmapAvailable(Bitmap imageBitmap) {
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
        if (Build.VERSION.SDK_INT >= 23)
            askPermissions();
        else
            setCameraView();
    }

    private void setCameraView() {
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);
        mOpenCvCameraView.setCameraEventsCallback(cameraEventsCallback);
        // Rotate the preview stream 90 to fix portrait issue
        mOpenCvCameraView.setUserRotation(90);
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
        readyToTakePicture = false;
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
        if(readyToTakePicture && !isCaptureMessageShown) {
            isCaptureMessageShown = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Touch to capture image!", Toast.LENGTH_SHORT).show();
                }
            });
        } else if(!readyToTakePicture && !isFocusMessageShown) {
            isFocusMessageShown = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Tap to focus..", Toast.LENGTH_SHORT).show();
                }
            });
        }
        return mOpenCvCameraView.processFrame(inputFrame.rgba());
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(readyToTakePicture) {
            mOpenCvCameraView.takePicture();
        } else {
            mOpenCvCameraView.focusOnTouch(motionEvent);
        }
        return true;
    }

    private Camera.Size getHighestResolutionInAspectRatio4_3() {
        Camera mCamera = Camera.open();
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> supportedSizes = params.getSupportedPreviewSizes();
        mCamera.release();

        // Sort sizes from high res to low res
        Collections.sort(supportedSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size size, Camera.Size t1) {
                if(size.width > t1.width)
                    return -1;
                else if(size.width < t1.width)
                    return 1;
                return 0;
            }
        });

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

    public void askPermissions() {
        boolean cameraPermission = (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
        boolean sdcardPermission = (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

        if(!cameraPermission && sdcardPermission) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA}, GENERIC_PERMISSION_REQUEST);
        } else if (cameraPermission && !sdcardPermission) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, GENERIC_PERMISSION_REQUEST);
        } else if(!cameraPermission && !sdcardPermission) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_SDCARD_PERMISSIONS);
        } else {
            setCameraView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case GENERIC_PERMISSION_REQUEST: {
                if(grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setCameraView();
                } else {
                    Toast.makeText(MainActivity.this, "Permission needed!", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case REQUEST_CAMERA_SDCARD_PERMISSIONS: {
                int granted = PackageManager.PERMISSION_GRANTED;
                if(grantResults.length > 0) {
                    if(grantResults[0] == granted && grantResults[1] == granted) {
                        setCameraView();
                    } else {
                        Toast.makeText(MainActivity.this, "Permissions needed!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
