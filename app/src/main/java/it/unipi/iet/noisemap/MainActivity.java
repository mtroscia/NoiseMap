package it.unipi.iet.noisemap;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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

import it.unipi.iet.noisemap.Utils.SingletonClass;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private final int MY_PERMISSIONS_REQUEST_ALL = 1;
    private static AlertDialog alertDialog = null;
    private SingletonClass singleton;

    public MainActivity() {
        singleton = SingletonClass.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("NoiseMap");

        //Set the shared preferences for the first time
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //Check permissions
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)  {
            Log.i(TAG, "[MYDEBUG] No permission granted\n");
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_ALL);
            return;
        }

        //Start the service
        singleton.scheduleServiceStart(getApplicationContext());
        Log.d(TAG, "Service start has been scheduled");

        SharedPreferences sp  = PreferenceManager.getDefaultSharedPreferences(this);
        boolean powerSaving = sp.getBoolean("powerSaving", SettingsActivity.DEFAULT_POWER_SAVING);
        if (!powerSaving)
            singleton.unregisterReceiver();
    }

    public void buttonPress(View v) {
        Log.d(TAG, "[MYDEBUG] In buttonPress()");
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    public void captureAudio(View v) {
        Log.d(TAG, "[MYDEBUG] In captureAudio()");
        double db = singleton.captureAudio();
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
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Log.d(TAG, "[MYDEBUG] Settings has been selected");
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_close) {
            Log.d(TAG, "[MYDEBUG] Close has been selected");
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
