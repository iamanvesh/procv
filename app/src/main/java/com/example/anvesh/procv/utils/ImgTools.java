package com.example.anvesh.procv.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by anvesh on 20/10/16.
 * Utility class which handles all the image related functionality
 */
public class ImgTools {
    private static final String TAG = "ImgTools";
    private ImgTools() {}

    public static Bitmap rotateImage(byte[] frame, int angle) {
        Bitmap bmp = BitmapFactory.decodeByteArray(frame, 0, frame.length);
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        bmp = Bitmap.createBitmap(bmp, 0, 0,
                bmp.getWidth(), bmp.getHeight(), matrix, true);

        return bmp;

    }

    public static Bitmap getBitmapFromBytes(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    private static File createFolder(String folderName) {
        File folder = new File(Environment.getExternalStorageDirectory()
                + File.separator + folderName);

        if(!folder.exists()) {
            folder.mkdir();
        }
        return folder;
    }

    public static File getImgFile(String folderName) {
        Long ts = System.currentTimeMillis() / 1000;
        final String IMG_FORMAT = ".jpg";
        String fileName = Long.toString(ts) + IMG_FORMAT;

        return new File(createFolder(folderName), fileName);
    }

    public static void saveImageFromBitmap(File imageFile, Bitmap imageBitmap, int quality) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);

        try {
            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(bos.toByteArray());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception in photoCallback", e);
        }
    }
}
