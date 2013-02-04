package com.example.thermodroid;

import java.util.TreeMap;


public class EEPROMData {

    //VTH0 of absolute temperature sensor
    //calculated from (Vth_H:0xDB(219) & Vth_L:0xDA(218))
    final private static int Vth_H = 0xD8;
    final private static int Vth_L = 0xDA;
    //KT1 of absolute temperature sensor
    //calculated from (Kt1_H:0xDD(221) & Kt1_L:0xDC(220))
    final private static int Kt1_H = 0xDD;
    final private static int Kt1_L = 0xDC;
    //KT2 of absolute temperature sensor
    //calculated from (Kt2_H:0xDF(223) & Kt2_L:0xDE(222))
    final private static int Kt2_H = 0xDF;
    final private static int Kt2_L = 0xDE;

    
    private TreeMap<Integer, Integer> data;

    public EEPROMData() {
    
    }
    
    public EEPROMData(byte[] source) {
      data = new TreeMap<Integer, Integer>(); 
      
      for (int i = 0; i < source.length; i++) {
          addRegister(i, source[i]);
      }
    }

    public String toString() {
        String res = "";
        for (int i=0; i < data.size(); i++) {
            res += "Register: "+Integer.toString(i)+" Value: "+Integer.toString(data.get(i));
            res += "\n";
        }
        return res;
    }
    //TODO: check datasheet to verify int is a good type to use here
    public void addRegister(int address, int value) {
        data.put(address, value);
    }
    
    public void getRegister(int address) {
        data.get(address);
    }
    
    public int getV_th() {
        return (data.get(Vth_H) <<8) + data.get(Vth_L);
    }
    
    public double getK_t1() {
        return (data.get(Kt1_H) <<8) + data.get(Kt1_L)/1024.0;
    }
    
    public double getK_t2() {
        return (data.get(Kt2_H) <<8) + data.get(Kt2_L)/1048576.0;
    }
    
    //(0xD4:212)Compensation pixel individual offset coefficients
    public int getA_cp() {
        int a_cp = data.get(212);
        return (a_cp > 127) ? a_cp - 256 : a_cp;
    }
    
    //(0xD5:213)Individual Ta dependence (slope) of the compensation pixel offset
    public int getB_cp() {
        int b_cp = data.get(213);
        return (b_cp > 127) ? b_cp - 256 : b_cp;
    }
    
  //(0xD8:216)Thermal gradient coefficient
    public int getTGC() {
      int tgc = data.get(216);
      return (tgc > 127) ? tgc - 256 : tgc;
    }
    
    //(0xD9:217)Scaling coefficient for slope of IR pixels offset
    public int getB_I_Scale() {
        //unsigned
        return data.get(217);
    }
    
    public double getEmissivity() {
        return (data.get(229) <<8) + data.get(228)/32768.0;
    }
    
    //(0x00...0x3F:0...63)IR pixel individual offset coefficient
    public int[] getA_IJ() {
      int[] a_ij = new int[64];
      for (int i=0; i<=63;i++) {
          int b = data.get(i);
          a_ij[i] = (b > 127) ? b - 256 : b;
      }
      return a_ij;
    }
    
    //(0x40...0x7F:64...127)Individual Ta dependence (slope) of IR pixels offset
    public int[] getB_IJ() {
        int[] b_ij = new int[64];
        for (int i=64; i<=127;i++) {
            int b = data.get(i);
            b_ij[i] = (b > 127) ? b - 256 : b;
        }
        return b_ij;
      }
}
