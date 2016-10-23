package com.example.anvesh.procv.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.example.anvesh.procv.intfs.CameraEvents;
import com.example.anvesh.procv.utils.ImgTools;

import org.opencv.android.JavaCameraView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anvesh on 17/10/16.
 */
public class CustomCameraView extends JavaCameraView implements Camera.PictureCallback {
    private final String TAG = "CustomCameraView";
    private CameraEvents cameraEventsCallback;
    private boolean safeToTakePicture = true;
    private final int FOCUS_AREA_SIZE = 300;

    public CustomCameraView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    @Override
    public void onPictureTaken(final byte[] bytes, Camera camera) {
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        Thread createBitmapThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // camera preview is in landscape so rotate
                // the image 90 degrees in clockwise direction
                Bitmap rotatedImageBitmap = ImgTools.rotateImage(bytes, 90);
                cameraEventsCallback.onBitmapAvailable(rotatedImageBitmap);
//                cameraEventsCallback.onPictureCaptured(rotatedImageBitmap);
                safeToTakePicture = true;
            }
        });
        createBitmapThread.start();
    }

    public void takePicture() {
        mCamera.setPreviewCallback(null);
        cameraEventsCallback.onPictureCaptured();

        if(safeToTakePicture) {
            mCamera.takePicture(null, null, this);
            safeToTakePicture = false;
        }
    }

    public Mat processFrame(Mat frame) {
        Mat gray = new Mat(frame.width() / 2, frame.height() / 2, frame.type());
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

        Mat hierarchy = new Mat(frame.size(), frame.type());
        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.threshold(gray, gray, 100, 255, Imgproc.THRESH_BINARY);
        Core.bitwise_not(gray, gray);

        Imgproc.findContours(gray, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        List<org.opencv.core.Rect> squares = new ArrayList<>();
        for(int i = 0; i < contours.size(); ++i) {
            MatOfPoint currentContour = contours.get(i);
            org.opencv.core.Rect rect = Imgproc.boundingRect(currentContour);
            Mat m = gray.submat(rect);
            Core.extractChannel(m, m, 0);
            int totalPixels = m.rows() * m.cols();
            // Since we are inverting the image using bitwise_not above
            // zeroPixel will be converted to nonZeroPixel
            int zeroPixels = Core.countNonZero(m);
            double blackPercentage = ((double) zeroPixels / totalPixels) * 100.0;

            double k = (double) rect.height / rect.width;

            if(0.9 < k && k < 1.1 && rect.area() > 200) {
                if (blackPercentage >= 90.0) {
                    squares.add(rect);
                    Imgproc.drawContours(frame, contours, i, new Scalar(255, 255, 0), 3);
                }
            }
        }
        Log.v("total", "" + squares.size());
        if(squares.size() == 4) {
            float MARGIN_ERROR = 150.0f;

            boolean d1d2 = (Math.abs(squares.get(0).area() - squares.get(1).area()) <= MARGIN_ERROR);
            boolean d2d3 = (Math.abs(squares.get(1).area() - squares.get(2).area()) <= MARGIN_ERROR);
            boolean d3d4 = (Math.abs(squares.get(2).area() - squares.get(3).area()) <= MARGIN_ERROR);

//            Log.e("areas", d1d2 + " " + d2d3 + " " + d3d4);
//            Rect s1 = squares.get(0);
//            Rect s2 = squares.get(1);
//            Rect s3 = squares.get(2);
//            Rect s4 = squares.get(3);
//
//            Log.e("areas", "(" + s1.x + "," + s1.y + "): " + s1.area());
//            Log.e("areas", "(" + s2.x + "," + s2.y + "): " + s2.area());
//            Log.e("areas", "(" + s3.x + "," + s3.y + "): " + s3.area());
//            Log.e("areas", "(" + s4.x + "," + s4.y + "): " + s4.area());
            if(d1d2 && d2d3 && d3d4) {
                // TODO: send a message to main activity to display a toast
                Log.v("squares", "All squares detected");
                cameraEventsCallback.readyToTakePicture();
                Log.e(TAG, "Capturing image!");
            }
        }
        return frame;
    }

    public void setCameraEventsCallback(CameraEvents cameraEventsCallback) {
        this.cameraEventsCallback = cameraEventsCallback;
    }

    public void focusOnTouch(MotionEvent event) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();

            Rect focusAreaRect = calculateFocusArea(event.getX(), event.getY());

            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
            meteringAreas.add(new Camera.Area(focusAreaRect, 800));
            parameters.setFocusAreas(meteringAreas);

            mCamera.setParameters(parameters);
            mCamera.autoFocus(mAutoFocusTakePictureCallback);
        } else {
            mCamera.autoFocus(mAutoFocusTakePictureCallback);
        }
    }

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / this.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / this.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper)+focusAreaSize/2>1000){
            if (touchCoordinateInCameraReper>0){
                result = 1000 - focusAreaSize/2;
            } else {
                result = -1000 + focusAreaSize/2;
            }
        } else{
            result = touchCoordinateInCameraReper - focusAreaSize/2;
        }
        return result;
    }

    private Camera.AutoFocusCallback mAutoFocusTakePictureCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if(success) {
                Log.v(TAG, "focus: success");
            } else {
                Log.v(TAG, "focus: failed");
            }
        }
    };
}
