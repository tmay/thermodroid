package com.example.thermodroid;

import android.graphics.Color;
import android.util.Log;

public class TempReading {
    final private float DEVICE_MIN_TEMP   =   -50.00f;
    final private float DEVICE_MAX_TEMP   =   300.00f;
    
    final private int MIN_TEMP_COLOR    =   Color.BLUE;
    final private int MAX_TEMP_COLOR    =   Color.RED;
    
    static int[] gradient;
    private float mReading;
    
    public TempReading() {
        
    }
    
    public TempReading(float reading) {
        this.mReading = reading;
    }

    public float getReading() {
        return this.mReading;
    }
    
    public int getColor() {
        return interpolateColor(MIN_TEMP_COLOR, MAX_TEMP_COLOR, getProportion());
    }
    
    public float getProportion() {
        return (mReading / DEVICE_MAX_TEMP);
    }

    /** Returns an interpoloated color, between <code>a</code> and <code>b</code> */
    private int interpolateColor(int a, int b, float proportion) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }
    

    private float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }
}
