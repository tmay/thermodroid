package com.example.thermodroid;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import android.util.Log;

public class ThermoFrame {

    private FloatBuffer temps;
    public ThermoFrame(IntBuffer frame) {
        temps = FloatBuffer.allocate(frame.limit());
        frame.rewind();
        //frame.compact();
        temps.clear();
        while(frame.hasRemaining()) {
            int[] f = new int[4];
            frame.get(f, 0, 4);
            
            int intbits = 0;
            intbits = (f[3] << 24 | f[2] << 16 | f[1] << 8 | f[0]);
            temps.put(Float.intBitsToFloat(intbits));
        } 
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
