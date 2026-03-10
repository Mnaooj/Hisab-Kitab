package com.example.hisabkitab;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddExpenseActivity extends AppCompatActivity {

    EditText edtAmount, edtDate, edtTitle, edtDescription, edtNewCategory;
    Button btnAddExpense;
    GridLayout gridCategory;

    String selectedCategory = "";

    DatabaseHandler db;
    FirebaseAuth auth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_expense);

        db = new DatabaseHandler(this);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        edtAmount = findViewById(R.id.edtAmount);
        edtDate = findViewById(R.id.edtDate);
        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtNewCategory = findViewById(R.id.edtNewCategory);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        gridCategory = findViewById(R.id.gridCategory);

        setupDatePicker();
        setupCategorySelection();

        btnAddExpense.setOnClickListener(v -> saveExpense());
    }

    // ✅ Internet check
    private boolean isNetworkAvailable() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }

        return false;
    }

    private void setupDatePicker() {

        edtDate.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            new DatePickerDialog(this,
                    (view, year, month, day) ->
                            edtDate.setText(day + "/" + (month + 1) + "/" + year),

                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void setupCategorySelection() {

        for (int i = 0; i < gridCategory.getChildCount(); i++) {

            View view = gridCategory.getChildAt(i);

            if (view instanceof TextView) {

                view.setOnClickListener(v -> {

                    for (int j = 0; j < gridCategory.getChildCount(); j++) {
                        gridCategory.getChildAt(j)
                                .setBackgroundResource(R.drawable.bg_category_unselected);
                    }

                    v.setBackgroundResource(R.drawable.bg_category_selected);

                    selectedCategory = ((TextView) v).getText().toString();

                    if (selectedCategory.equals("Other")) {
                        edtNewCategory.setVisibility(View.VISIBLE);
                    } else {
                        edtNewCategory.setVisibility(View.GONE);
                    }
                });
            }
        }
    }
    private void saveExpense() {

        String amountStr = edtAmount.getText().toString().trim();
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String date = edtDate.getText().toString().trim();

        if (amountStr.isEmpty() || title.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory.equals("Other")) {
            selectedCategory = edtNewCategory.getText().toString().trim();
        }

        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please select category", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        String userUid = "offline_user";

        if (auth.getCurrentUser() != null) {
            userUid = auth.getCurrentUser().getUid();
        }

        // 1️⃣ ALWAYS SAVE LOCALLY FIRST
        long localId = db.insertExpense(
                "",
                userUid,
                title,
                amount,
                selectedCategory,
                description,
                date,
                0
        );

        Toast.makeText(this, "Expense saved locally", Toast.LENGTH_SHORT).show();

        // 2️⃣ TRY SYNC IF INTERNET EXISTS
        SyncManager.syncData(this);

        finish();
    }
    }
}