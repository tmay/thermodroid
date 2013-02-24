package com.terrymay.thermodroid.util;

import android.graphics.Color;

public class ColorUtility {


    /** Returns an interpoloated color, between a and b */
    public static int interpolateColor(int a, int b, float proportion) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }
    

    private static float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }
}
