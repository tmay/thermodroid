package com.terrymay.thermodroid.views;

import java.nio.FloatBuffer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.terrymay.thermodroid.models.TempReading;
import com.terrymay.thermodroid.models.ThermoFrame;
import com.terrymay.thermodroid.util.ColorUtility;

public class ThermoView extends View {
    final private int LEFT_MARGIN = 40;
    final private int TOP_MARGIN = 150;
    
    Paint paint = new Paint();
    TempReading[] temps; 
    float avgTemp = 0.00f;
    boolean randomize = false;
    
    public ThermoView(Context context) {
        super(context);
        this.initFakeData(randomize);
    }

    public ThermoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initFakeData(randomize);
    }

    public ThermoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.initFakeData(randomize);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        
        if (event.getRawX() > 400) {
            initFakeData(true);
        } else {
            initFakeData(false);
        }
        
        this.invalidate();
        return true;
    };
    
    public void update(ThermoFrame data) {
        FloatBuffer readings = data.getTemps();
        avgTemp = data.getTempAvg();
        //Log.i("avg", Float.toString(avgTemp));
        readings.rewind();
        temps = new TempReading[64];
        for (int i = 0; i < 64; i++) {
            temps[i] = new TempReading(readings.get());
        }
        this.invalidate();
    }
    
    private void initFakeData(boolean isRandom) {
        float simTemp = 0.00f;
        temps = new TempReading[64];
        for (int i = 0; i < 64; i++) {
            if (isRandom) {
                temps[i] = new TempReading((float)Math.random()*TempReading.NORMAL_MAX_TEMP);
            } else {
                temps[i] = new TempReading(simTemp);
                simTemp += 1.562f;
            }
        }
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        int totalWidth = LEFT_MARGIN;
        int totalHeight = TOP_MARGIN;
        int gap = 10;
        int width = 60;
        int height = 60;
        int count = 0;
        for (int c=0; c<15;c++) {
            totalWidth += width + gap;
            totalHeight = TOP_MARGIN;
            for (int r=0; r<4; r++) { 
                paint.setColor(temps[count].getColor());
                paint.setStrokeWidth(0);
                canvas.drawRect(totalWidth, totalHeight, totalWidth+width, totalHeight+height, paint);
                totalHeight += height + gap;
                count++;
            }
        }
        drawLegend(canvas, (avgTemp/TempReading.NORMAL_MAX_TEMP));
    }
    
    private void drawLegend(Canvas canvas, float avgProportion) {
        int top = TOP_MARGIN + 350;
        int left = LEFT_MARGIN+70;
        float width = 1040.00f;
        int height = 50;
        
        for (int c=0; c<width; c++) {
            float proportion = ((left+c)/width);
            if (proportion < avgProportion) {
                paint.setColor(Color.RED);
                paint.setStrokeWidth(3);
            } else {
                paint.setColor(ColorUtility.interpolateColor(TempReading.MIN_TEMP_COLOR, TempReading.MAX_TEMP_COLOR, proportion));
                paint.setStrokeWidth(0);
            }
            
            canvas.drawLine((left+c), top, (left+c), (top+height), paint);
        }
        
    }
}
