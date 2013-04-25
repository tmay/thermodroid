package com.terrymay.thermodroid.activites;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.terrymay.thermodroid.R;

public class MainActivity extends Activity {
    private final static int REQUEST_ENABLE_BT = 0x1;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BTDevice> mPairedDeviceList;
    private Set<BluetoothDevice> mPairedDevices;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        
        initPairedDeviceList();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                initPairedDeviceList();
            } else if (resultCode == RESULT_CANCELED) {
                //user said no.
            }
                
        }
    };
    
    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
 
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    private void initPairedDeviceList() {
        findPairedDevices();
        ListView pairedDeviceListView = (ListView)findViewById(R.id.list_paired_devices);
        DeviceArrayAdapter pairedDeviceAdapter = new DeviceArrayAdapter(this, android.R.layout.simple_list_item_1, mPairedDeviceList);
        
        pairedDeviceListView.setAdapter(pairedDeviceAdapter);
        pairedDeviceListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mBluetoothAdapter.cancelDiscovery();
                Intent i = new Intent(MainActivity.this, ThermoActivity.class);
                i.putExtra("bt_address", mPairedDeviceList.get(arg2).getAddress());
                startActivity(i);
            }
            
        });
    }
    
    private void findPairedDevices() {
     mPairedDeviceList = new ArrayList<BTDevice>();
     mPairedDevices = mBluetoothAdapter.getBondedDevices();
     // If there are paired devices
     if (mPairedDevices.size() > 0) {
         // Loop through paired devices
         for (BluetoothDevice device : mPairedDevices) {
             // Add the name and address to an array adapter to show in a ListView
             mPairedDeviceList.add(new BTDevice(device));
         }
     }
    }
    
    private class BTDevice {
        
        private BluetoothDevice mDevice;
        
        public BTDevice(BluetoothDevice device) {
            mDevice = device;
        }
        
        public String getName() {
            return mDevice.getName();
        }
        
        public String getAddress() {
            return mDevice.getAddress();
        }
        
        public BluetoothDevice getDevice() {
            return mDevice;
        }
        
        public String toString() {
            return mDevice.getAddress();
        }
    }
    
    //list item
    private class DeviceArrayAdapter extends ArrayAdapter<BTDevice> {

        HashMap<BTDevice, Integer> mIdMap = new HashMap<BTDevice, Integer>();

        public DeviceArrayAdapter(Context context, int textViewResourceId,
            List<BTDevice> objects) {
          super(context, textViewResourceId, objects);
          for (int i = 0; i < objects.size(); ++i) {
            mIdMap.put(objects.get(i), i);
          }
        }
        
        
        
        @Override
        public long getItemId(int position) {
          BTDevice item = getItem(position);
          return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
          return true;
        }

      }
}
