package fr.irsn.openradroid.openradroid;

import android.os.Handler;

/**
 * Created by wilfried on 19/11/17.
 */

public class UPMCSensorRunnable implements Runnable {

    private final Handler m_handler;
    private boolean m_stop = false;

    public UPMCSensorRunnable (Handler m_handler) {
        this.m_handler = m_handler;
    }


    public void terminate () {
        m_stop = true;
    }

    public void run () {


    }
}
