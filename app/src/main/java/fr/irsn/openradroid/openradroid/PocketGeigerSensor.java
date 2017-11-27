package fr.irsn.openradroid.openradroid;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

/**
 * Created by wilfried on 19/11/17.
 */

public class PocketGeigerSensor implements SensorInterface {

    private static String TAG = PocketGeigerSensor.class.getName ();
    private final Handler m_handler;
    private boolean m_stop = false;
    private Context m_context;
    private Thread m_thread;
    private RunRun m_runnable;

    private final static long NOISE_REJECT_DURATION = 200; // ms


    public PocketGeigerSensor (Handler handler, Context context) {
        m_handler = handler;
        m_context = context;
    }

    private void send_log (String msg) {
        m_handler.sendMessage (m_handler.obtainMessage (1, msg));
    }

    private void send_error (String msg) {
        m_handler.sendMessage (m_handler.obtainMessage (2, msg));
    }

    private class RunRun implements Runnable {

        @Override
        public void run () {

            UsbManager usbManager = (UsbManager) m_context.getSystemService (Context.USB_SERVICE);

            // Vendor Id and Product Id for Microchip Type 6
            ProbeTable customTable = new ProbeTable ();
            customTable.addProduct (1240, 62575, CdcAcmSerialDriver.class);

            // Find the above device
            UsbSerialProber prober = new UsbSerialProber (customTable);
            List<UsbSerialDriver> availableDrivers = prober.findAllDrivers (usbManager);
            if (availableDrivers.isEmpty ()) {
                send_log ("No device found");
                return;
            }

            // Get the first and only available driver
            UsbSerialDriver driver = availableDrivers.get (0);

            UsbDeviceConnection connection = usbManager.openDevice (driver.getDevice ());
            if (connection == null) {
                send_log ("Can't open the device");
                return;
            }

            UsbSerialPort port = driver.getPorts ().get (0);

            try {
                port.open (connection);
                port.setParameters (38400, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                byte buffer[] = new byte[100];
                int nb_bytes_read;
                long noise_rejection_until = 0;

                port.write ("S\n".getBytes (), 1000);
                send_log ("Start the device");

                while (m_stop == false) {

                    nb_bytes_read = port.read (buffer, 100);

                    if (noise_rejection_until != 0) {
                        Log.d (TAG, "noise reject !");
                        long current_time = System.currentTimeMillis ();
                        if (current_time > noise_rejection_until) {
                            noise_rejection_until = 0;
                        } else
                            continue;
                    }

                    if (nb_bytes_read != 6 || buffer[0] != '>' ||
                            (buffer[1] < '0' && buffer[1] > '9') ||
                            buffer[2] != ',' ||
                            (buffer[3] < '0' && buffer[3] > '9')) {
                        Log.d (TAG, "Bad frame format");
                        continue;
                    }

                    int signal = buffer[1] - '0';
                    int noise = buffer[3] - '0';

                    if (noise > 0) {
                        noise_rejection_until = System.currentTimeMillis () + NOISE_REJECT_DURATION;
                        continue;
                    }
                    if (signal > 0) {
                        m_handler.sendEmptyMessage (0);
                    }

                }

                send_log ("Stoping the device");
                port.write ("E\n".getBytes (), 1000);

                send_log ("Waiting for the last incoming data");
                boolean end;
                do {
                    nb_bytes_read = port.read (buffer, 100);
                    if (nb_bytes_read > 0) {
                        end = nb_bytes_read == 4 && buffer[0] == 'E' && buffer[1] == '\r';
                    } else {
                        end = true;
                        send_log ("Can't get End message from device, you should probably disconnect the device");
                    }
                } while (end == false);

                port.close ();
                send_log ("Device is stopped");

            } catch (IOException e) {
                e.printStackTrace ();
                send_error (e.getMessage ());
            }
        }

        public void terminate () {
            m_stop = true;
        }
    }


    public boolean is_started () {
        if (m_thread == null)
            return false;
        // https://developer.android.com/reference/java/lang/Thread.html#isAlive()
        // TODO see link
        return m_thread.isAlive ();
    }


    @Override
    public void start () {
        if (is_started ())
            return;
        m_runnable = new RunRun ();
        m_thread = new Thread (m_runnable);
        m_thread.start ();
    }

    @Override
    public void stop () {
        if (!is_started ())
            return;
        m_runnable.terminate ();
        try {
            m_thread.join ();
        } catch (InterruptedException e) {
            e.printStackTrace ();
        }
        m_thread = null;
        m_runnable = null;
        Log.i (TAG, "Thread is terminated");
    }
}
