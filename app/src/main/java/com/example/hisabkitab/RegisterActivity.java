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
    DatabaseHandler dbHandler;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dbHandler = new DatabaseHandler(this);

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

    private boolean isNetworkAvailable() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo network = cm.getActiveNetworkInfo();
            return network != null && network.isConnected();
        }

        return false;
    }

    private void registerUser() {

        if (!isNetworkAvailable()) {
            Toast.makeText(this,
                    "No internet! Connect to internet first.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) ||
                TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password)) {

            Toast.makeText(this,
                    "All fields required",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateAccount.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    btnCreateAccount.setEnabled(true);

                    if (task.isSuccessful()) {

                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {

                            // Send verification email
                            user.sendEmailVerification();

                            // Save user to Firestore
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("name", name);
                            userMap.put("email", email);

                            db.collection("users")
                                    .document(user.getUid())
                                    .set(userMap);

                            // Save user locally for offline login
                            dbHandler.insertUser(user.getUid(), email, password, name);

                            Toast.makeText(this,
                                    "Account created! Verify your email.",
                                    Toast.LENGTH_LONG).show();

                            startActivity(new Intent(this, VerifyEmailActivity.class));
                            finish();
                        }

                    } else {

                        Toast.makeText(this,
                                "Registration failed: " +
                                        task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}