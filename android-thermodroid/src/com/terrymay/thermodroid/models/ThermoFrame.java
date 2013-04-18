package com.terrymay.thermodroid.models;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ThermoFrame {

    private FloatBuffer temps;
    private float avgTemp;
    public ThermoFrame(IntBuffer frame) {
        float tempSum = 0.00f;
        int tempsToRead = 64;
        temps = FloatBuffer.allocate(tempsToRead);
        frame.rewind();
        //frame.compact();
        temps.clear();
        
        //while(frame.hasRemaining()) {
        while(tempsToRead > 0) {
            if (frame.limit() - frame.position() < 4) {
                frame.clear();
                break;
            }
            
            int[] f = new int[4];
            frame.get(f, 0, 4);
            
            int intbits = 0;
            intbits = (f[3] << 24 | f[2] << 16 | f[1] << 8 | f[0]);
            float temp = Float.intBitsToFloat(intbits);
            tempSum += temp;
            temps.put(temp);
            tempsToRead--;
        } 
        avgTemp = tempSum/64;
    }
    
    
    public FloatBuffer getTemps() {
        return temps.duplicate();
    }
    
    public float getTempAvg() {
        return avgTemp;
    }
    
    public String toFloatString() {
       // temps.compact();
        
        temps.rewind();
        String msg = "";
        msg += Float.toString(temps.get());
        for (int i = 1 ; i < temps.limit(); i++) {
            msg += Float.toString(temps.get())+", ";
            if (i%16 == 0) msg += "\n";
        }
        msg += "\n";
        return msg;
    }
    
}
