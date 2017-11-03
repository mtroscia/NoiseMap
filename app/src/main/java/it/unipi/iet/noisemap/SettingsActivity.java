package it.unipi.iet.noisemap;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import it.unipi.iet.noisemap.Utils.SingletonClass;

public class SettingsActivity extends AppCompatActivity {
    private final String TAG = "SettingsActivity";
    public static boolean DEFAULT_RUNNING = true;
    public static String DEFAULT_INTERVAL = "300000";
    public static boolean DEFAULT_POWER_SAVING = true;
    private static SingletonClass singleton;

    @Override
    protected  void  onCreate(Bundle savedInstanceState)  {
        Log.d(TAG, "[MYDEBUG] onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        singleton = SingletonClass.getInstance();
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private final String TAG = "SettingsFragment";

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            Log.d(TAG, "[MYDEBUG] onCreate()");
            super.onCreate(savedInstanceState);

            //Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            
            SharedPreferences  sp =  getPreferenceManager().getSharedPreferences();
            boolean  running  =  sp.getBoolean("running",  SettingsActivity.DEFAULT_RUNNING);
            if (!running)  {
                getPreferenceScreen().findPreference("power_saving").setEnabled(false);
            }
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(TAG,  "[MYDEBUG] Preference changed");
            SharedPreferences  sp =  getPreferenceManager().getSharedPreferences();
            boolean  running  =  sp.getBoolean("running",  SettingsActivity.DEFAULT_RUNNING);
            if (running)  {
                Log.d(TAG, "[MYDEBUG] Schedule service start");
                getPreferenceScreen().findPreference("power_saving").setEnabled(true);
                singleton.scheduleServiceStart(getActivity());
            } else {
                Log.d(TAG, "[MYDEBUG] Schedule service stop");
                getPreferenceScreen().findPreference("power_saving").setEnabled(false);
                singleton.scheduleServiceStop(getActivity());
            }
            boolean powerSaving = sp.getBoolean("power_saving", SettingsActivity.DEFAULT_POWER_SAVING);
            if (powerSaving) {
                Log.d(TAG, "[MYDEBUG] Enable power management");
                singleton.registerReceiver(getActivity());
            } else {
                Log.d(TAG, "[MYDEBUG] Disable power management");
                singleton.unregisterReceiver(getActivity());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

}
