package it.unipi.iet.noisemap;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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
    private GoogleMap mMap;
    private LatLng myPos;
    private GoogleApiClient mApiClient = null;
    DatabaseHandler database;
    FirebaseDatabase fdatabase;
    List<DatabaseEntry> list = new ArrayList<>();
    ObservableBoolean obs = new ObservableBoolean();
    ValueEventListener valueEventListener;

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

       valueEventListener = new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                            /*if (!list.isEmpty())
                                list.clear();*/
                if (mMap==null) {
                    Log.d(TAG, "[DEBUG] Map is null");
                    return;
                }
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Log.d(TAG, "[DEBUG] Retrieved child is " + child);
                    Map<String, String> value = (Map<String, String>) child.getValue();
                    String timestamp = value.get("timestamp");
                    String coord = value.get("coordinates");
                    String[] latLon = coord.split("-");
                    double latt = Double.parseDouble(latLon[0]);
                    String latt_s = String.format("%.1f", latt);
                    double lonn = Double.parseDouble(latLon[1]);
                    String lonn_s = String.format("%.1f", lonn);
                    double noise = Double.parseDouble(value.get("noise"));
                    String activity = value.get("activity");

                    Log.d(TAG, "[DEBUG] Retrieved values are " + timestamp + " " + latt_s + "-" + lonn_s +
                            " " + noise + "dB " + activity);
                    if (mMap!=null) {
                        Log.d(TAG, "[DEBUG] Adding a marker...");
                        MarkerOptions opt = new MarkerOptions()
                                .position(new LatLng(latt, lonn))
                                .visible(true)
                                .title("Measurement")
                                .snippet("Noise: " + noise + "dB\n Activity: " + activity + "\n")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                        mMap.addMarker(opt);
                        Log.d(TAG, "[DEBUG] Marker added");
                    }
                    //DatabaseEntry e = new DatabaseEntry(timestamp, latt, lonn, noise, activity);
                    //list.add(e);
                }
                            /*Log.d(TAG, "[DEBUG] List is " + list);
                            obs.set(true);
                            Log.d(TAG, "[DEBUG] obs.set(true)");*/
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
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
        //mMap.addMarker(new MarkerOptions().position(myPos).title("ME"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(myPos));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myPos, 15.0f));

        //Write into Firebase database.
        /*FirebaseDatabase database = FirebaseDatabase.getInstance();
        Log.d(TAG, "[DEBUG] Obtained reference to Firebase\n");
        DatabaseReference entryRef = database.getReference("myDB");*/

        //Log.d(TAG, "[DEBUG] Asking for elements to display");
        //Query query = DatabaseHandler.retrieveFromDatabase("myDB");
        //Log.d(TAG, "[DEBUG] Showing results");
        fdatabase = FirebaseDatabase.getInstance();
        DatabaseReference entryRef = fdatabase.getReference("myDB");
        if (entryRef!=null) {
            Log.d(TAG, "[DEBUG] Obtained reference to my DB\n");
            Query query = entryRef.orderByChild("timestamp");
            query.addValueEventListener(valueEventListener);
                    /*new ValueEventListener() {
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            //if (!list.isEmpty())
                                //list.clear();
                            if (mMap==null) {
                                Log.d(TAG, "[DEBUG] Map is null");
                                return;
                            }
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                Log.d(TAG, "[DEBUG] Retrieved child is " + child);
                                Map<String, String> value = (Map<String, String>) child.getValue();
                                String timestamp = value.get("timestamp");
                                String coord = value.get("coordinates");
                                String[] latLon = coord.split("-");
                                double latt = Double.parseDouble(latLon[0]);
                                String latt_s = String.format("%.1f", latt);
                                double lonn = Double.parseDouble(latLon[1]);
                                String lonn_s = String.format("%.1f", lonn);
                                double noise = Double.parseDouble(value.get("noise"));
                                String activity = value.get("activity");


                                Log.d(TAG, "[DEBUG] Retrieved values are " + timestamp + " " + latt_s + "-" + lonn_s +
                                        " " + noise + "dB " + activity);
                                if (mMap!=null) {
                                    Log.d(TAG, "[DEBUG] Adding a marker...");
                                    MarkerOptions opt = new MarkerOptions()
                                            .position(new LatLng(latt, lonn))
                                            .visible(true)
                                            .title("Measurement")
                                            .snippet("Noise: " + noise + "dB\n Activity: " + activity + "\n")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                    mMap.addMarker(opt);
                                    Log.d(TAG, "[DEBUG] Marker added");
                                }
                                //DatabaseEntry e = new DatabaseEntry(timestamp, latt, lonn, noise, activity);
                                //list.add(e);
                            }
                            //Log.d(TAG, "[DEBUG] List is " + list);
                            //obs.set(true);
                            //Log.d(TAG, "[DEBUG] obs.set(true)");
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });*/
            Log.d(TAG, "[DEBUG] Before the list check");

            /*obs.setListener(new OnBooleanChangeListener()
            {
                @Override
                public void onBooleanChanged(boolean newValue)
                {
                    Log.d(TAG, "[DEBUG] In onBooleanChanged");
                    if (newValue==true) {
                        Log.d(TAG, "New value is true");
                        if (!list.isEmpty()) {
                            Log.d(TAG, "[DEBUG] List is not empty");
                            for (DatabaseEntry d : list) {
                                Log.d(TAG, "[DEBUG] Adding a marker...");
                                MarkerOptions opt = new MarkerOptions()
                                        .position(new LatLng(d.getLat(), d.getLon()))
                                        .visible(true)
                                        .title("Measurement")
                                        .snippet("Noise: " + d.getNoise() + "dB\n Activity: " + d.getActivity() + "\n")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                mMap.addMarker(opt);
                                Log.d(TAG, "[DEBUG] Marker added");
                            }
                            Log.d(TAG, "[DEBUG] obs.set(false)");
                            obs.set(false);
                        } else {
                            Log.d(TAG, "[DEBUG] List is empty");
                        }
                    }
                }
            });*/

        }

        /*if (entryRef!=null)
            Log.d(TAG, "[DEBUG] Obtained reference to my DB\n");
        //DatabaseReference childRef = entryRef.child("entries");
        //if (childRef!=null)
            //Log.d(TAG, "[DEBUG] Obtained reference to child\n");
        Entry entry = new Entry();
        Log.d(TAG, "[DEBUG] New entry generated "+entry);*/




        /*//New entry
        DatabaseReference newChild = entryRef.push();
        Log.d(TAG, "[DEBUG] New child");
        newChild.setValue(entry.toMap());*/

        //childRef.push().setValue(entry);

        //Log.d(TAG, "[DEBUG] Entry is set");
        //myRef.setValue("Hello!");

        /*entryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Log.d(TAG, "[DEBUG] onDataChange");
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Log.d(TAG, "[DEBUG] Retrieved child is " + child);
                    Map<String,String> value = (Map<String,String>)child.getValue();
                    String timestamp = value.get("timestamp");
                    String coord = value.get("coordinates");
                    String[] latLon = coord.split("-");
                    double latt = Double.parseDouble(latLon[0]);
                    String latt_s = String.format("%.1f", latt);
                    double lonn = Double.parseDouble(latLon[1]);
                    String lonn_s = String.format("%.1f", lonn);
                    double noise = Double.parseDouble(value.get("noise"));
                    String activity = value.get("activity");
                    Log.d(TAG, "[DEBUG] Retrieved values are " + timestamp + " "+latt_s+"-"+lonn_s+
                        " "+noise+"dB "+activity);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });*/

        //Output updates from the service.
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
        /*if (mApiClient==null) {
            mApiClient = new GoogleApiClient.Builder(this)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mApiClient.connect();*/
    }

    @Override
    protected void onDestroy(){
        Log.d(TAG, "[DEBUG] onDestroy\n");
        super.onDestroy();
    }
}
