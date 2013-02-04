package com.example.thermodroid;

public class Header {
    final private int COMMAND_INDEX         =   1;
    final private int FRAME_SIZE_INDEX_H    =   2;
    final private int FRAME_SIZE_INDEX_L    =   3;
    
    private byte[] mSource;
    
    public Header() {

    }
    
    public Header(byte[] source) {
        mSource = source;
    }

    public byte getCommand() {
        return mSource[COMMAND_INDEX];
    }
    
    public int getFrameSize() {
        return (mSource[FRAME_SIZE_INDEX_L] << 8) + mSource[FRAME_SIZE_INDEX_H];
    }
    
    public String toString() {
        return "COMMAND: "+Integer.toString(getCommand())+" SIZE: "+Integer.toString(getFrameSize());
    }
}
