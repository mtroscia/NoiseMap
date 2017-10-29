package it.unipi.iet.noisemap;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

    public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private final String TAG = "MapsActivity";
        private GoogleMap mMap = null;
        private GoogleApiClient mApiClient = null;
        private FirebaseDatabase fdatabase;
        private LocationManager locationManager;
        private String provider;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setContentView(R.layout.activity_maps);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar3);
            //toolbar.setVisibility(View.VISIBLE);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null){
                getSupportActionBar().show();
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }

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
            Log.d(TAG, "[MYDEBUG] In onMapReady()\n");
            mMap = googleMap;

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "[MYDEBUG] Without permission you can't go on\n");
                return;
            }

            //Map is being set up.
            mMap.setMyLocationEnabled(true);
            locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            provider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(provider);
            double lat = 0.0, lon = 0.0;
            if (location != null) {
                lat = location.getLatitude();
                lon = location.getLongitude();
                Log.d(TAG, "[MYDEBUG] Last location available: " + lat +", "+ lon + "\n");
            } else {
                Log.d(TAG, "[MYDEBUG] Last location not available\n");
                return;
            }

            mMap.setPadding(0, 200,0,0);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15.5f));

            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {

                    LinearLayout info = new LinearLayout(getApplicationContext());
                    info.setOrientation(LinearLayout.VERTICAL);

                    TextView title = new TextView(getApplicationContext());
                    title.setTextColor(Color.BLACK);
                    title.setGravity(Gravity.CENTER);
                    title.setTypeface(null, Typeface.BOLD);
                    title.setText(marker.getTitle());

                    TextView snippet = new TextView(getApplicationContext());
                    snippet.setTextColor(Color.GRAY);
                    snippet.setText(marker.getSnippet());

                    info.addView(title);
                    info.addView(snippet);

                    return info;
                }
            });

            fdatabase = FirebaseDatabase.getInstance();
            DatabaseReference entryRef = fdatabase.getReference("myDB");
            if (entryRef!=null) {
                Log.d(TAG, "[MYDEBUG] Obtained reference to my DB\n");
                Query query = entryRef.orderByChild("timestamp");
                query.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Log.d(TAG, "[MYDEBUG] onChildAdded");
                        Map<String, String> value = (Map<String, String>) dataSnapshot.getValue();
                        String timestamp = value.get("timestamp");
                        float opacity = 0.8f;
                        try {
                            DateFormat df = DateFormat.getInstance();
                            Date d = df.parse(timestamp);
                            double timeDiff = new Date().getTime()-d.getTime();
                            if (timeDiff>5*24*60*60) {
                                //it is more than 2 days old
                                Log.d(TAG, "Very old measurement");
                                return;
                            } else if (timeDiff>2*24*60*60 && timeDiff<=5*24*60*60)
                                opacity = 0.5f;
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        String coord = value.get("coordinates");
                        String[] latLon = coord.split("-");
                        double latt = Double.parseDouble(latLon[0]);
                        double lonn = Double.parseDouble(latLon[1]);
                        Location pointPos = new Location("");
                        pointPos.setLatitude(latt);
                        pointPos.setLongitude(lonn);
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            Log.w(TAG, "[MYDEBUG] Without permission you can't go on\n");
                            return;
                        }
                        Location myPos = locationManager.getLastKnownLocation(provider);
                        if (myPos.distanceTo(pointPos)>5000) {
                            //distance in metres
                            Log.d(TAG, "Very far point");
                            return;
                        }

                        double noise = Double.parseDouble(value.get("noise"));

                        String activity = value.get("activity");

                        Log.d(TAG, "[MYDEBUG] Retrieved values are " + timestamp + " "+latt +"-" + lonn +
                                " " + noise + "dB " + activity);

                        MarkerOptions opt = new MarkerOptions()
                                .position(new LatLng(latt, lonn))
                                .visible(true)
                                .alpha(opacity)
                                .flat(true)
                                .title(timestamp)
                                .snippet("Noise: "+noise+"dB\n"+"Activity: "+activity+"\n");
                        if (noise>65)
                            opt.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_red));
                        else if (noise>50 && noise<=65)
                            opt.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_yellow));
                        else if (noise<50)
                            opt.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_green));
                        mMap.addMarker(opt);
                        Log.d(TAG, "[MYDEBUG] Marker added");
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            //This method is invoked when the connection with the GoogleApiClient is complete.
            Log.d(TAG, "[MYDEBUG] onConnected\n");
            Intent intent = new Intent(this, ActivityRecognizedService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // [REDUCE COMPUTATIONS WHILE DEBUGGING]
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 60000, pendingIntent);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "[MYDEBUG] onConnectionSuspended\n");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(TAG, "[MYDEBUG] onConnectionFailed\n");
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

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // handle arrow click here
            if (item.getItemId() == android.R.id.home) {
                finish(); // close this activity and return to preview activity (if there is any)
            }

            return super.onOptionsItemSelected(item);
        }
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

}*/












