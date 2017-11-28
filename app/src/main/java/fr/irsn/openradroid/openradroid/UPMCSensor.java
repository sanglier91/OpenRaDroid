package fr.irsn.openradroid.openradroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
    private final static UUID UUID_SERVICE = sixteenBitUuid(0x2220);

    private static UUID sixteenBitUuid (long shortUuid) {
        String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";
        assert shortUuid >= 0 && shortUuid <= 0xFFFF;
        return UUID.fromString(String.format(shortUuidFormat, shortUuid & 0xFFFF));
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver () {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_ON) {

            } else if (state == BluetoothAdapter.STATE_OFF) {

            }
        }
    };

    private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean ret = m_bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE;
            Log.d (TAG, String.valueOf (ret));
        }
    };


    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d (TAG, action);
        }
    };

    public UPMCSensor (Handler m_handler) {
        this.m_handler = m_handler;
    }


    @Override
    public void start () {

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        m_context.registerReceiver (scanModeReceiver, new IntentFilter (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        m_context.registerReceiver (bluetoothStateReceiver, new IntentFilter (BluetoothAdapter.ACTION_STATE_CHANGED));

        /*IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CONNECTED);
        filter.addAction(ACTION_DISCONNECTED);
        filter.addAction(ACTION_DATA_AVAILABLE);
        m_context.registerReceiver(rfduinoReceiver, filter);*/

        //bluetoothAdapter.isEnabled();

        m_bluetoothAdapter.startLeScan(
                new UUID[]{ UUID_SERVICE },
                UPMCSensor.this);

    }

    @Override
    public void stop () {
        m_bluetoothAdapter.stopLeScan (UPMCSensor.this);

        m_context.unregisterReceiver (scanModeReceiver);
        m_context.unregisterReceiver (bluetoothStateReceiver);
        m_context.unregisterReceiver (rfduinoReceiver);
    }

    @Override
    public boolean is_started () {
        return false;
    }

    @Override
    public void onLeScan (BluetoothDevice device, int rssi, byte[] scanRecord) {
        m_bluetoothAdapter.stopLeScan(this);
        m_bluetoothDevice = device;

        String str = new StringBuilder ()
                .append("Name: ").append(device.getName())
                .append("\nMAC: ").append(device.getAddress())
                .append("\nRSSI: ").append(rssi)
                //.append("\nScan Record:").append(parseScanRecord(scanRecord))
                .toString();

        Log.d (TAG, str);

        /*MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceInfoText.setText(
                        BluetoothHelper.getDeviceInfoText(bluetoothDevice, rssi, scanRecord));
                updateUi();
            }
        });*/
    }
}
