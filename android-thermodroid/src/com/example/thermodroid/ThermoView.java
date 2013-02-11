package com.example.thermodroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ThermoView extends View {
    final private int LEFT_MARGIN = 40;
    final private int TOP_MARGIN = 150;
    
    Paint paint = new Paint();
    TempReading[] temps;
    
    public ThermoView(Context context) {
        super(context);
        temps = new TempReading[64];
        for (int i = 0; i < 64; i++) {
            temps[i] = new TempReading(10.50f);
        }
    }

    public ThermoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public ThermoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
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
              
          }
            totalHeight += height + gap;
        }
    }
}
