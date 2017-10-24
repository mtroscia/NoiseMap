package it.unipi.iet.noisemap;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import it.unipi.iet.noisemap.Utils.DatabaseEntry;

public class DatabaseHandler {
    private final static String TAG = "DatabaseHandler";
    private FirebaseDatabase database = null;

    /*Map<String,String> toMap() {
            Map<String,String> map = new ArrayMap<>();
            map.put("timestamp", getTimestamp());
            String coord = getLat()+"-"+getLon();
            map.put("coordinates", coord);
            String noise_s=String.format("%.1f", getNoise());
            map.put("noise", noise_s);
            map.put("activity", getActivity());
            return map;
        }

    }*/

    public DatabaseHandler() {
        if (database==null)
            database = FirebaseDatabase.getInstance();
        Log.d(TAG, "[DEBUG] Obtained reference to Firebase\n");
    }

    public void insertIntoDatabase(String databaseName, DatabaseEntry e) {
        Log.d(TAG, "[DEBUG] In insertIntoDatabase\n");
        DatabaseReference entryRef = database.getReference(databaseName);
        if (entryRef!=null)
            Log.d(TAG, "[DEBUG] Obtained reference to my DB\n");
        DatabaseReference newChild = entryRef.push();
        Log.d(TAG, "[DEBUG] New child");

        Map<String,String> map = new ArrayMap<>();
        map.put("timestamp", e.getTimestamp());
        String coord = e.getLat()+"-"+e.getLon();
        map.put("coordinates", coord);
        String noise_s=String.format("%.1f", e.getNoise());
        map.put("noise", noise_s);
        map.put("activity", e.getActivity());
        newChild.setValue(map);
        Log.d(TAG, "[DEBUG] Entry is set");

        entryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                /* This method is called once with the initial value and again
                   whenever data at this location is updated. */
                Log.d(TAG, "[DEBUG] onDataChange");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.d(TAG, "[DEBUG] onCancelled");
                Log.w(TAG, "[DEBUG] Failed to read value", error.toException());
            }
        });
    }

    public Query retrieveFromDatabase(String databaseName) {
        Log.d(TAG, "[DEBUG] in retrieveFromDatabase");
        DatabaseReference entryRef = database.getReference(databaseName);
        if (entryRef==null) {
            Log.d(TAG, "[DEBUG] Not obtained reference to my DB\n");
            return null;
        }
        Log.d(TAG, "[DEBUG] Obtained reference to my DB\n");
        Query query = entryRef.orderByChild("timestamp")
                /*.addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<DatabaseEntry> app = new ArrayList<>();
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
                        DatabaseEntry e = new DatabaseEntry(timestamp, latt, lonn, noise, activity);
                        app.add(e);
                    }
                    list = app;
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            })*/;
        Log.d(TAG, "[DEBUG] Return query\n");
        return query;
    }

}
