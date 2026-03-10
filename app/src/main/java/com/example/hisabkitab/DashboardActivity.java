package com.example.hisabkitab;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.widget.*;
import android.database.Cursor;
import android.graphics.Color;
import android.view.ViewGroup;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashboardActivity extends Activity {

    LinearLayout navBtnAccount, transactionContainer;
    Button btnAddIncome, btnAddExpense;

    TextView tvHelloName, tvBalance, tvIncome, tvExpense, tvSyncStatus;

    DatabaseHandler db;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String currentUserUid;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();


        currentUserUid = currentUser.getUid();

        // Initialize views
        navBtnAccount = findViewById(R.id.navBtnAccount);
        transactionContainer = findViewById(R.id.transactionContainer);
        btnAddIncome = findViewById(R.id.btnAddIncome);
        btnAddExpense = findViewById(R.id.btnAddExpense);

        tvHelloName = findViewById(R.id.tvHelloName);
        tvBalance = findViewById(R.id.tvBalance);
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);

        db = new DatabaseHandler(this);

        setUserName();
        loadDashboardData();

        navBtnAccount.setOnClickListener(v ->
                startActivity(new Intent(this, AccountActivity.class)));

        btnAddIncome.setOnClickListener(v ->
                startActivity(new Intent(this, AddIncomeActivity.class)));

        btnAddExpense.setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));
    }

    private void setUserName() {

        if (currentUser.getDisplayName() != null &&
                !currentUser.getDisplayName().isEmpty()) {

            tvHelloName.setText("Hello, " + currentUser.getDisplayName());

        } else if (currentUser.getEmail() != null) {

            String emailName = currentUser.getEmail().split("@")[0];
            tvHelloName.setText("Hello, " + emailName);

        } else {
            tvHelloName.setText("Hello, User");
        }
    }

    private void loadDashboardData() {

        transactionContainer.removeAllViews();

        double totalIncome = 0;
        double totalExpense = 0;
        boolean hasUnsynced = false;

        // 🔹 Load Income
        Cursor incomeCursor = db.getIncome(currentUserUid);
        if (incomeCursor != null) {
            while (incomeCursor.moveToNext()) {

                double amount = incomeCursor.getDouble(
                        incomeCursor.getColumnIndexOrThrow("amount"));

                totalIncome += amount;

                int synced = incomeCursor.getInt(
                        incomeCursor.getColumnIndexOrThrow("synced"));

                if (synced == 0) hasUnsynced = true;

                addTransactionView(
                        incomeCursor.getString(
                                incomeCursor.getColumnIndexOrThrow("title")),
                        amount,
                        true
                );
            }
            incomeCursor.close();
        }

        // 🔹 Load Expense
        Cursor expenseCursor = db.getExpenses(currentUserUid);
        if (expenseCursor != null) {
            while (expenseCursor.moveToNext()) {

                double amount = expenseCursor.getDouble(
                        expenseCursor.getColumnIndexOrThrow("amount"));

                totalExpense += amount;

                int synced = expenseCursor.getInt(
                        expenseCursor.getColumnIndexOrThrow("synced"));

                if (synced == 0) hasUnsynced = true;

                addTransactionView(
                        expenseCursor.getString(
                                expenseCursor.getColumnIndexOrThrow("title")),
                        amount,
                        false
                );
            }
            expenseCursor.close();
        }

        double balance = totalIncome - totalExpense;

        tvIncome.setText("+ Rs " + totalIncome);
        tvExpense.setText("- Rs " + totalExpense);
        tvBalance.setText("Rs " + balance);

        if (hasUnsynced) {
            tvSyncStatus.setText("Not Synced");
            tvSyncStatus.setTextColor(Color.RED);
        } else {
            tvSyncStatus.setText("All Synced");
            tvSyncStatus.setTextColor(Color.WHITE);
        }
    }

    private void addTransactionView(String title, double amount, boolean isIncome) {

        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 20, 0, 20);

        TextView tvTitle = new TextView(this);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tvTitle.setText(title);
        tvTitle.setTextSize(14);

        TextView tvAmount = new TextView(this);
        tvAmount.setTextSize(14);
        tvAmount.setTextColor(isIncome ?
                Color.parseColor("#2E7D32") :
                Color.parseColor("#E53935"));

        tvAmount.setText((isIncome ? "+ Rs " : "- Rs ") + amount);

        row.addView(tvTitle);
        row.addView(tvAmount);

        transactionContainer.addView(row);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }
}