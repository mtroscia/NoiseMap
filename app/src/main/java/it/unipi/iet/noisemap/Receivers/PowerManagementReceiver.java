package it.unipi.iet.noisemap.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import it.unipi.iet.noisemap.Utils.SingletonClass;

public class PowerManagementReceiver extends BroadcastReceiver {
    private SingletonClass singleton = SingletonClass.getInstance();

    @Override
    public void onReceive(Context context, Intent intent) {
        final String TAG = "PowerManagementReceiver";
        if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
            Log.i(TAG, "[MYDEBUG] Action battery low");
            singleton.scheduleServiceStop();
        } else if (Intent.ACTION_BATTERY_OKAY.equals(intent.getAction())) {
            Log.i(TAG, "[MYDEBUG] Action battery ok");
            singleton.scheduleServiceStart();
        }
    }
}
