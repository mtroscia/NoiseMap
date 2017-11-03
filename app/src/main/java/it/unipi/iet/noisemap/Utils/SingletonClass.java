package it.unipi.iet.noisemap.Utils;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

import it.unipi.iet.noisemap.Receivers.PowerManagementReceiver;
import it.unipi.iet.noisemap.Services.ActivityRecognizedService;
import it.unipi.iet.noisemap.SettingsActivity;

public class SingletonClass implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private final static String TAG = "SingletonClass";
    private static SingletonClass singleton = null;
    private Context context;

    //private Context context = null;
    private FirebaseDatabase database = null;
    private AudioRecord audioRecorder = null;
    private GoogleApiClient mApiClient = null;
    private Handler mHandler = new Handler();

    private boolean serviceRunning;
    private boolean receiverSet = true; //by default, it is true as it is in the manifest
    private String choosenInterval = "";
    private static int count = 0;
    private String lastActivity = null;
    private String lastAddress = null;
    private String lastNoise = null;
    private String lastTimestamp = null;

    public SingletonClass() {
    }

    public void setContext(Context c) {
        context = c;
    }

    public static SingletonClass getInstance() {
        if (singleton==null)
            singleton = new SingletonClass();
        return singleton;
    }

    public void setDBconnection() {
        //Connect to Firebase
        if (database==null) {
            database = FirebaseDatabase.getInstance();
            Log.d(TAG, "[MYDEBUG] Obtained reference to Firebase\n");
        }
    }

    public void setServiceRunning(boolean serviceRunning) {
        this.serviceRunning = serviceRunning;
    }

    public void scheduleServiceStart(final Context c) {
        Log.d(TAG, "[MYDEBUG] Service is going to be started\n");

        //Connect to GoogleApiClient
        if (mApiClient==null) {
            mApiClient = new GoogleApiClient.Builder(c)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mApiClient.connect();
            Log.d(TAG, "[MYDEBUG] Obtained reference to GoogleApiClient\n");
        } else {
            SharedPreferences sp =  PreferenceManager.getDefaultSharedPreferences(c);
            String interval_s = sp.getString("interval", SettingsActivity.DEFAULT_INTERVAL);
            if (serviceRunning && interval_s.equals(choosenInterval))
                return;
            serviceRunning = true;
            choosenInterval = interval_s;
            Intent intent = new Intent(c, ActivityRecognizedService.class);
            PendingIntent pendingIntent = PendingIntent.getService(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient, pendingIntent);

            boolean  running  =  sp.getBoolean("running",  SettingsActivity.DEFAULT_RUNNING);
            if (running) {
                long interval = Long.parseLong(interval_s);
                Log.d(TAG, "[MYDEBUG] Chosen interval="+interval);
                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, interval, pendingIntent);
                Log.d(TAG, "[MYDEBUG] Service has been started\n");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CharSequence text = "Activity recognition started";
                        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    public void scheduleServiceStop(final Context c) {
        if (!serviceRunning)
            return;
        serviceRunning = false;
        Intent intent = new Intent(c, ActivityRecognizedService.class);
        PendingIntent pendingIntent = PendingIntent.getService(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient, pendingIntent);
        Log.d(TAG, "[MYDEBUG] Service has been stopped\n");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "Activity recognition stopped";
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void registerReceiver(final Context c) {
        Log.d(TAG, "[MYDEBUG] In registerReceiver");
        /*if (context==null) {
            Log.d(TAG, "Setting up context");
            this.context = context;
        }*/
        if (receiverSet)
            return;
        receiverSet = true;
        ComponentName receiver = new ComponentName(c, PowerManagementReceiver.class);
        PackageManager pm = c.getPackageManager();
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        Log.d(TAG, "[MYDEBUG] Receiver is registered");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "Power management started";
                Toast.makeText(c, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void unregisterReceiver(final Context c) {
        Log.d(TAG, "[MYDEBUG] In unregisterReceiver");
        /*if (context==null) {
            Log.d(TAG, "Setting up context");
            this.context = context;
        }*/
        if (!receiverSet)
            return;
        receiverSet = false;
        ComponentName receiver = new ComponentName(c, PowerManagementReceiver.class);
        PackageManager pm = c.getPackageManager();
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        Log.d(TAG, "[MYDEBUG] Receiver is unregistered");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "Power management stopped";
                Toast.makeText(c, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    public double captureAudio() {
        Log.d(TAG, "[MYDEBUG] In captureAudio()");
        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT);
        bufferSize = bufferSize*4;
        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        short data [] = new short[bufferSize];
        audioRecorder.startRecording();
        audioRecorder.read(data, 0, bufferSize);
        audioRecorder.stop();
        audioRecorder.release();
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
        return db;
    }

    public void insertIntoDatabase(String databaseName, DatabaseEntry e) {
        Log.d(TAG, "[MYDEBUG] In insertIntoDatabase\n");
        DatabaseReference entryRef = database.getReference(databaseName);
        if (entryRef!=null)
            Log.d(TAG, "[MYDEBUG] Obtained reference to my DB\n");
        DatabaseReference newChild = entryRef.push();
        Log.d(TAG, "[MYDEBUG] New child");

        Map<String,String> map = new ArrayMap<>();
        map.put("timestamp", e.getTimestamp());
        String coord = e.getLat()+"-"+e.getLon();
        map.put("coordinates", coord);
        String noise_s=String.format("%.1f", e.getNoise());
        map.put("noise", noise_s);
        map.put("activity", e.getActivity());
        newChild.setValue(map);
        Log.d(TAG, "[MYDEBUG] Entry is set");

        entryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                /* This method is called once with the initial value and again
                   whenever data at this location is updated. */
                Log.d(TAG, "[MYDEBUG] onDataChange");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.d(TAG, "[MYDEBUG] onCancelled");
                Log.w(TAG, "[MYDEBUG] Failed to read value", error.toException());
            }
        });
    }

    public Query retrieveFromDatabase(String databaseName) {
        Log.d(TAG, "[MYDEBUG] in retrieveFromDatabase");
        DatabaseReference entryRef = database.getReference(databaseName);
        if (entryRef==null) {
            Log.d(TAG, "[MYDEBUG] Not obtained reference to my DB\n");
            return null;
        }
        Log.d(TAG, "[MYDEBUG] Obtained reference to my DB\n");
        Query query = entryRef.orderByChild("timestamp");
        Log.d(TAG, "[MYDEBUG] Return query\n");
        return query;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //This method is invoked when the connection with the GoogleApiClient is complete.
        Log.d(TAG, "[MYDEBUG] onConnected\n");
        Intent intent = new Intent(context, ActivityRecognizedService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // [REDUCE COMPUTATIONS WHILE DEBUGGING]
        SharedPreferences sp =  PreferenceManager.getDefaultSharedPreferences(context);
        boolean  running  =  sp.getBoolean("running",  SettingsActivity.DEFAULT_RUNNING);
        if (running) {
            serviceRunning = true;
            String is = sp.getString("interval", SettingsActivity.DEFAULT_INTERVAL);
            choosenInterval = is;
            long interval = Long.parseLong(is);
            Log.d(TAG, "[MYDEBUG] Chosen interval="+interval);
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, interval, pendingIntent);
            Log.d(TAG, "[MYDEBUG] Service has been started\n");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    CharSequence text = "Activity recognition started";
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "[MYDEBUG] onConnectionSuspended\n");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "[MYDEBUG] onConnectionFailed\n");
    }

    public void incrementCount() {
        count++;
    }

    public void setCount(int c) {
        count = c;
    }

    public int getCount() {
        return count;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(String lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getLastAddress() {
        return lastAddress;
    }

    public void setLastAddress(String lastAddress) {
        this.lastAddress = lastAddress;
    }

    public String getLastNoise() {
        return lastNoise;
    }

    public void setLastNoise(String lastNoise) {
        this.lastNoise = lastNoise;
    }

    public String getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(String lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }
}
