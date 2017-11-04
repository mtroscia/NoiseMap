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
import android.support.v4.app.ActivityCompat;
import android.util.Log;

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
    private SingletonClass singleton;

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
        Log.d(TAG, "[MYDEBUG] in ActivityRecognizedService()");

        singleton = SingletonClass.getInstance();
        singleton.setDBconnection();
    }

    @Override
    public void onHandleIntent(Intent intent) {
        //The method is invoked every time a new result has to be sent to the GoogleApiClient
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "[MYDEBUG] Without permission you can't go on");
                return;
            }

            if (activityToString(mostProbableActivity).equals("tilting") ||
                    activityToString(mostProbableActivity).equals("unknown")) {
                Log.d(TAG, "[MYDEBUG] Strange activity ("+activityToString(mostProbableActivity)+")");
                return;
            }

            // Obtain the location
            LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, true);
            if (provider==null) {
                Log.e(TAG, "[MYDEBUG] Provider is null");
                return;
            }
            Location location = locationManager.getLastKnownLocation(provider);
            if (location==null) {
                Log.e(TAG, "[MYDEBUG] No information about the location available");
                return;
            }

            double lat = location.getLatitude();
            double lon = location.getLongitude();

            //Obtain the address associated to the coordinates
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1);
            } catch (IOException ioException) {
                Log.e(TAG, "[MYDEBUG] Geocoder service not available", ioException);
            } catch (IllegalArgumentException illegalArgumentException) {
                Log.e(TAG, "[MYDEBUG] Invalid coordinates: " + location.getLatitude() +
                        "-" + location.getLongitude(), illegalArgumentException);
            }

            ArrayList<String> addressFragments = new ArrayList<>();
            // Handle case where no address was found
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
            SimpleDateFormat sdfDate = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss", Locale.US);
            Date date = new Date();
            String strDate = sdfDate.format(date);

            //Set parameters for last measurement (to be shown into the main)
            if (!addressFragments.isEmpty()) {
                StringBuilder address_s = new StringBuilder(150);
                for (int i=0; i<addressFragments.size(); i++)
                    if (i!=addressFragments.size()-1) {
                        address_s.append(addressFragments.get(i)).append(",");
                    }
                    else {
                        address_s.append(addressFragments.get(i));
                    }
                singleton.setLastAddress(address_s.toString());
            }
            String noise_s = String.format(Locale.US, "%.1f", noise);
            Log.d(TAG, "[MYDEBUG] Noise="+noise_s);
            singleton.setLastNoise(noise_s+"dB");
            singleton.setLastTimestamp(strDate);
            singleton.setLastActivity(activityToString(mostProbableActivity));

            /*Avoid inserting too many records with activity==still (great changes are not expected
            when you are not moving */
            if (activityToString(mostProbableActivity).equals("still"))
                singleton.incrementCount();
            else
                singleton.resetCount();

            if (singleton.getCount()==3) {
                Log.i(TAG, "[MYDEBUG] Count equal to "+singleton.getCount());
                return;
            }

            /*Check if the device is attached to a power source: in this case, the device is still
            even if the GPS shows some movement. Avoid inserting these records as before. */
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = getApplicationContext().registerReceiver(null, intentFilter);
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            if (status==-1) {
                Log.e(TAG, "[MYDEBUG] Status not available");
                return;
            }
            if (BatteryManager.BATTERY_STATUS_CHARGING==status || BatteryManager.BATTERY_STATUS_FULL==status) {
                Log.i(TAG, "[MYDEBUG] Phone is attached to power source");
                return;
            }

            //Generate a new entry and insert it into the database
            DatabaseEntry e = new DatabaseEntry(strDate, lat, lon, noise, activityToString(mostProbableActivity));
            Log.d(TAG, "[MYDEBUG] New entry generated "+e);
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
