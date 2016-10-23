package com.example.anvesh.procv.utils;

import android.graphics.Bitmap;
import android.util.LruCache;

import java.io.File;

/**
 * Created by anvesh on 20/10/16.
 * Singleton to handle caching of images before preview
 */
public class BitmapCache {
    private static LruCache<File, Bitmap> bitmapLruCache;
    private final int cacheSize = 10 * 1024 * 1024; // 10 MiB
    private static BitmapCache instance;

    private BitmapCache() {
        bitmapLruCache = new LruCache<>(cacheSize);
    }

    public static BitmapCache getInstance() {
        if(instance == null)
            instance = new BitmapCache();
        return instance;
    }

    public Bitmap getBitmap(File file) {
        return bitmapLruCache.get(file);
    }

    public void putBitmap(File file, Bitmap bitmap) {
        bitmapLruCache.put(file, bitmap);
    }

    public void removeBitmap(File file) {
        bitmapLruCache.remove(file);
    }
}
