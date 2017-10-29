package it.unipi.iet.noisemap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final String TAG = "SettingsActivity";
    public static boolean DEFAULT_RUNNING = true;
    public static String DEFAULT_INTERVAL = "300000";
    private SingletonClass singleton;

    @Override
    protected  void  onCreate(Bundle savedInstanceState)  {
        Log.d(TAG, "[MYDEBUG] onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        singleton = SingletonClass.getInstance();

        SharedPreferences  sp  = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    public static class SettingsFragment extends PreferenceFragment {
        private final String TAG = "SettingsFragment";

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            Log.d(TAG, "[MYDEBUG] onCreate()");
            super.onCreate(savedInstanceState);

            //Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //TODO: handle onSharedPreferenceChanged
        Log.d(TAG,  "[MYDEBUG] Preference changed");
        SharedPreferences  sp =  PreferenceManager.getDefaultSharedPreferences(this);
        boolean  running  =  sp.getBoolean("running",  SettingsActivity.DEFAULT_RUNNING);
        if (running)  {
            singleton.scheduleServiceStart(this);
        } else {
            singleton.scheduleServiceStop();
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
