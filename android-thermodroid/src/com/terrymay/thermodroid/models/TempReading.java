package com.terrymay.thermodroid.models;

import com.terrymay.thermodroid.util.ColorUtility;

import android.graphics.Color;

public class TempReading {
    
    final public float DEVICE_MIN_TEMP     =   -50.00f;
    final public float DEVICE_MAX_TEMP     =   300.00f;
    
    final public static float NORMAL_MIN_TEMP     =     0.00f;
    final public static float NORMAL_MAX_TEMP     =   100.00f;
    
    
    final public static int MIN_TEMP_COLOR    =   0xFF00FF;
    final public static int MAX_TEMP_COLOR    =   Color.RED;
    
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
        return ColorUtility.interpolateColor(MIN_TEMP_COLOR, MAX_TEMP_COLOR, getProportion());
    }
    
    public float getProportion() {
        return (mReading / NORMAL_MAX_TEMP);
    }
}
