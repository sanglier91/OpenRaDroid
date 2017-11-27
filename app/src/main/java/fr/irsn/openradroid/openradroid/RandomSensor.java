package fr.irsn.openradroid.openradroid;


import android.os.Handler;
import android.util.Log;

import java.util.Random;

/**
 * Created by wilfried on 19/11/17.
 */

public class RandomSensor implements SensorInterface {

    private static String TAG = RandomSensor.class.getSimpleName ();

    private final Handler m_handler;
    private boolean m_stop = false;
    private double m_probability;
    private Thread m_thread;
    private RunRun m_runnable;

    private void send_log (String msg) {
        m_handler.sendMessage (m_handler.obtainMessage (1, msg));
    }

    private void send_error (String msg) {
        m_handler.sendMessage (m_handler.obtainMessage (2, msg));
    }

    private class RunRun implements Runnable {

        public void run () {

            send_log ("Sensor started");

            Random rnd = new Random ();

            try {

                while (m_stop == false) {

                    Thread.sleep (100);

                    if (rnd.nextDouble () > 0.3)
                        m_handler.sendEmptyMessage (0);
                }

            } catch (InterruptedException e) {
                e.printStackTrace ();
                send_error (e.getMessage ());
            }
            send_log ("Sensor stopped");
        }

        public void terminate () {
            m_stop = true;
        }
    }



    public RandomSensor (Handler handler) {
        m_handler = handler;
        m_probability = 0.5;
    }

    public void set_probability (double proba) {
        m_probability = proba;
        if (m_probability < 0)
            m_probability = 0;
        if (m_probability > 1)
            m_probability = 1;
    }

    @Override
    public boolean is_started () {
        if (m_thread == null)
            return false;
        return  m_thread.isAlive ();
    }

    @Override
    public void start () {
        if (m_thread != null) {
            return;
        }
        m_runnable = new RunRun ();
        m_thread = new Thread (m_runnable);
        m_thread.start ();
    }

    @Override
    public void stop () {
        if (m_thread == null)
            return;
        m_runnable.terminate ();
        try {
            m_thread.join ();
        } catch (InterruptedException e) {
            e.printStackTrace ();
            send_error (e.getMessage ());
        }
        m_thread = null;
        m_runnable = null;
        send_log ("Sensor thread terminated");
    }
}
