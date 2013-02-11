package com.example.thermodroid;

import android.graphics.Color;

public class TempReading {

    private float mReading;
    
    public TempReading(float reading) {
        this.mReading = reading;
    }

    public float getReading() {
        return this.mReading;
    }
    
    public int getColor() {
        return Color.RED;
    }
}
