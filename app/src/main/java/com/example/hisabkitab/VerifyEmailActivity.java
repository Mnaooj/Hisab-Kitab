package com.example.hisabkitab;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

        txtMessage.setText(
                "A verification email has been sent.\nPlease check your inbox or spam."
        );

        btnRefresh.setOnClickListener(v -> checkEmailVerification());
    }

    private void checkEmailVerification() {

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {

            user.reload().addOnCompleteListener(task -> {

                if (task.isSuccessful()) {

                    if (user.isEmailVerified()) {

                        Toast.makeText(this,
                                "Email verified successfully!",
                                Toast.LENGTH_LONG).show();

                        startActivity(new Intent(
                                VerifyEmailActivity.this,
                                DashboardActivity.class));

                        finish();

                    } else {

                        Toast.makeText(this,
                                "Email not verified yet.",
                                Toast.LENGTH_LONG).show();
                    }

                } else {

                    Toast.makeText(this,
                            "Verification check failed.",
                            Toast.LENGTH_LONG).show();
                }
            });

        } else {

            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}