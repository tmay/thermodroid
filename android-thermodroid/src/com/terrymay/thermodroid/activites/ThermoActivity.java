package com.terrymay.thermodroid.activites;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

import com.terrymay.thermodroid.R;
import com.terrymay.thermodroid.models.ThermoFrame;
import com.terrymay.thermodroid.views.ThermoView;

public class ThermoActivity extends Activity implements Runnable {
    private final String TAG = ThermoActivity.class.getSimpleName();
    
    //IDs
    final private static UUID THERMO_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    //protocol
    final private static byte START_FLAG = 0x12;
    final private static byte END_FLAG = 0x13;
    final private static byte ESCAPE = 0x7D;
    
    //arduino states
    final private static byte READY = 0x01;
    final private static byte IDLE = 0x02;
    final private static byte RUN = 0x03;
    
    //handler ids
    final private static int NEW_FRAME = 100;
    

/*
            ThermoActivity.this.updateReceivedData(data);
            ThermoActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //
                }
            });
*/

    private ThermoView view;
    private String mDeviceID;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private BluetoothDevice mDevice;
    
    boolean escaped = false;
    
    int frameByteCount = 0;
    int bytesRead = 0;

    boolean writeToFrame = false;
    private int[] ints = new int[512];
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new ThermoView(this);
        view.setBackgroundColor(Color.BLACK);
        setContentView(view);
        
        Intent i = this.getIntent();
        mDeviceID = i.getStringExtra("bt_address");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mDevice = mBluetoothAdapter.getRemoteDevice(mDeviceID);
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
        try {
            mmSocket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        
        
        try {
            openConnection(mDevice.createInsecureRfcommSocketToServiceRecord(THERMO_UUID));
        } catch (IOException e) {
            Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        try {
            mmSocket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void openConnection(BluetoothSocket socket) {
        mmSocket = socket;
        
        try {
            mmSocket.connect();
            mmInStream = mmSocket.getInputStream();
            mmOutStream = mmSocket.getOutputStream();
            
            Thread thread = new Thread(null, this, "thermodroid");
            thread.start();
        } catch (IOException e) {
            Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
        }
        
    }
    
    private void sendCommand(byte flag) {
        byte[] command = new byte[1];
        command[0] = flag;
        try {
            mmOutStream.write(command);
        } catch (IOException e) {
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            IntBuffer buff = IntBuffer.wrap((int[]) msg.obj, 0, msg.arg1);
            
            ThermoFrame frame = new ThermoFrame(buff);
            view.update(frame);
        }
    };
    


    @Override
    public void run() {
        //final String message = "Read " + data.length + " bytes: \n"
                //+ HexDump.dumpHexString(data) + "\n\n";
       // Log.i(TAG, message); 
        
       
        try {
            
            while(true) {
               // Log.i("bytes", Integer.toString(mmInStream.available()));
                //int b = inBuffer.get() & 0xFF;
                int b = mmInStream.read() & 0xff;
                //Log.i("int",Integer.toString(b));
                if (escaped) {
                    //Log.i("frame","escaped");
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
                    //Log.i("frame", "END");
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
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
