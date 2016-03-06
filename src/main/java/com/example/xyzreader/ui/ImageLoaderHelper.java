package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

public class ImageLoaderHelper {

    private static ImageLoaderHelper mInstance;

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static Context mCtx;

    public static synchronized ImageLoaderHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ImageLoaderHelper(context);
        }
        return mInstance;
    }


    private ImageLoaderHelper(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();
        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);
            @Override
            public void putBitmap(String key, Bitmap value) {
                cache.put(key, value);
            }

            @Override
            public Bitmap getBitmap(String key) {
                return cache.get(key);
            }
        });
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }
}
