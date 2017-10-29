package it.unipi.iet.noisemap;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.text.SimpleDateFormat;
import java.util.Date;

import it.unipi.iet.noisemap.Utils.DatabaseEntry;

public class ActivityRecognizedService extends IntentService {
    private final String TAG = "ActivityRecognizedServ";
    private Handler mHandler;
    private SingletonClass singleton;

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
        Log.d(TAG, "[MYDEBUG] in ActivityRecognizedService()\n");

        singleton = SingletonClass.getInstance();
        singleton.setDBconnection();

        //For debug purposes
        mHandler = new Handler();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "Activity recognition started";
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onHandleIntent(Intent intent) {
        //The method is invoked every time a new result has to be sent to the GoogleApiClient
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            Log.d(TAG, "[MYDEBUG] The most probable activity is "+activityToString(mostProbableActivity));

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "[MYDEBUG] Without permission you can't go on\n");
                return;
            }

            if (activityToString(mostProbableActivity).equals("still"))
                singleton.incrementCount();
            else
                singleton.setCount(0);

            if (singleton.getCount()==3 || activityToString(mostProbableActivity).equals("tilting") || activityToString(mostProbableActivity).equals("unknown")) {
                Log.d(TAG, "Count equal to 2 ("+singleton.getCount()+") or strange activity ("+activityToString(mostProbableActivity)+")");
                return;
            }

            // TODO: HANDLE UNIQUE LOCATION
            // Obtain the location
            LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(provider);
            if (location==null) {
                Log.d(TAG, "[MYDEBUG] No information about the location available");
                return;
            }
            double lat = location.getLatitude();
            double lon = location.getLongitude();

            // Obtain the noise
            double noise = singleton.captureAudio();

            //Obtain the timestamp
            SimpleDateFormat sdfDate = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss");
            Date date = new Date();
            String strDate = sdfDate.format(date);

            //Generate a new entry
            DatabaseEntry e = new DatabaseEntry(strDate, lat, lon, noise, activityToString(mostProbableActivity));
            Log.d(TAG, "New entry generated "+e);

            //Insert into the DB
            // [REDUCE COMPUTATIONS WHILE DEBUGGING]
            singleton.insertIntoDatabase("myDB", e);
        }
    }

    private static String activityToString(DetectedActivity activity) {
        switch( activity.getType() ) {
            case DetectedActivity.IN_VEHICLE: {
                return "in vehicle";
            }
            case DetectedActivity.ON_BICYCLE: {
                return "on bicycle";
            }
            case DetectedActivity.ON_FOOT: {
                return "on foot";
            }
            case DetectedActivity.RUNNING: {
                return "running";
            }
            case DetectedActivity.STILL: {
                return "still";
            }
            case DetectedActivity.TILTING: {
                return "tilting";
            }
            case DetectedActivity.WALKING: {
                return "walking";
            }
            case DetectedActivity.UNKNOWN: {
                return "unknown";
            }
            default: {
                return "no activity";
            }
        }
    }
}
