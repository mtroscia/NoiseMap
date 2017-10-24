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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getNoise() {
        return noise;
    }

    public void setNoise(double noise) {
        this.noise = noise;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }
}
