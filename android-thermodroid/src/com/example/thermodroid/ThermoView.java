package com.example.thermodroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ThermoView extends View {
    final private int LEFT_MARGIN = 40;
    final private int TOP_MARGIN = 150;
    
    Paint paint = new Paint();
    TempReading[] temps;
    boolean randomize = true;
    
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
    
    private void initFakeData(boolean isRandom) {
        float simTemp = -50.00f;
        temps = new TempReading[64];
        for (int i = 0; i < 64; i++) {
            if (isRandom) {
                temps[i] = new TempReading((float)Math.random()*300);
            } else {
                temps[i] = new TempReading(simTemp);
                simTemp += 5.50f;
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
        
        for (int r = 0; r < 4; r++) {
            totalWidth = LEFT_MARGIN;
            for (int c = 0; c < 16; c++) {
              paint.setColor(temps[count].getColor());
              paint.setStrokeWidth(0);
              canvas.drawRect(totalWidth, totalHeight, totalWidth+width, totalHeight+height, paint);
              totalWidth += width + gap;
              count++;
          }
            totalHeight += height + gap;
            
        }
    }
}
