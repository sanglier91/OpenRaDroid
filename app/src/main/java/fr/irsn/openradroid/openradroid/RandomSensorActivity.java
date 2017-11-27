package fr.irsn.openradroid.openradroid;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

public class RandomSensorActivity extends AppCompatActivity {

    private static String TAG = RandomSensorActivity.class.getSimpleName ();

    private TextView f_log;
    private SeekBar f_cpm;
    private EditText f_nb_counts;
    private RandomSensor m_sensor;
    private int m_nb_counts;


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
        setContentView (R.layout.activity_random_sensor);
        f_log = findViewById (R.id.f_log);
        f_cpm = findViewById (R.id.f_cpm);
        f_cpm.setProgress (50);
        f_nb_counts = findViewById (R.id.f_nb_counts);
        m_sensor = new RandomSensor (m_handler);
    }

    @Override
    protected void onStop () {
        super.onStop ();
        stop_counting ();
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
        m_sensor.set_probability (f_cpm.getProgress () / 100.);
        m_sensor.start ();
    }

    public void stop_counting () {
        m_sensor.stop ();
        m_nb_counts = 0;
    }


}
