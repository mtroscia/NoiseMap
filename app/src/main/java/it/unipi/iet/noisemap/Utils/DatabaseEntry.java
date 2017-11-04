package it.unipi.iet.noisemap.Utils;

public class DatabaseEntry {
    private String timestamp;
    private double lat;
    private double lon;
    private double noise;
    private String activity;

    public DatabaseEntry (String timestamp, double lat, double lon, double noise, String activity) {
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;
        this.noise = noise;
        this.activity = activity;
    }

    public String toString() {
        String s = "Timestamp: "+timestamp;
        s+=" - Coordinates: "+lat+" "+lon;
        s+=" - Noise: "+noise+"dB";
        s+=" - Activity: "+activity;
        return s;
    }

    String getTimestamp() {
        return timestamp;
    }

    double getLat() {
        return lat;
    }

    double getLon() {
        return lon;
    }

    double getNoise() {
        return noise;
    }

    String getActivity() {
        return activity;
    }
}
