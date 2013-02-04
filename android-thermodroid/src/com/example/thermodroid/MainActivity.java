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
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity implements Runnable {

    final private static byte DUMP_EEPROM   =   100;
    
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
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        output = (TextView) findViewById(R.id.output);
        updateOutput("startup\n");
        eepromData = new EEPROMData();
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
            updateOutput("File Descriptor is not null");
            sendCommand(DUMP_EEPROM, (byte) 2 , 3);
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
    
    
    public void run() {
        Header header = new Header();
        int ret = 0;
        //byte[] buffer = new byte[16384];
        byte[] buffer = new byte[16384];
        byte[] headerBuffer = new byte[4];
        int i;
        
        //when ret is -1 there no more data 
        try {
            mInputStream.read(headerBuffer, 0, 4);
            header = new Header(headerBuffer);
            updateOutput(header.toString());
            buffer = new byte[header.getFrameSize()];
            mInputStream.read(buffer, 0, header.getFrameSize());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        switch(header.getCommand()) {
            case DUMP_EEPROM:
                eepromData = new EEPROMData(buffer); 
                updateOutput(eepromData.toString());
                break;
        }
        
        /*
        while (ret >= 0) {
            try {

                //ret = mInputStream.read(buffer, 4, header.getFrameSize());
            } catch (IOException e) {
                updateOutput("IO EXCEPTION");
                updateOutput(e.getMessage());
                break;
                
            }

            i = 0;
            while (i < ret) {
                int len = ret - i;
                int value = (int)buffer[i];
                updateOutput(Integer.toHexString(i)+":"+Integer.toString(value));
                i += 1;
               
                switch(header.getCommand()) {
                  case DUMP_EEPROM:
                      int value = (int)buffer[i];
                      //updateOutput(Integer.toHexString(i)+":"+Integer.toString(value));
                      eepromData.addRegister(i, value);
                      break;
                }
                i += 1;
                
                if (len >= 1) {
                    Message m = Message.obtain(mHandler);
                    int value = (int)buffer[i];
                    //m.obj = new EEPROMData("f", value);
                    updateOutput(Integer.toHexString(i)+":"+Integer.toString(value));
                    //mHandler.sendMessage(m);
                }
                
                //i += 1; num of bytes from arduino
            }

        }*/
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
