package com.example.thermodroid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
    
    public short getFrameSize() {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(mSource[FRAME_SIZE_INDEX_H]);
        bb.put(mSource[FRAME_SIZE_INDEX_L]);
        return bb.getShort(0);
    }
    
    public String toString() {
        return "COMMAND: "+Integer.toString(getCommand())+" SIZE: "+Short.toString(getFrameSize());
    }
}
