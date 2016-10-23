package com.example.anvesh.procv.intfs;

import android.graphics.Bitmap;

import java.io.File;

/**
 * Created by anvesh on 19/10/16.
 */
public interface CameraEvents {
    public void readyToTakePicture();
    public void onPictureCaptured();
    public void onBitmapAvailable(Bitmap imageBitmap);
}
