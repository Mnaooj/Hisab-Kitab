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
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        auth = FirebaseAuth.getInstance();
        dbHandler = new DatabaseHandler(this);
        sessionManager = new SessionManager(this);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);

        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        }

        btnLogin.setOnClickListener(v -> loginUser());

        txtGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        txtForgotPassword.setOnClickListener(v -> forgotPassword());
    }

    private boolean isNetworkAvailable() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo network = cm.getActiveNetworkInfo();
            return network != null && network.isConnected();
        }

        return false;
    }

    private void loginUser() {

        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {

            Toast.makeText(this,
                    "All fields required",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        // ONLINE LOGIN
        if (isNetworkAvailable()) {

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        btnLogin.setEnabled(true);

                        if (task.isSuccessful()) {

                            FirebaseUser user = auth.getCurrentUser();

                            if (user != null && user.isEmailVerified()) {

                                String uid = user.getUid();
                                String name = dbHandler.getUsername(email, password);

                                sessionManager.saveUserSession(uid, email, name);

                                startActivity(new Intent(this, DashboardActivity.class));
                                finish();

                            } else {

                                Toast.makeText(this,
                                        "Verify your email first",
                                        Toast.LENGTH_LONG).show();

                                startActivity(new Intent(this, VerifyEmailActivity.class));
                            }

                        } else {

                            Toast.makeText(this,
                                    "Login Failed: " +
                                            task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }

        // OFFLINE LOGIN
        else {

            boolean exists = dbHandler.checkUser(email, password);

            btnLogin.setEnabled(true);

            if (exists) {

                String name = dbHandler.getUsername(email, password);

                sessionManager.saveUserSession(
                        "offline_" + email,
                        email,
                        name
                );

                Toast.makeText(this,
                        "Offline Login Successful",
                        Toast.LENGTH_SHORT).show();

                startActivity(new Intent(this, DashboardActivity.class));
                finish();

            } else {

                Toast.makeText(this,
                        "No internet & user not found offline",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void forgotPassword() {

        String email = edtEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this,
                    "Enter email first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (isNetworkAvailable()) {

            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this,
                                    "Reset Email Sent",
                                    Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        }

        else {

            Toast.makeText(this,
                    "No internet. Cannot reset password offline.",
                    Toast.LENGTH_LONG).show();
        }
    }
}