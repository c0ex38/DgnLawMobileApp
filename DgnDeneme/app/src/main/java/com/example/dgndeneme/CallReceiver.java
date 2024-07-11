package com.example.dgndeneme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * Bu broadcast receiver, telefon durumundaki değişiklikleri dinler ve gerekli işlemleri başlatır.
 */
public class CallReceiver extends BroadcastReceiver {

    private static final String TAG = "CallReceiver";
    private static boolean isRinging = false;
    private static boolean isCallReceived = false;
    private static String incomingNumber = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action != null && action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            if (state != null) {
                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    Log.d(TAG, "Telefon çalıyor.");
                    incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    isRinging = true;

                    if (!TextUtils.isEmpty(incomingNumber)) {
                        Intent serviceIntent = new Intent(context, CallHandlingService.class);
                        serviceIntent.putExtra("phoneNumber", incomingNumber);
                        serviceIntent.putExtra("callState", "CALL_STATE_START");
                        context.startService(serviceIntent);
                        Log.d(TAG, "Gelen arama başladı: phoneNumber=" + incomingNumber);
                    } else {
                        Log.w(TAG, "Gelen arama: phoneNumber değeri null veya boş olduğu için işlenmedi");
                    }
                } else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    Log.d(TAG, "Telefon görüşmesi başlatıldı.");
                    if (isRinging) {
                        isCallReceived = true;
                    }
                } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    Log.d(TAG, "Telefon görüşmesi sona erdi.");
                    if (isCallReceived && !TextUtils.isEmpty(incomingNumber)) {
                        Intent serviceIntent = new Intent(context, CallHandlingService.class);
                        serviceIntent.putExtra("phoneNumber", incomingNumber);
                        serviceIntent.putExtra("callState", "CALL_STATE_END");
                        context.startService(serviceIntent);
                        Log.d(TAG, "Gelen arama sona erdi: phoneNumber=" + incomingNumber);
                    }
                    isRinging = false;
                    isCallReceived = false;
                    incomingNumber = null;
                }
            } else {
                Log.e(TAG, "Telefon durumu değeri null.");
            }
        } else {
            Log.e(TAG, "Beklenmeyen aksiyon: " + action);
        }
    }
}