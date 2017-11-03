package it.unipi.iet.noisemap.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import it.unipi.iet.noisemap.Utils.SingletonClass;

public class PowerManagementReceiver extends BroadcastReceiver {
    private final String TAG = "PowerManagementReceiver";
    private SingletonClass singleton = SingletonClass.getInstance();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()==Intent.ACTION_BATTERY_LOW) {
            Log.d(TAG, "[MYDEBUG] Action battery low");
            singleton.scheduleServiceStop(context);
        } else if (intent.getAction()==Intent.ACTION_BATTERY_OKAY) {
            Log.d(TAG, "[MYDEBUG] Action battery ok");
            singleton.scheduleServiceStart(context);
        }
    }
}
