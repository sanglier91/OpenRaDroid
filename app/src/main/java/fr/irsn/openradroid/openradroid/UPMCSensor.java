package fr.irsn.openradroid.openradroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import java.util.UUID;

/**
 * Created by wilfried on 19/11/17.
 */

public class UPMCSensor implements SensorInterface, BluetoothAdapter.LeScanCallback {

    private static String TAG = UPMCSensor.class.getName ();

    private final Handler m_handler;
    private Context m_context;
    private boolean m_stop = false;

    private BluetoothAdapter m_bluetoothAdapter;
    private BluetoothDevice m_bluetoothDevice;
    private BluetoothManager m_BluetoothManager;
    private BluetoothGattService m_bluetoothGattService;
    private BluetoothGatt m_bluetoothGatt;

    private final static UUID UUID_SERVICE = sixteenBitUuid(0x2220);
    public final static UUID UUID_RECEIVE = sixteenBitUuid(0x2221);
    public final static UUID UUID_CLIENT_CONFIGURATION = sixteenBitUuid(0x2902);

    private static UUID sixteenBitUuid (long shortUuid) {
        String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";
        assert shortUuid >= 0 && shortUuid <= 0xFFFF;
        return UUID.fromString(String.format(shortUuidFormat, shortUuid & 0xFFFF));
    }

    private static String getDeviceInfoText (BluetoothDevice device, int rssi, byte[] scanRecord) {
        return new StringBuilder()
                .append("Name: ").append(device.getName())
                .append("\nMAC: ").append(device.getAddress())
                .append("\nRSSI: ").append(rssi)
                .append("\nScan Record:").append(parseScanRecord(scanRecord))
                .toString();
    }

    // Bluetooth Spec V4.0 - Vol 3, Part C, section 8
    private static String parseScanRecord (byte[] scanRecord) {
        StringBuilder output = new StringBuilder();
        int i = 0;
        while (i < scanRecord.length) {
            int len = scanRecord[i++] & 0xFF;
            if (len == 0) break;
            switch (scanRecord[i] & 0xFF) {
                // https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
                case 0x0A: // Tx Power
                    output.append("\n  Tx Power: ").append(scanRecord[i+1]);
                    break;
                case 0xFF: // Manufacturer Specific data (RFduinoBLE.advertisementData)
                    output.append("\n  Advertisement Data: ")
                            .append(HexAsciiHelper.bytesToHex(scanRecord, i + 3, len));

                    String ascii = HexAsciiHelper.bytesToAsciiMaybe(scanRecord, i + 3, len);
                    if (ascii != null) {
                        output.append(" (\"").append(ascii).append("\")");
                    }
                    break;
            }
            i += len;
        }
        return output.toString();
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to RFduino.");
                Log.i(TAG, "Attempting to start service discovery:" + m_bluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from RFduino.");
                //broadcastUpdate(ACTION_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                m_bluetoothGattService = gatt.getService(UUID_SERVICE);
                if (m_bluetoothGattService == null) {
                    Log.e (TAG, "RFduino GATT service not found!");
                    return;
                }

                BluetoothGattCharacteristic receiveCharacteristic = m_bluetoothGattService.getCharacteristic(UUID_RECEIVE);
                if (receiveCharacteristic != null) {
                    BluetoothGattDescriptor receiveConfigDescriptor = receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
                    if (receiveConfigDescriptor != null) {
                        gatt.setCharacteristicNotification(receiveCharacteristic, true);

                        receiveConfigDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(receiveConfigDescriptor);
                    } else {
                        Log.e (TAG, "RFduino receive config descriptor not found!");
                    }

                } else {
                    Log.e (TAG, "RFduino receive characteristic not found!");
                }

                //broadcastUpdate(ACTION_CONNECTED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver () {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            Log.d (TAG, "bluetoothStateReceiver = " + state);
            if (state == BluetoothAdapter.STATE_ON) {

            } else if (state == BluetoothAdapter.STATE_OFF) {

            }
        }
    };

    private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean ret = m_bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE;
            Log.d (TAG, "ScanMode : " + String.valueOf (ret));
        }
    };


    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d (TAG, action);
        }
    };

    public UPMCSensor (Handler handler, Context context) {
        m_handler = handler;
        m_context = context;
    }


    @Override
    public void start () {

        Log.d (TAG, "start");


        m_BluetoothManager = (BluetoothManager) m_context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (m_BluetoothManager == null) {
            Log.e (TAG, "Unable to initialize BluetoothManager.");
            return;
        }

        m_bluetoothAdapter = m_BluetoothManager.getAdapter();
        if (m_bluetoothAdapter == null) {
            Log.e (TAG, "Unable to obtain a BluetoothAdapter.");
            return;
        }

        if (m_bluetoothAdapter.isEnabled() == false) {
            Log.d (TAG, "BLE not enable");
            return;
        }

        //m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        m_context.registerReceiver (scanModeReceiver, new IntentFilter (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        m_context.registerReceiver (bluetoothStateReceiver, new IntentFilter (BluetoothAdapter.ACTION_STATE_CHANGED));

        /*IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CONNECTED);
        filter.addAction(ACTION_DISCONNECTED);
        filter.addAction(ACTION_DATA_AVAILABLE);
        m_context.registerReceiver(rfduinoReceiver, filter);*/



        m_bluetoothAdapter.startLeScan(
                new UUID[]{ UUID_SERVICE },
                UPMCSensor.this);

    }

    @Override
    public void stop () {
        m_bluetoothAdapter.stopLeScan (UPMCSensor.this);

        m_bluetoothGatt.disconnect();
        m_bluetoothGatt.close();
        m_bluetoothGatt = null;

        m_context.unregisterReceiver (scanModeReceiver);
        m_context.unregisterReceiver (bluetoothStateReceiver);
        //m_context.unregisterReceiver (rfduinoReceiver);
    }

    @Override
    public boolean is_started () {
        return false;
    }

    @Override
    public void onLeScan (BluetoothDevice device, int rssi, byte[] scanRecord) {
        m_bluetoothAdapter.stopLeScan(this);
        m_bluetoothDevice = device;



        Log.d (TAG, getDeviceInfoText(m_bluetoothDevice, rssi, scanRecord));

        Log.d (TAG, m_bluetoothDevice.getAddress());

        //final BluetoothDevice device = m_bluetoothAdapter.getRemoteDevice (m_bluetoothDevice.getAddress());

        m_bluetoothGatt = device.connectGatt(m_context, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");


    }
}
