package com.example.dgndeneme;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;

/**
 * MainPage aktivitesi, kullanıcının ana sayfasını ve giden arama işlemlerini yönetir.
 */
public class MainPage extends AppCompatActivity {
    private static final String TAG = "MainPage";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private DatabaseReference userOutgoingRef;
    private ValueEventListener outgoingListener;
    private boolean doubleBackToExitPressedOnce = false;

    private String callEndTime;
    private boolean callInProgress = false;
    private DataSnapshot currentSnapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        checkPermissions();

        // FirebaseListenerService servisini başlat
        Intent intent = new Intent(this, FirebaseListenerService.class);
        startService(intent);
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE},
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permissions are already granted, proceed with setting up the PhoneStateListener
            setupPhoneStateListener();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with setting up the PhoneStateListener
                setupPhoneStateListener();
            } else {
                Toast.makeText(this, "Permission denied. The app cannot function without these permissions.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Firebase referanslarını ayarla ve dinleyiciyi başlat
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        String userId = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("userId", null);
        if (userId != null) {
            DatabaseReference userRef = databaseReference.child("Users").child(userId);
            userOutgoingRef = userRef.child("Outgoings");
            startOutgoingListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Outgoings dinleyicisini kaldır
        if (outgoingListener != null && userOutgoingRef != null) {
            userOutgoingRef.removeEventListener(outgoingListener);
        }
    }

    /**
     * Outgoings düğümünü dinlemeye başlar.
     */
    private void startOutgoingListener() {
        outgoingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.child("PhoneNumber").exists() && snapshot.child("isCalled").exists()) {
                        String phoneNumber = snapshot.child("PhoneNumber").getValue(String.class);
                        Integer isCalled = snapshot.child("isCalled").getValue(Integer.class);
                        if (!TextUtils.isEmpty(phoneNumber) && isCalled != null && isCalled == 0) {
                            makeCall(phoneNumber, snapshot);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase veri dinleme iptal edildi: " + databaseError.getMessage());
            }
        };
        userOutgoingRef.addValueEventListener(outgoingListener);
    }

    /**
     * Telefon numarasını arar ve isCalled değerini günceller.
     *
     * @param phoneNumber Aranacak telefon numarası
     * @param snapshot İlgili veri snapshot'ı
     */
    private void makeCall(String phoneNumber, DataSnapshot snapshot) {
        String callStartTime = getCurrentDateTime();
        callInProgress = true;
        currentSnapshot = snapshot;

        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(callIntent);
            snapshot.getRef().child("isCalled").setValue(1);
            snapshot.getRef().child("callStartTime").setValue(callStartTime);
            Log.d(TAG, "Arama başlama zamanı kaydedildi: " + callStartTime + ", Telefon numarası: " + phoneNumber);
        } else {
            Log.w(TAG, "Arama yapma izni yok.");
        }
    }

    private void setupPhoneStateListener() {
        Log.d(TAG, "PhoneStateListener kuruluyor."); // Log mesajı eklendi
        // TelephonyManager ve PhoneStateListener'ı başlat
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                Log.d(TAG, "onCallStateChanged çağrıldı: state=" + state + ", phoneNumber=" + phoneNumber); // Log mesajı eklendi

                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d(TAG, "Telefon görüşmesi başlatıldı.");
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (callInProgress) {
                            callEndTime = getCurrentDateTime();
                            if (currentSnapshot != null) {
                                currentSnapshot.getRef().child("callEndTime").setValue(callEndTime);
                                currentSnapshot.getRef().child("isCalled").setValue(2); // isCalled değerini 2 olarak güncelle
                                Log.d(TAG, "Arama bitiş zamanı ve isCalled kaydedildi: " + callEndTime);
                            } else {
                                Log.d(TAG, "currentSnapshot null");
                            }
                            callInProgress = false;
                        } else {
                            Log.d(TAG, "callInProgress false");
                        }
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(TAG, "Telefon çalıyor: " + phoneNumber);
                        break;
                    default:
                        Log.d(TAG, "Bilinmeyen arama durumu: " + state);
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private String getCurrentDateTime() {
        return DateFormat.getDateTimeInstance().format(new Date());
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LogIn.class);
            startActivity(intent);
            finish();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Çıkış yapmak için tekrar geri butonuna basın", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    public void cikisYap(View view) {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LogIn.class);
        startActivity(intent);
        finish();
    }
}
