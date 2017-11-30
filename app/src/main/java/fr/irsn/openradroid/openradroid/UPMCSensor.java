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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.Vector;

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
    public final static UUID UUID_SEND = sixteenBitUuid(0x2222);
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

    private void send_log (String msg) {
        m_handler.sendMessage (m_handler.obtainMessage (1, msg));
    }

    private void send_error (String msg) {
        m_handler.sendMessage (m_handler.obtainMessage (2, msg));
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
                send_log ("Connected to RFduino.");
                m_bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                send_log ("Disconnected from RFduino.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                send_error ("Services discovered failed!");
                return;
            }

            m_bluetoothGattService = gatt.getService(UUID_SERVICE);
            if (m_bluetoothGattService == null) {
                send_error ("RFduino GATT service not found!");
                return;
            }

            BluetoothGattCharacteristic receiveCharacteristic = m_bluetoothGattService.getCharacteristic(UUID_RECEIVE);
            if (receiveCharacteristic == null) {
                send_error ("RFduino receive characteristic not found!");
                return;
            }

            BluetoothGattDescriptor receiveConfigDescriptor = receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);

            if (receiveConfigDescriptor == null) {
                send_error ("RFduino receive config descriptor not found!");
            }

            gatt.setCharacteristicNotification (receiveCharacteristic, true);
            receiveConfigDescriptor.setValue (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor (receiveConfigDescriptor);

            send_log ("Service discovered");
        }

        @Override
        public void onCharacteristicRead (BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
        }

        @Override
        public void onCharacteristicChanged (BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic) {

            byte [] data = characteristic.getValue();

            if (data.length == 0)
                return;

            if (data[0] == 0x3) {
                int len = (int)data[1];
                StringBuffer str_buff = new StringBuffer(len);
                for (int i = 0; i < len; i++)
                    str_buff.append ((char)data[i+2]);
                send_log("Sensor type = " + str_buff);
            }

            if (data[0] == 0x10) {
                int len = (int)data[1];
                StringBuffer str_buff = new StringBuffer(len);
                for (int i = 0; i < len; i++)
                    str_buff.append ((char)data[i+2]);
                send_log("Tybe type = " + str_buff);
            }
        }
    };


    public UPMCSensor (Handler handler, Context context) {
        m_handler = handler;
        m_context = context;
    }


    public void connect () {

        m_BluetoothManager = (BluetoothManager) m_context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (m_BluetoothManager == null) {
            send_error ("Unable to initialize BluetoothManager.");
            return;
        }

        m_bluetoothAdapter = m_BluetoothManager.getAdapter();
        if (m_bluetoothAdapter == null) {
            send_error ("Unable to obtain a BluetoothAdapter.");
            return;
        }

        if (m_bluetoothAdapter.isEnabled() == false) {
            send_error ("BLE not enable");
            return;
        }

        m_bluetoothAdapter.startLeScan(new UUID[]{ UUID_SERVICE },UPMCSensor.this);
    }

    @Override
    public void start () {

        //Log.d (TAG, "start");

        BluetoothGattCharacteristic characteristic = m_bluetoothGattService.getCharacteristic (UUID_SEND);
        byte [] data = {0x12};
        characteristic.setValue (data);
        characteristic.setWriteType (BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        boolean ret = m_bluetoothGatt.writeCharacteristic (characteristic);
        //Log.d (TAG, "Send return = "+ ret);
    }

    @Override
    public void stop () {
        m_bluetoothAdapter.stopLeScan (UPMCSensor.this);

        m_bluetoothGatt.disconnect();
        m_bluetoothGatt.close();
        m_bluetoothGatt = null;
    }

    @Override
    public boolean is_started () {
        return false;
    }

    @Override
    public void onLeScan (BluetoothDevice device, int rssi, byte[] scanRecord) {
        m_bluetoothAdapter.stopLeScan(this);
        m_bluetoothDevice = device;

        send_log (getDeviceInfoText(m_bluetoothDevice, rssi, scanRecord));
        //final BluetoothDevice device = m_bluetoothAdapter.getRemoteDevice (m_bluetoothDevice.getAddress());
        m_bluetoothGatt = m_bluetoothDevice.connectGatt (m_context, false, mGattCallback);
    }
}
