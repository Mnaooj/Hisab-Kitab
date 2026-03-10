package com.example.hisabkitab;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.text.TextUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends Activity {
    TextView txtGoToLogin;
    EditText edtName, edtEmail, edtPassword;
    Button btnCreateAccount;

    FirebaseAuth auth;
    FirebaseFirestore db;
    DatabaseHandler dbHandler; // SQLite handler

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.register);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // SQLite
        dbHandler = new DatabaseHandler(this);

        // Views
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        txtGoToLogin = findViewById(R.id.txtGoToLogin);

        btnCreateAccount.setOnClickListener(v -> registerUser());

        txtGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    // 🔹 Check internet connectivity
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    // 🔹 Register user
    private void registerUser() {

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet! Connect to Wi-Fi or mobile data.", Toast.LENGTH_LONG).show();
            return;
        }

        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateAccount.setEnabled(false);

        // 🔹 Check if user already exists
        auth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    btnCreateAccount.setEnabled(true);

                    if (task.isSuccessful()) {
                        boolean isNewUser = task.getResult().getSignInMethods().isEmpty();

                        if (!isNewUser) {
                            Toast.makeText(this, "Email already exists! Try login.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // 🔹 Create Firebase user
                        auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(regTask -> {
                                    btnCreateAccount.setEnabled(true);

                                    if (regTask.isSuccessful()) {
                                        FirebaseUser user = auth.getCurrentUser();
                                        if (user != null) {
                                            // 🔹 Send verification email
                                            user.sendEmailVerification()
                                                    .addOnSuccessListener(unused -> {
                                                        // 🔹 Store user in Firestore
                                                        Map<String, Object> userMap = new HashMap<>();
                                                        userMap.put("name", name);
                                                        userMap.put("email", email);

                                                        db.collection("users")
                                                                .document(user.getUid())
                                                                .set(userMap);

                                                        // 🔹 Store in SQLite for offline login
                                                        dbHandler.insertUser(user.getUid(), email, password, name);

                                                        // 🔹 Sign out user to prevent login before verification
                                                        auth.signOut();

                                                        // 🔹 Go to VerifyEmail page
                                                        Intent intent = new Intent(RegisterActivity.this, VerifyEmailActivity.class);
                                                        intent.putExtra("email", email);
                                                        startActivity(intent);
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(this, "Failed to send verification email: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    });
                                        }

                                    } else {
                                        Toast.makeText(this, "Registration failed: " + regTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });

                    } else {
                        Toast.makeText(this, "Error checking email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}