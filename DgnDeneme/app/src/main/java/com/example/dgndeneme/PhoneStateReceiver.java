package com.example.dgndeneme;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneStateReceiver extends BroadcastReceiver {

    private static final String TAG = "PhoneStateReceiver";

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (state != null) {
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                Log.d(TAG, "Telefon çalıyor. Arayan numara: " + incomingNumber);
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                Log.d(TAG, "Telefon görüşmesi başlatıldı.");
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                Log.d(TAG, "Telefon görüşmesi sona erdi.");
                // Arama bitiş zamanını kaydedin
                MainPage.updateCallEndTime(context);
            } else {
                Log.d(TAG, "Bilinmeyen arama durumu: " + state);
            }
        }
    }
}
