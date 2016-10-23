package com.example.anvesh.procv.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.example.anvesh.procv.R;
import com.example.anvesh.procv.utils.BitmapCache;
import com.example.anvesh.procv.utils.ImgTools;

import java.io.File;

public class ImagePreviewActivity extends AppCompatActivity {
    private ImageView imagePreview;
    private File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        imagePreview = (ImageView) findViewById(R.id.image_preview);
        imageFile = new File(getIntent().getStringExtra("imageFilePath"));
        imagePreview.setImageBitmap(BitmapCache.getInstance().getBitmap(imageFile));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void saveImgFromCache(View view) {
        Bitmap imageBitmap = BitmapCache.getInstance().getBitmap(imageFile);
        int quality = 85;
        ImgTools.saveImageFromBitmap(imageFile, imageBitmap, quality);
        finish();
    }

    public void deleteImgFromCache(View view) {
        BitmapCache.getInstance().removeBitmap(imageFile);
        finish();
    }
}
