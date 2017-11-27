package fr.irsn.openradroid.openradroid;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class PocketGeigerSensorActivity extends AppCompatActivity {

    private static String TAG = PocketGeigerSensorActivity.class.getName ();

    private TextView f_log;
    private TextView f_nb_counts;

    private PocketGeigerSensor m_sensor;
    private int m_nb_counts = 0;

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
        setContentView (R.layout.activity_pocket_geiger_sensor);

        f_log = findViewById (R.id.f_log);
        f_nb_counts = findViewById (R.id.f_nb_counts);

        m_sensor = new PocketGeigerSensor (m_handler, this);
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
