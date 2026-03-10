package com.example.hisabkitab;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.text.TextUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends Activity {

    EditText edtEmail, edtPassword;
    Button btnLogin;
    TextView txtGoToRegister, txtForgotPassword;

    FirebaseAuth auth;
    DatabaseHandler dbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        auth = FirebaseAuth.getInstance();
        dbHandler = new DatabaseHandler(this);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);

        // If already logged in online
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
            finish();
        }

        btnLogin.setOnClickListener(v -> loginUser());

        txtGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        txtForgotPassword.setOnClickListener(v -> forgotPassword());
    }

    // 🔹 Check Internet
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }

        return false;
    }

    // 🔹 Login Logic
    private void loginUser() {

        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        // ===============================
        // 🔹 ONLINE LOGIN
        // ===============================

        if (isNetworkAvailable()) {

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        btnLogin.setEnabled(true);

                        if (task.isSuccessful()) {

                            FirebaseUser user = auth.getCurrentUser();

                            if (user != null && user.isEmailVerified()) {

                                String name = user.getDisplayName();
                                String uid = user.getUid();

                                // 🔹 Save user locally for offline login
                                dbHandler.insertUser(uid, email, password, name);

                                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                finish();

                            } else {

                                Toast.makeText(this, "Please verify your email first", Toast.LENGTH_LONG).show();
                                auth.signOut();

                            }

                        } else {

                            Toast.makeText(this,
                                    "Login Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();

                        }

                    });

        }

        // ===============================
        // 🔹 OFFLINE LOGIN
        // ===============================

        else {

            boolean exists = dbHandler.checkUser(email, password);

            btnLogin.setEnabled(true);

            if (exists) {

                String name = dbHandler.getUsername(email, password);

                Toast.makeText(this,
                        "Offline Login Successful\nWelcome " + name,
                        Toast.LENGTH_SHORT).show();

                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();

            } else {

                Toast.makeText(this,
                        "No internet & user not found offline",
                        Toast.LENGTH_LONG).show();

            }

        }

    }

    // 🔹 Forgot Password
    private void forgotPassword() {

        String email = edtEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter email first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isNetworkAvailable()) {

            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Reset Email Sent", Toast.LENGTH_LONG).show())

                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());

        }

        else {

            Toast.makeText(this,
                    "No internet. Cannot reset password offline.",
                    Toast.LENGTH_LONG).show();

        }
    }
}