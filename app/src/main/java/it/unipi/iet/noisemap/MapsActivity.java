package it.unipi.iet.noisemap;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.unipi.iet.noisemap.Utils.DatabaseEntry;
import it.unipi.iet.noisemap.Utils.ObservableBoolean;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final String TAG = "MapsActivity";
    private GoogleMap mMap = null;
    private LatLng myPos;
    private GoogleApiClient mApiClient = null;
    private FirebaseDatabase fdatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Connect to GoogleApiClient.
        if (mApiClient==null) {
            mApiClient = new GoogleApiClient.Builder(this)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mApiClient.connect();
        }

    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        Log.d(TAG, "[DEBUG] In onMapReady()\n");
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "[DEBUG] Without permission you can't go on\n");
            return;
        }

        //Map is being set up.
        mMap.setMyLocationEnabled(true);
        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(provider);
        double lat = 0.0, lon = 0.0;
        if (location != null) {
            lat = location.getLatitude();
            lon = location.getLongitude();
            Log.d(TAG, "[DEBUG] Last location available: " + lat +", "+ lon + "\n");
        } else {
            Log.d(TAG, "[DEBUG] Last location not available\n");
            return;
        }
        myPos = new LatLng(lat, lon);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myPos, 15.0f));

        fdatabase = FirebaseDatabase.getInstance();
        DatabaseReference entryRef = fdatabase.getReference("myDB");
        if (entryRef!=null) {
            Log.d(TAG, "[DEBUG] Obtained reference to my DB\n");
            Query query = entryRef.orderByChild("timestamp");
            query.addValueEventListener(new ValueEventListener() {
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                Log.d(TAG, "[DEBUG] Retrieved child is " + child);
                                Map<String, String> value = (Map<String, String>) child.getValue();
                                String timestamp = value.get("timestamp");
                                String coord = value.get("coordinates");
                                String[] latLon = coord.split("-");
                                double latt = Double.parseDouble(latLon[0]);
                                //latt_s for DEBUG purposes
                                String latt_s = String.format("%.1f", latt);
                                double lonn = Double.parseDouble(latLon[1]);
                                //lonn_s for DEBUG purposes
                                String lonn_s = String.format("%.1f", lonn);
                                double noise = Double.parseDouble(value.get("noise"));
                                String activity = value.get("activity");

                                Log.d(TAG, "[DEBUG] Retrieved values are " + timestamp + " " + latt_s + "-" + lonn_s +
                                        " " + noise + "dB " + activity);

                                Log.d(TAG, "[DEBUG] Adding a marker...");
                                CircleOptions optc = new CircleOptions()
                                        .center(new LatLng(latt, lonn))
                                        .radius(5)
                                        .visible(true)
                                        .fillColor(Color.GREEN)
                                        .strokeColor(Color.GREEN)
                                        .clickable(true);
                                mMap.addCircle(optc);
                                Log.d(TAG, "[DEBUG] Marker added");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }

        if (ActivityRecognizedService.mostProbableActivity!=null) {
            Log.d(TAG, "[DEBUG] The most probable activity is " +
                    ActivityRecognizedService.activityToString(ActivityRecognizedService.mostProbableActivity));
        }
   }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //This method is invoked when the connection with the GoogleApiClient is complete.
        Log.d(TAG, "[DEBUG] onConnected\n");
        Intent intent = new Intent(this, ActivityRecognizedService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // [REDUCE COMPUTATIONS WHILE DEBUGGING]
        //ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 60000, pendingIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "[DEBUG] onPause\n");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "[DEBUG] onResume\n");
        super.onResume();
    }

    @Override
    protected void onDestroy(){
        Log.d(TAG, "[DEBUG] onDestroy\n");
        super.onDestroy();
    }
}
