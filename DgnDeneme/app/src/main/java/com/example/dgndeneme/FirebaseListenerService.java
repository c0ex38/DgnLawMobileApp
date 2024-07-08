package com.example.dgndeneme;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Bu servis, Firebase veritabanındaki değişiklikleri dinler ve belirli koşullar sağlandığında CallHandlingService servisini başlatır.
 */
public class FirebaseListenerService extends Service {

    private static final String TAG = "FirebaseListenerService";

    @Override
    public void onCreate() {
        super.onCreate();
        listenForFirebaseChanges();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        listenForFirebaseChanges();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void listenForFirebaseChanges() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        DatabaseReference phoneNumberRef = databaseReference.child("PhoneNumber");

        phoneNumberRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String phoneNumber = dataSnapshot.child("number").getValue(String.class);
                Integer isCalled = dataSnapshot.child("isCalled").getValue(Integer.class);

                if (phoneNumber != null && isCalled != null && isCalled == 0) {
                    // Arama işleme servisini başlat
                    Context context = getApplicationContext();
                    Intent serviceIntent = new Intent(context, CallHandlingService.class);
                    serviceIntent.putExtra("phoneNumber", phoneNumber);
                    context.startService(serviceIntent);
                    Log.d(TAG, "Arama işleme servisi başlatıldı: phoneNumber=" + phoneNumber);
                } else {
                    Log.w(TAG, "Gelen veriler işlenemedi: phoneNumber=" + phoneNumber + ", isCalled=" + isCalled);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase veritabanı hatası: " + databaseError.getMessage());
            }
        });
    }
}
