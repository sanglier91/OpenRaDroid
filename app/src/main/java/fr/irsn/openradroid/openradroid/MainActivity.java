package fr.irsn.openradroid.openradroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void start_random_sensor_activity (View view) {
        Intent intent = new Intent (MainActivity.this, RandomSensorActivity.class);
        startActivity (intent);
    }

    public void start_upmc_sensor_activity (View view) {
        Intent intent = new Intent (MainActivity.this, UPMCSensorActivity.class);
        startActivity (intent);
    }

    public void start_pocketgeiger_sensor_activity (View view) {
        Intent intent = new Intent (MainActivity.this, PocketGeigerSensorActivity.class);
        startActivity (intent);
    }
}
