package fr.irsn.openradroid.openradroid;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class UPMCSensorActivity extends AppCompatActivity {

    private UPMCSensor m_sensor;
    private int m_nb_counts;
    private TextView f_nb_counts;
    private TextView f_log;

    private final Handler m_handler = new Handler () {
        @Override
        public void handleMessage (Message msg) {
            super.handleMessage (msg);

            if (msg.what == 0) {
                m_nb_counts++;
                f_nb_counts.setText (String.valueOf (m_nb_counts));
            }

            if (msg.what == 1 || msg.what == 2) {
                f_log.append (msg.obj + "\n");
            }
        }
    };

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_upmcsensor);
        f_log = findViewById (R.id.f_log);
        f_nb_counts = findViewById (R.id.f_nb_counts);

        m_sensor = new UPMCSensor (m_handler);
    }


    public void bt_start (View view) {
        start_counting ();
    }

    public void bt_stop (View view) {
        stop_counting ();
    }

    public void start_counting () {
        if (m_sensor.is_started ())
            return;
        m_nb_counts = 0;
        m_sensor.start ();
    }

    public void stop_counting () {
        m_sensor.stop ();
        m_nb_counts = 0;
    }
}
