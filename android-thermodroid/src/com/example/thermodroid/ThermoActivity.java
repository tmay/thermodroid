package com.example.thermodroid;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class ThermoActivity extends Activity {
    private final String TAG = ThermoActivity.class.getSimpleName();
    
    //prtocol
    final private static byte START_FLAG = 0x12;
    final private static byte END_FLAG = 0x13;
    final private static byte ESCAPE = 0x7D;
    
    //arduino states
    final private static byte READY = 0x01;
    final private static byte IDLE = 0x02;
    final private static byte RUN = 0x03;
    
    //handler ids
    final private static int NEW_FRAME = 100;
    
    private UsbSerialDriver mSerialDevice;
    private UsbManager mUsbManager;
    
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            ThermoActivity.this.updateReceivedData(data);
            ThermoActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //
                }
            });
        }
    };

    private ThermoView view;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new ThermoView(this);
        view.setBackgroundColor(Color.BLACK);
        setContentView(view);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        menu.add(1, 1, 1, "Ready").setOnMenuItemClickListener(new OnMenuItemClickListener() {
            
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                sendCommand(READY);
                return false;
            }
        });
        
        menu.add(1, 2, 2, "Idle").setOnMenuItemClickListener(new OnMenuItemClickListener() {
            
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                sendCommand(IDLE);
                return false;
            }
        });
        
        menu.add(1, 1, 3, "Run").setOnMenuItemClickListener(new OnMenuItemClickListener() {
            
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                sendCommand(RUN);
                return false;
            }
        });
        return true;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (mSerialDevice != null) {
            try {
                mSerialDevice.close();
            } catch (IOException e) {
                // Ignore.
            }
            mSerialDevice = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSerialDevice = UsbSerialProber.acquire(mUsbManager);
        Log.d(TAG, "Resumed, mSerialDevice=" + mSerialDevice);
        if (mSerialDevice == null) {
            Log.i(TAG, "No Serial Device");
        } else {
            try {
                mSerialDevice.open();
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                try {
                    mSerialDevice.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                mSerialDevice = null;
                return;
            }
            Log.i(TAG, "Serial device: "+mSerialDevice);
        }
        onDeviceStateChange();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (mSerialDevice != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(mSerialDevice, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }
    
    private void sendCommand(byte flag) {
        byte[] command = new byte[1];
        command[0] = flag;
        try {
            mSerialDevice.write(command, 1);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }
    
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            IntBuffer buff = IntBuffer.wrap((int[]) msg.obj, 0, msg.arg1);
            
            ThermoFrame frame = new ThermoFrame(buff);
            view.update(frame);
        }
    };
    
    boolean escaped = false;
    
    int frameByteCount = 0;
    int bytesRead = 0;

    boolean writeToFrame = false;
    private int[] ints = new int[512];

    private void updateReceivedData(byte[] data) {
        //final String message = "Read " + data.length + " bytes: \n"
                //+ HexDump.dumpHexString(data) + "\n\n";
       // Log.i(TAG, message);        
        
        ByteBuffer inBuffer = ByteBuffer.wrap(data);
        
        while(inBuffer.hasRemaining()) {
            
            int b = inBuffer.get() & 0xFF;
            //Log.i("int",Integer.toString(b));
            if (escaped) {
                Log.i("frame","escaped");
                //frame.put(b);
                ints[frameByteCount++] = b;
                escaped = false;
                continue;
            }
            
            switch (b) {
            case ESCAPE:
                escaped = true;
                break;
            case START_FLAG:
                //frame.clear();
                frameByteCount = 0;
                ints = new int[512];
                //we didn't actually read a byte
                //but we used this byte to mark a new frame
                break;
            case END_FLAG:
                Log.i("frame", "END");
                //frame.flip();
                Message m = Message.obtain(mHandler, NEW_FRAME);
                m.obj = ints;
                m.arg1 = frameByteCount;
                mHandler.sendMessage(m);
                //frame.clear();
                //we really didn't read a byte
                //but we have to push the counter
                break;
            default:
                ints[frameByteCount++] = b;
                //frame.put(b);
            }
        }
        
    }

    public IntBuffer cloneIntBuffer(IntBuffer original) {
        IntBuffer clone = IntBuffer.allocate(original.capacity());
        original.rewind();//copy from the beginning
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }

}
