package it.unipi.iet.noisemap.Services;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import it.unipi.iet.noisemap.Utils.DatabaseEntry;
import it.unipi.iet.noisemap.Utils.SingletonClass;

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
        /*mHandler = new Handler();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "Activity recognition started";
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });*/
    }

    @Override
    public void onHandleIntent(Intent intent) {
        //The method is invoked every time a new result has to be sent to the GoogleApiClient
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "[MYDEBUG] Without permission you can't go on\n");
                return;
            }

            if (activityToString(mostProbableActivity).equals("tilting") || activityToString(mostProbableActivity).equals("unknown")) {
                Log.d(TAG, "[MYDEBUG] Strange activity ("+activityToString(mostProbableActivity)+")");
                return;
            }

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

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = null;

            try {
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1);
            } catch (IOException ioException) {
                Log.e(TAG, "[MYDEBUG] Service not available", ioException);
            } catch (IllegalArgumentException illegalArgumentException) {
                Log.e(TAG, "[MYDEBUG] Invalid coordinates" + ". " +
                        "Latitude = " + location.getLatitude() +
                        ", Longitude = " +
                        location.getLongitude(), illegalArgumentException);
            }

            ArrayList<String> addressFragments = new ArrayList<>();
            // Handle case where no address was found.
            if (addresses == null || addresses.size()  == 0) {
                Log.e(TAG, "[MYDEBUG] No address found");
            } else {
                Address address = addresses.get(0);

                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }
                Log.i(TAG, "[MYDEBUG] Address found "+addressFragments);
            }

            //Obtain the noise
            double noise = singleton.captureAudio();

            //Obtain the timestamp
            SimpleDateFormat sdfDate = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss");
            Date date = new Date();
            String strDate = sdfDate.format(date);

            singleton.setLastActivity(activityToString(mostProbableActivity));
            if (!addressFragments.isEmpty()) {
                String address_s = "";
                for (int i=0; i<addressFragments.size(); i++)
                    if (i!=addressFragments.size()-1)
                        address_s += addressFragments.get(i)+",";
                    else
                        address_s += addressFragments.get(i);
                singleton.setLastAddress(address_s);
            }
            String noise_s = String.format("%.1f", noise);
            singleton.setLastNoise(noise_s+"dB");
            singleton.setLastTimestamp(strDate);

            if (activityToString(mostProbableActivity).equals("still"))
                singleton.incrementCount();
            else
                singleton.setCount(0);

            if (singleton.getCount()==3) {
                Log.d(TAG, "[MYDEBUG] Count equal to "+singleton.getCount());
                return;
            }

            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = getApplicationContext().registerReceiver(null, intentFilter);
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            if (BatteryManager.BATTERY_STATUS_CHARGING==status || BatteryManager.BATTERY_STATUS_FULL==status) {
                Log.d(TAG, "[MYDEBUG] Phone is attached to power source");
                return;
            }

            Log.d(TAG, "[MYDEBUG] The most probable activity is "+activityToString(mostProbableActivity));

            //Generate a new entry
            DatabaseEntry e = new DatabaseEntry(strDate, lat, lon, noise, activityToString(mostProbableActivity));
            Log.d(TAG, "[MYDEBUG] New entry generated "+e);

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
