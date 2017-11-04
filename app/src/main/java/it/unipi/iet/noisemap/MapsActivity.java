package it.unipi.iet.noisemap;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.google.firebase.database.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import it.unipi.iet.noisemap.Utils.SingletonClass;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String TAG = "MapsActivity";
    private GoogleMap mMap = null;
    private LocationManager locationManager;
    private String provider;
    private SingletonClass singleton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set layout, orientation and toolbar
        setContentView(R.layout.activity_maps);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar3);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null){
            getSupportActionBar().show();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        //Obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        singleton = SingletonClass.getInstance();
        singleton.setDBconnection();
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        Log.d(TAG, "[MYDEBUG] Map ready");
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "[MYDEBUG] Without permission you can't go on");
            return;
        }

        //Map is being set up
        mMap.setMyLocationEnabled(true);
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(provider);
        double lat, lon;
        if (location != null) {
            lat = location.getLatitude();
            lon = location.getLongitude();
        } else {
            Log.w(TAG, "[MYDEBUG] Last location not available");
            return;
        }

        //Customize the map
        mMap.setPadding(0, 200,0,0);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15.5f));

        //Customize the aspect of the snippets
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

        //Populate the map
        Query query = singleton.retrieveFromDatabase("myDB");
        if (query==null) {
            Log.w(TAG, "[MYDEBUG] No data has been fund on the DB");
            return;
        }
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "[MYDEBUG] onChildAdded");
                Map<String, String> value = (Map<String, String>) dataSnapshot.getValue();
                if (value==null) {
                    Log.w(TAG, "[MYDEBUG] Error in retrieving the element");
                    return;
                }

                //Obtain the timestamp
                String timestamp = value.get("timestamp");
                float opacity = 0.8f;
                Date d = null;
                try {
                    SimpleDateFormat sdfDate = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss", Locale.US);
                    d = sdfDate.parse(timestamp);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (d==null) {
                    Log.e(TAG, "[MYDEBUG] Date has not been parsed");
                    return;
                }
                long timeDiff = new Date().getTime()-d.getTime();
                if (timeDiff>(long)(5*24*60*60*1000)) {
                    //it is more than 5 days old
                    Log.i(TAG, "[MYDEBUG] Very old measurement: more than 5 days old");
                    return;
                } else if (timeDiff>(long)(2*24*60*60*1000) && timeDiff<=(long)(5*24*60*60*1000))
                    opacity = 0.5f;

                //Obtain the location
                String receivedCoordinates = value.get("coordinates");
                String[] latLon = receivedCoordinates.split("-");
                double receivedLat = Double.parseDouble(latLon[0]);
                double receivedLon = Double.parseDouble(latLon[1]);
                Location pointPos = new Location("");
                pointPos.setLatitude(receivedLat);
                pointPos.setLongitude(receivedLon);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "[MYDEBUG] Without permission you can't go on");
                    return;
                }

                Location myPos = locationManager.getLastKnownLocation(provider);
                if (myPos == null) {
                    Log.w(TAG, "[MYDEBUG] Last location not available");
                    return;
                }
                float distance = myPos.distanceTo(pointPos);
                if (distance>5000) {
                    //distance in metres
                    Log.i(TAG, "[MYDEBUG] Very far point: more than 5Km far");
                    return;
                }

                //Obtain the activity
                String activity = value.get("activity");

                //Obtain the noise
                double noise;
                try {
                    noise = Double.parseDouble(value.get("noise"));
                } catch (NullPointerException e) {
                    Log.e(TAG, "Null pointer exception");
                    return;
                }
                catch (NumberFormatException e) {
                    Log.e(TAG, "Number format exception");
                    return;
                }

                //Set the marker
                MarkerOptions opt = new MarkerOptions()
                        .position(new LatLng(receivedLat, receivedLon))
                        .visible(true)
                        .alpha(opacity)
                        .flat(true)
                        .title(timestamp)
                        .snippet("Noise: "+noise+"dB\n"+"Activity: "+activity+"\n"
                        +"Distance from your position: "+(int)distance+"m\n");
                if (noise>75)
                    opt.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_red));
                else if (noise>65 && noise<=75)
                    opt.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_orange));
                else if (noise>40 && noise<=65)
                    opt.icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_yellow));
                else if (noise<40)
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // close this activity and return to preview activity (if there is any)
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}