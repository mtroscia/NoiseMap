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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

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

        //Set layout, orientation and toolbar
        setContentView(R.layout.activity_main);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar()!=null)
            getSupportActionBar().setTitle("NoiseMap");

        //Set the shared preferences for the first time
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        singleton.setContext(getApplicationContext());

        //Check permissions
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)  {
            Log.i(TAG, "[MYDEBUG] No permission granted");
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_ALL);
            return;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean running = sp.getBoolean("running",  SettingsActivity.DEFAULT_RUNNING);
        boolean powerSaving = sp.getBoolean("running",  SettingsActivity.DEFAULT_RUNNING);
        if (running) {
            singleton.scheduleServiceStart();
            Log.d(TAG, "[MYDEBUG] Service start has been scheduled");
            if (!powerSaving) {
                Log.d(TAG, "[MYDEBUG] Receiver must be unset");
                singleton.unregisterReceiver();
            }
        } else {
            Log.d(TAG, "[MYDEBUG] Service is not active");
            singleton.setServiceRunningFalse();
            if (!powerSaving) {
                Log.d(TAG, "[MYDEBUG] Receiver must be unset");
                singleton.unregisterReceiver();
            }
        }
    }

    public void buttonPress(View v) {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    public void captureAudio(View v) {
        double db = singleton.captureAudio();
        String db_s = String.format(Locale.US, "%.1f", db);
        String message = "Sensed noise is " + db_s + "dB\n";
        TextView tv = (TextView) findViewById(R.id.textView);
        if (tv != null) {
            tv.setText(message);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_close) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "[MYDEBUG] onResume");
        super.onResume();

        TextView tv = (TextView) findViewById(R.id.textView);
        if (tv != null) {
            tv.setText("");
        }

        tv = (TextView) findViewById(R.id.textView2);
        if (tv != null) {
            if (singleton.getLastAddress()==null) {
                String message = "No measurement available. Is the GPS set and the activity recognition enabled?";
                tv.setText(message);
            } else {
                String text = singleton.getLastTimestamp()+"\n"+singleton.getLastAddress()+" (estimated)\n"
                        +"Noise: "+singleton.getLastNoise()+"\nActivity: "+singleton.getLastActivity();
                tv.setText(text);
            }
        }

        if (singleton.getLastAddress()!=null) {
            ImageView image = (ImageView) findViewById(R.id.image);
            image.setVisibility(View.VISIBLE);
            switch (singleton.getLastActivity()) {
                case "in vehicle":
                    image.setImageResource(R.drawable.act_in_vehicle);
                    break;
                case "on bicycle":
                    image.setImageResource(R.drawable.act_on_bycicle);
                    break;
                case "on foot":
                    image.setImageResource(R.drawable.act_on_foot);
                    break;
                case "walking":
                    image.setImageResource(R.drawable.act_walking);
                    break;
                case "running":
                    image.setImageResource(R.drawable.act_running);
                    break;
                case "still":
                    image.setImageResource(R.drawable.act_still);
                    break;
                default:
                    image.setVisibility(View.INVISIBLE);
            }
        }
    }

}
