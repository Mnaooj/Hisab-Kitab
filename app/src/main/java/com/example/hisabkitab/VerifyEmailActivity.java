package com.example.hisabkitab;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends Activity {

    TextView txtMessage;
    Button btnRefresh;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify_email);

        txtMessage = findViewById(R.id.txtMessage);
        btnRefresh = findViewById(R.id.btnRefresh);

        auth = FirebaseAuth.getInstance();

        txtMessage.setText("A verification email has been sent to your email. Please check inbox or spam.");

        btnRefresh.setOnClickListener(v -> checkEmailVerification());
    }

    private void checkEmailVerification() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    Toast.makeText(this, "Email verified! You can now login.", Toast.LENGTH_LONG).show();
                    auth.signOut(); // make sure to sign out before login
                    startActivity(new Intent(VerifyEmailActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "Email not verified yet. Please check your inbox/spam.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}