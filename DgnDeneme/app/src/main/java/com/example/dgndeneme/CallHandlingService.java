package com.example.dgndeneme;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;

/**
 * Bu servis, gelen aramaları yönetir ve Firebase'e kaydeder.
 */
public class CallHandlingService extends Service {

    private static final String TAG = "CallHandlingService";
    private static final String CALL_STATE_START = "CALL_STATE_START";
    private static final String CALL_STATE_END = "CALL_STATE_END";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String phoneNumber = intent.getStringExtra("phoneNumber");
        String callState = intent.getStringExtra("callState");

        if (!TextUtils.isEmpty(phoneNumber) && !TextUtils.isEmpty(callState)) {
            handleCall(this, phoneNumber, callState);
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleCall(Context context, String phoneNumber, String callState) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (!TextUtils.isEmpty(phoneNumber) && userId != null) {
            DatabaseReference userCallLogRef = databaseReference.child("Users").child(userId).child("Incomings");
            userCallLogRef.orderByChild("phoneNumber").equalTo(phoneNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String key = snapshot.getKey();
                            assert key != null;
                            if (CALL_STATE_START.equals(callState)) {
                                userCallLogRef.child(key).child("callStartTime").setValue(currentDateTime);
                                Log.d(TAG, "Gelen arama başladı: Telefon numarası zaten kayıtlı. Başlama zamanı güncellendi.");
                            } else if (CALL_STATE_END.equals(callState)) {
                                userCallLogRef.child(key).child("callEndTime").setValue(currentDateTime);
                                Log.d(TAG, "Gelen arama bitti: Telefon numarası zaten kayıtlı. Bitiş zamanı güncellendi.");
                            }
                        }
                    } else {
                        if (CALL_STATE_START.equals(callState)) {
                            addCallToFirebase(context, phoneNumber, currentDateTime, null);
                            Log.d(TAG, "Gelen arama başladı: Telefon numarası kayıtlı değil. Yeni kayıt eklendi.");
                        } else if (CALL_STATE_END.equals(callState)) {
                            addCallToFirebase(context, phoneNumber, null, currentDateTime);
                            Log.d(TAG, "Gelen arama bitti: Telefon numarası kayıtlı değil. Yeni kayıt eklendi.");
                        }
                    }
                    stopSelf(); // Veri işleme tamamlandıktan sonra servisi durdur
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Firebase veritabanı hatası: " + databaseError.getMessage());
                    stopSelf(); // Hata durumunda servisi durdur
                }
            });
        } else {
            Log.d(TAG, "Gelen arama: phoneNumber veya userId değeri null olduğu için Firebase'e kayıt yapılmadı");
            stopSelf(); // Gerekli bilgiler yoksa servisi durdur
        }
    }

    private void addCallToFirebase(Context context, String phoneNumber, String callStartTime, String callEndTime) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (!TextUtils.isEmpty(phoneNumber)) {
            assert userId != null;
            DatabaseReference userCallLogRef = databaseReference.child("Users").child(userId).child("Incomings").push();
            userCallLogRef.child("phoneNumber").setValue(phoneNumber)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Gelen arama kaydı başarıyla Firebase'e eklendi"))
                    .addOnFailureListener(e -> Log.e(TAG, "Firebase'e arama kaydı eklenirken hata oluştu: " + e.getMessage()));
            if (callStartTime != null) {
                userCallLogRef.child("callStartTime").setValue(callStartTime);
            }
            if (callEndTime != null) {
                userCallLogRef.child("callEndTime").setValue(callEndTime);
            }
        } else {
            Log.d(TAG, "Gelen arama: phoneNumber değeri null olduğu için Firebase'e kayıt yapılmadı");
        }
    }
}
