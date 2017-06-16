package com.joaquinalan.blluetoothmadeformyself;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.joaquinalan.blluetoothmadeformyself.bluetooth.BluetoothService;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, BluetoothDevicesAdapter.ListItemClickListener {
    private static final int REQUEST_ENABLE_BT = 0;
    private static final String TAG = "MainActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private Button mButtonEnable;
    private Button mButtonSendMessage;
    private Button mButtonOnVisibility;
    private RecyclerView mRecyclerViewDevices;
    private ArrayList<BluetoothDevice> mDiscoveredDevices = new ArrayList<>();
    private BluetoothDevicesAdapter mBluetoothDevicesAdapter;
    private BluetoothService mBluetoothConnection;
    private BluetoothDevice mBluetoothDevice;
    private EditText mEditTextMessageToSend;
    private TextView mTextViewInbox;

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastDeviceFound = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                // Add the name and address to an array adapter to show in a ListView
                mDiscoveredDevices.add(device);
                mBluetoothDevicesAdapter.addDevice(mDiscoveredDevices);
            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastBond = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBluetoothDevice = device;
                }
                //case2: creating a bone
                if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            byte[] readBuf = (byte[]) msg.obj;
            int numberOfBytes = msg.arg1;

            // construct a string from the valid bytes in the buffer
            String readMessage = new String(readBuf, 0, numberOfBytes);
            mTextViewInbox.setText(readMessage);
            Log.d(TAG, readMessage);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonEnable = (Button) findViewById(R.id.button_main_enablebluetooth);
        mButtonSendMessage = (Button) findViewById(R.id.button_main_sendmessage);
        mButtonOnVisibility = (Button) findViewById(R.id.button_main_onvisibility);
        mRecyclerViewDevices = (RecyclerView) findViewById(R.id.reciclerview_main);
        mEditTextMessageToSend = (EditText) findViewById(R.id.edittext_main_messagetosend);
        mTextViewInbox = (TextView) findViewById(R.id.textview_main_inbox);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mButtonEnable.setOnClickListener(this);
        mButtonSendMessage.setOnClickListener(this);
        mButtonOnVisibility.setOnClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerViewDevices.setLayoutManager(layoutManager);
        mRecyclerViewDevices.setHasFixedSize(true);

        mDiscoveredDevices = new ArrayList<>();
        mBluetoothDevicesAdapter = new BluetoothDevicesAdapter(mDiscoveredDevices, this);

        mRecyclerViewDevices.setAdapter(mBluetoothDevicesAdapter);

        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastDeviceFound, discoverDevicesIntent);
        mBluetoothAdapter.startDiscovery();

        //showPairedDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastDeviceFound);
        unregisterReceiver(mBroadcastBond);
        mBluetoothConnection.stop();
    }

//    public void showPairedDevices() {
//        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//// If there are paired devices
//        if (pairedDevices.size() > 0) {
//            // Loop through paired devices
//            mPairedDevices = new ArrayList<>();
//            mPairedDevices.addAll(pairedDevices);
//            mBluetoothDevicesAdapter = new BluetoothDevicesAdapter(mPairedDevices, this);
//        }
//        mRecyclerViewDevices.setAdapter(mBluetoothDevicesAdapter);
//    }

    private void enableBluetooth() {
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            //showPairedDevices();
        }
    }

    public void onVisibility() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    public void onDiscovery() {
        mBluetoothAdapter.startDiscovery();
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastDeviceFound, filter); // Don't forget to unregister during onDestroy
    }

    public void bondDevice(BluetoothDevice device) {
//Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastBond, filter);
        device.createBond();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_main_enablebluetooth:
                enableBluetooth();
                break;
            case R.id.button_main_onvisibility:
                onVisibility();
                break;
            case R.id.button_main_sendmessage:
                sendMessage();
                break;
        }
    }

    private void sendMessage() {
        byte[] bytes = mEditTextMessageToSend.getText().
                toString().getBytes(Charset.defaultCharset());
        mBluetoothConnection.write(bytes);
    }


    @Override
    public void onListItemClick(int clickedItemIndex) {
        //first cancel discovery because its very memory intensive.
        //mBluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = mDiscoveredDevices.get(clickedItemIndex);
        //BluetoothDevice device = mPairedDevices.get(clickedItemIndex);

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = device.getName();
        String deviceAddress = device.getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        mBluetoothConnection = new BluetoothService(mHandler);
        Log.d(TAG, "Connecting with " + device.getName());
        //device.createBond();
        mBluetoothConnection.startClient(device);
    }
}


