package it.unipi.iet.noisemap;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.Date;
import java.util.List;

import it.unipi.iet.noisemap.Utils.DatabaseEntry;

public class ActivityRecognizedService extends IntentService {
    public static DetectedActivity mostProbableActivity = null;
    private final String TAG = "ActivityRecognizedServ";
    Handler mHandler;
    DatabaseHandler dbHandler;

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
        Log.d(TAG, "[DEBUG] in ActivityRecognizedService()\n");
        mHandler = new Handler();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "Activity recognition started";
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });
        dbHandler = new DatabaseHandler();
    }

    @Override
    public void onHandleIntent(Intent intent) {
        //The method is invoked every time a new result is sensed.
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            //handleDetectedActivities( result.getProbableActivities() );
            mostProbableActivity = result.getMostProbableActivity();
            Log.d(TAG, "[DEBUG] The most probable activity is "+activityToString(mostProbableActivity));


            //TODO: IT HAS TO ASK FOR LAT/LON
            double lat = 43.734235;
            double lon = 10.864575;

            //TODO: HANDLE A UNIQUE CAPTURE AUDIO
            int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
            bufferSize = bufferSize * 4;
            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            short data[] = new short[bufferSize];
            recorder.startRecording();
            recorder.read(data, 0, bufferSize);
            recorder.stop();
            recorder.release();
            double average = 0.0;
            for (short s : data) {
                if (s > 0)
                    average += Math.abs(s);
                else
                    bufferSize--;
            }
            double x = average / bufferSize;
            //String x_s = String.format("%.1f", x);

            //Calculating the Pascal pressure based on the idea that the max amplitude
            //(between 0 and 32767) is relative to the pressure.
            //The value 51805.5336 can be derived from assuming that x=32767=0.6325Pa and
            //x=1=0.00002Pa (the reference value)
            double pressure = x / 51805.5336;
            double noise = (20 * Math.log10(pressure / 0.00002));

            DatabaseEntry e = new DatabaseEntry(new Date().toString(), lat, lon, noise, activityToString(mostProbableActivity));
            //dbHandler.insertIntoDatabase("myDB", e);
        }

        /*
            //it has to ask for lat/lon
            lat = 43.734235;
            lon = 10.864575;
            int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
            bufferSize = bufferSize * 4;
            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            short data[] = new short[bufferSize];
            recorder.startRecording();
            recorder.read(data, 0, bufferSize);
            recorder.stop();
            recorder.release();
            double average = 0.0;
            for (short s : data) {
                if (s > 0)
                    average += Math.abs(s);
                else
                    bufferSize--;
            }
            double x = average / bufferSize;
            //String x_s = String.format("%.1f", x);

            //Calculating the Pascal pressure based on the idea that the max amplitude
            //(between 0 and 32767) is relative to the pressure.
            //The value 51805.5336 can be derived from assuming that x=32767=0.6325Pa and
            //x=1=0.00002Pa (the reference value)
            double pressure = x / 51805.5336;
            noise = (20 * Math.log10(pressure / 0.00002));

            activity = "still";

            //create a new Entry() and ask the DatabaseHandler to submit it*/
    }

    public static String activityToString(DetectedActivity activity) {
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

    /*private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        for( DetectedActivity activity : probableActivities ) {
            switch( activity.getType() ) {
                case DetectedActivity.IN_VEHICLE: {
                    Log.d(TAG, "[DEBUG] In vehicle: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    Log.d(TAG, "[DEBUG] On bicycle: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    Log.d(TAG, "[DEBUG] On foot: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.RUNNING: {
                    Log.d(TAG, "[DEBUG] Running: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.STILL: {
                    Log.d(TAG, "[DEBUG] Still: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.TILTING: {
                    Log.d(TAG, "[DEBUG] Tilting: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.WALKING: {
                    Log.d(TAG, "[DEBUG] Walking: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    Log.d(TAG, "[DEBUG] Unknown: " + activity.getConfidence() );
                    break;
                }
            }
        }
    }*/
}
