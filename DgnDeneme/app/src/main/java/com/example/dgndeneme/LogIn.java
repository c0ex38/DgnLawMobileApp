package com.example.dgndeneme;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Bu sınıf, kullanıcının giriş yapmasını sağlar. Kullanıcı giriş yaptığında, kimlik bilgilerini doğrulamak için Firebase Authentication kullanır ve başarılı giriş sonrası kullanıcıyı MainPage aktivitesine yönlendirir.
 */
public class LogIn extends AppCompatActivity {
    private EditText editEmail, editSifre;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private boolean doubleBackToExitPressedOnce = false;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.editTextEmail);
        editSifre = findViewById(R.id.editTextPassword);
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void girisYap(View view) {
        String txtEmail = editEmail.getText().toString();
        String txtSifre = editSifre.getText().toString();

        if (!TextUtils.isEmpty(txtEmail) && !TextUtils.isEmpty(txtSifre)) {
            // Kullanıcıya giriş yapılıyor bilgisini göstermek için bir ilerleme çubuğu göster
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Giriş yapılıyor...");
            progressDialog.show();

            mAuth.signInWithEmailAndPassword(txtEmail, txtSifre)
                    .addOnSuccessListener(this, authResult -> {
                        mUser = mAuth.getCurrentUser();
                        if (mUser != null) {
                            saveUserIdToPreferences();
                            progressDialog.dismiss(); // İşlem tamamlandığında ilerleme çubuğunu gizle
                            navigateToMainPage();
                        }
                    }).addOnFailureListener(this, e -> {
                        progressDialog.dismiss(); // İşlem tamamlandığında ilerleme çubuğunu gizle
                        Toast.makeText(LogIn.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Email veya şifre boş olamaz", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserIdToPreferences() {
        String userId = mUser.getUid();
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userId", userId);
        editor.apply();
    }

    private void navigateToMainPage() {
        Intent intent = new Intent(LogIn.this, MainPage.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Çıkış yapmak için tekrar geri butonuna basın", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }
}
