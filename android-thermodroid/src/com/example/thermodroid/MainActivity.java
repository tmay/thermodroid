package com.example.thermodroid;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity implements Runnable {

    //prtocol
    final private static byte START_FLAG = 0x12;
    final private static byte END_FLAG = 0x13;
    final private static byte ESCAPE = 0x7D;
    
    final private static byte DUMP_EEPROM   =   100;
    final private static byte WRITE_TRIM    =   101;
    final private static byte ACK           =   104; //10-4 good buddy!
    final private static byte READY         =   0x00;
    private UsbManager mUsbManager;
    
    private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
    private PendingIntent mPermissionIntent;
    private UsbAccessory mAccessory;
    private ParcelFileDescriptor mFileDescriptor;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;

    private boolean mPermissionRequestPending;
    
    private TextView output;
    private String outputBuffer = "";

    private int mCommand;
    
    private EEPROMData eepromData;
    private ThermoView view;
    
    protected class IRDataMsg {
        private byte[] data;
        public IRDataMsg(byte[] data) {
            this.data = data;
        }
        
        
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new ThermoView(this);
        view.setBackgroundColor(Color.WHITE);
        //setContentView(view);
        setContentView(R.layout.activity_main);
        
        output = (TextView) findViewById(R.id.output);
        updateOutput("startup\n");
        
        initUSB();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mAccessory != null) {
            return mAccessory;
        } else {
            return super.onRetainNonConfigurationInstance();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
 
 
        if (mInputStream != null && mOutputStream != null) {
            //streams were not null");
            return;
        }
        //streams were null");
        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            } else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            // null accessory
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
 
    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }
    
    private void initUSB() {
        outputBuffer = "";
        updateOutput("init usb");
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    private void openAccessory(UsbAccessory accessory) {
        updateOutput("openaccessory");
        
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            
            mOutputStream = new FileOutputStream(fd);
            Thread thread = new Thread(null, this, "ThermoDroid");
            thread.start();
            updateOutput("Connect");
            init();
        } else {
            
            updateOutput("File Descriptor is null");
        }
    }
    
    private void closeAccessory() {
        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
                updateOutput("Closed");
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }
    
    public void init() {
      sendCommand(READY, (byte) 0 , 0);
    }
        
    public void run() {
        /*
        byte[] buffer = new byte[16384];
        byte[] frame = new byte[512];
        int bytesAvailable = 0;
        int frameByteCount = 0;
        int bytesRead = 0;
        boolean escaped = false;
        
    main:
        while (bytesAvailable >= 0) {
            try {
                bytesAvailable = mInputStream.available(); 
                updateOutput(Integer.toString(bytesAvailable));
            } catch (IOException e) {
                
                break main;
            }         
            bytesRead = 0;
            
        
            while(bytesRead < bytesAvailable) {
                updateOutput(Integer.toString(bytesRead));
                byte b = buffer[bytesRead];
                updateOutput(Integer.toString(frameByteCount)+" : "+Integer.toString(b & 0xff));
                if (escaped) {
                    frame[frameByteCount++] = b;
                    escaped = false;
                    bytesRead += 1;
                    continue;
                }

                switch (b) {
                case ESCAPE:
                    escaped = true;
                    bytesRead += 1;
                    break;
                case START_FLAG:
                    updateOutput("new Frame");
                    frameByteCount = 0;
                    frame = new byte[512];
                    bytesRead += 1;
                    break;
                case END_FLAG:
                    //this is where the handler needs to be called with a complete frame
                    //mHandler.
                    clearOutput();
                    break;
                default:
                    frame[frameByteCount++] = b;
                    bytesRead += 1;
                }
            }
        }
        */
    }


    
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            EEPROMData data = (EEPROMData) msg.obj;
            //Log.i("DATA", t.getReading());
            //updateOutput(Integer.toString(data.getReading()));
        }
    };
    
    public void sendCommand(byte command, byte target, int value) {
        mCommand = command;
        byte[] buffer = new byte[3];
        if (value > 255)
            value = 255;

        buffer[0] = command;
        buffer[1] = target;
        buffer[2] = (byte) value;
        if (mOutputStream != null && buffer[1] != -1) {
            try {
                mOutputStream.write(buffer);
            } catch (IOException e) {
                updateOutput("send command failed");
            }
        }
    }
    
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            updateOutput("ask perms");
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        updateOutput("open accessory");
                        openAccessory(accessory);
                    } else {
                        updateOutput("USB PERMS DENIED\n\n");
                    }
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null && accessory.equals(mAccessory)) {
                    updateOutput("close accessory");
                    closeAccessory();
                }
            }
        }
    };
  
    private void clearOutput() {
        runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                outputBuffer = "";
                output.setText(outputBuffer);        
            }
        });
    }
    
    private void updateOutput(final String msg) {
        runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                outputBuffer += msg + "\n";
                output.setText(outputBuffer);                
            }
        });
        
    }
}
