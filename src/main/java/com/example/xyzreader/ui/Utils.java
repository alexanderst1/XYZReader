package com.example.xyzreader.ui;

import android.graphics.Bitmap;
import android.support.annotation.ColorInt;
import android.support.v7.graphics.Palette;

/**
 * Created by Alexander on 2/29/2016.
 */
public class Utils {

    public static int getDarkMutedColor(Bitmap bm)
    {
        Palette p = new Palette.Builder(bm).maximumColorCount(12).generate();
        return p.getDarkMutedColor(0xFF333333);
    }
}
