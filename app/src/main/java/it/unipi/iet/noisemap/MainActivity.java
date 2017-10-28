package it.unipi.iet.noisemap;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private final int MY_PERMISSIONS_REQUEST_ALL = 1;
    private static AlertDialog alertDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("NoiseMap");

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)  {
            Log.i(TAG, "[MYDEBUG] No permission granted\n");
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_ALL);
            return;
        }
    }

    public void buttonPress(View v) {
        Log.d(TAG, "[MYDEBUG] In buttonPress()");
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    public void captureAudio(View v) {
        Log.d(TAG, "[MYDEBUG] In captureAudio()");
        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT);
        bufferSize = bufferSize*4;
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        short data [] = new short[bufferSize];
        recorder.startRecording();
        recorder.read(data, 0, bufferSize);
        recorder.stop();
        recorder.release();
        double average = 0.0;
        for (short s : data) {
            if (s>0)
                average += Math.abs(s);
            else
                bufferSize--;
        }
        double measurement = average/bufferSize;

        /* Calculating the Pascal pressure based on the idea that the max amplitude
         (between 0 and 32767) is relative to the pressure.
         The value 51805.5336 can be derived from assuming that x=32767=0.6325Pa and
         x=1=0.00002Pa (the reference value) */
        double pressure = measurement/51805.5336;
        double db = (20 * Math.log10(pressure/0.00002));
        String db_s = String.format("%.1f", db);
        Log.d(TAG, "[MYDEBUG] Noise is "+db_s+"dB");
        TextView tv = (TextView) findViewById(R.id.text_view);
        if (tv != null) {
            tv.setText("Sensed noise is " + db_s + "dB\n");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        String message = "";
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ALL:
                if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (grantResults.length > 0) {
                        if (!(ContextCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ||
                                !(ContextCompat.checkSelfPermission(this,
                                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED))
                            //permission not obtained: cannot use the GPS
                            message = message + "Without these permissions, you will " +
                                    "not able to use the application\n";
                        Log.i(TAG, "Permission refused\n");
                        if (!message.equals("")) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                            alertDialogBuilder.setTitle("ATTENTION")
                                    .setMessage(message)
                                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                            alertDialog.dismiss();
                                        }
                                    });
                            alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                        }
                    }
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Log.d(TAG, "[MYDEBUG] Settings has been selected");
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_close) {
            //TODO: STUFF WHEN CLOSE IS PRESSED
            Log.d(TAG, "[MYDEBUG] Close has been selected");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
