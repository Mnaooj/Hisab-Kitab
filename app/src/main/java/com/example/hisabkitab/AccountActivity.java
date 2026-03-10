package com.example.hisabkitab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountActivity extends Activity {

    TextView txtName, txtEmail;
    Button btnLogout, btnDeleteAccount;

    FirebaseAuth mAuth;
    FirebaseFirestore firestore;
    DatabaseHandler dbHandler; // SQLite

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account);

        // Views
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // SQLite
        dbHandler = new DatabaseHandler(this);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            txtEmail.setText(user.getEmail());
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                txtName.setText(user.getDisplayName());
            } else {
                txtName.setText("Welcome User");
            }
        }

        // Logout
        btnLogout.setOnClickListener(v -> logoutUser());

        // Delete Account
        btnDeleteAccount.setOnClickListener(v -> deleteAccount(user));
    }

    // --------------------------
    // LOGOUT METHOD
    // --------------------------
    private void logoutUser() {
        // No internet required
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // --------------------------
    // DELETE ACCOUNT METHOD
    // --------------------------
    private void deleteAccount(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = user.getEmail();

        // Ask user for password to re-authenticate
        EditText edtPassword = new EditText(this);
        edtPassword.setHint("Enter your password");

        new AlertDialog.Builder(this)
                .setTitle("Confirm Account Deletion")
                .setMessage("For security, please enter your password to delete your account.")
                .setView(edtPassword)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String password = edtPassword.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Reauthenticate user
                    user.reauthenticate(EmailAuthProvider.getCredential(email, password))
                            .addOnSuccessListener(aVoid -> {

                                // Delete Firestore data
                                firestore.collection("users")
                                        .document(user.getUid())
                                        .delete()
                                        .addOnSuccessListener(aVoid1 -> {

                                            // Delete Firebase user
                                            user.delete()
                                                    .addOnSuccessListener(aVoid2 -> {

                                                        // Delete local SQLite user
                                                        dbHandler.deleteUser(email);

                                                        Toast.makeText(this, "Account deleted successfully!", Toast.LENGTH_LONG).show();

                                                        // Redirect to Login
                                                        Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);

                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(this, "Failed to delete user: " + e.getMessage(), Toast.LENGTH_LONG).show());

                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Failed to delete Firestore data: " + e.getMessage(), Toast.LENGTH_LONG).show());

                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Reauthentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show());

                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}