package com.example.hisabkitab;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.widget.*;
import android.database.Cursor;
import android.graphics.Color;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.*;

public class DashboardActivity extends Activity {

    LinearLayout navBtnAccount, transactionContainer;

    Button btnAddIncome, btnAddExpense;
    Button btnFilterAll, btnFilterIncome, btnFilterExpense;

    TextView tvHelloName, tvBalance, tvIncome, tvExpense, tvSyncStatus;

    DatabaseHandler db;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String currentUserUid;

    SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle b) {

        super.onCreate(b);
        setContentView(R.layout.dashboard);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        currentUserUid = currentUser.getUid();

        navBtnAccount = findViewById(R.id.navBtnAccount);
        transactionContainer = findViewById(R.id.transactionContainer);

        btnAddIncome = findViewById(R.id.btnAddIncome);
        btnAddExpense = findViewById(R.id.btnAddExpense);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterIncome = findViewById(R.id.btnFilterIncome);
        btnFilterExpense = findViewById(R.id.btnFilterExpense);

        tvHelloName = findViewById(R.id.tvHelloName);
        tvBalance = findViewById(R.id.tvBalance);
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);

        db = new DatabaseHandler(this);

        SyncManager.syncData(this);

        setUserName();
        loadAllTransactions();

        navBtnAccount.setOnClickListener(v ->
                startActivity(new Intent(this, AccountActivity.class)));

        btnAddIncome.setOnClickListener(v ->
                startActivity(new Intent(this, AddIncomeActivity.class)));

        btnAddExpense.setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));

        btnFilterAll.setOnClickListener(v -> {
            setFilterUI(0);
            loadAllTransactions();
        });

        btnFilterIncome.setOnClickListener(v -> {
            setFilterUI(1);
            loadIncomeOnly();
        });

        btnFilterExpense.setOnClickListener(v -> {
            setFilterUI(2);
            loadExpenseOnly();
        });
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

    private void setFilterUI(int selected) {

        btnFilterAll.setBackgroundResource(
                selected == 0 ? R.drawable.filter_selected : R.drawable.filter_unselected);

        btnFilterIncome.setBackgroundResource(
                selected == 1 ? R.drawable.filter_selected : R.drawable.filter_unselected);

        btnFilterExpense.setBackgroundResource(
                selected == 2 ? R.drawable.filter_selected : R.drawable.filter_unselected);
    }

    // ---------- LOAD ALL TRANSACTIONS ----------

    private void loadAllTransactions() {

        transactionContainer.removeAllViews();

        List<TransactionItem> allTransactions = new ArrayList<>();

        double totalIncome = 0;
        double totalExpense = 0;
        boolean hasUnsynced = false;

        Cursor incomeCursor = db.getIncome(currentUserUid);

        if (incomeCursor != null) {

            while (incomeCursor.moveToNext()) {

                String title = incomeCursor.getString(incomeCursor.getColumnIndexOrThrow("title"));
                double amount = incomeCursor.getDouble(incomeCursor.getColumnIndexOrThrow("amount"));
                String date = incomeCursor.getString(incomeCursor.getColumnIndexOrThrow("date"));
                int synced = incomeCursor.getInt(incomeCursor.getColumnIndexOrThrow("synced"));

                totalIncome += amount;
                if (synced == 0) hasUnsynced = true;

                allTransactions.add(new TransactionItem(title, amount, date, true, synced));
            }

            incomeCursor.close();
        }

        Cursor expenseCursor = db.getExpenses(currentUserUid);

        if (expenseCursor != null) {

            while (expenseCursor.moveToNext()) {

                String title = expenseCursor.getString(expenseCursor.getColumnIndexOrThrow("title"));
                double amount = expenseCursor.getDouble(expenseCursor.getColumnIndexOrThrow("amount"));
                String date = expenseCursor.getString(expenseCursor.getColumnIndexOrThrow("date"));
                int synced = expenseCursor.getInt(expenseCursor.getColumnIndexOrThrow("synced"));

                totalExpense += amount;
                if (synced == 0) hasUnsynced = true;

                allTransactions.add(new TransactionItem(title, amount, date, false, synced));
            }

            expenseCursor.close();
        }

        sortTransactions(allTransactions);

        for (TransactionItem tx : allTransactions) {

            addTransactionView(tx.title, tx.amount, tx.date, tx.isIncome);
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

    // ---------- INCOME FILTER ----------

    private void loadIncomeOnly() {

        transactionContainer.removeAllViews();

        List<TransactionItem> list = new ArrayList<>();

        Cursor cursor = db.getIncome(currentUserUid);

        if (cursor != null) {

            while (cursor.moveToNext()) {

                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

                list.add(new TransactionItem(title, amount, date, true, 1));
            }

            cursor.close();
        }

        sortTransactions(list);

        for (TransactionItem tx : list) {
            addTransactionView(tx.title, tx.amount, tx.date, true);
        }
    }

    // ---------- EXPENSE FILTER ----------

    private void loadExpenseOnly() {

        transactionContainer.removeAllViews();

        List<TransactionItem> list = new ArrayList<>();

        Cursor cursor = db.getExpenses(currentUserUid);

        if (cursor != null) {

            while (cursor.moveToNext()) {

                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

                list.add(new TransactionItem(title, amount, date, false, 1));
            }

            cursor.close();
        }

        sortTransactions(list);

        for (TransactionItem tx : list) {
            addTransactionView(tx.title, tx.amount, tx.date, false);
        }
    }

    // ---------- SORTING METHOD ----------

    private void sortTransactions(List<TransactionItem> list) {

        Collections.sort(list, (a, b) -> {

            try {

                Date d1 = sdf.parse(a.date);
                Date d2 = sdf.parse(b.date);

                return d2.compareTo(d1);

            } catch (Exception e) {

                return 0;
            }
        });
    }

    // ---------- UI CREATION ----------

    private void addTransactionView(String title, double amount, String date, boolean isIncome) {

        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 24, 0, 24);

        LinearLayout rowTop = new LinearLayout(this);
        rowTop.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1));

        tvTitle.setText(title);
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.WHITE);

        TextView tvAmount = new TextView(this);
        tvAmount.setTextSize(18);

        tvAmount.setTextColor(isIncome ?
                Color.parseColor("#2E7D32") :
                Color.parseColor("#E53935"));

        tvAmount.setText((isIncome ? "+ Rs " : "- Rs ") + amount);

        rowTop.addView(tvTitle);
        rowTop.addView(tvAmount);

        TextView tvDate = new TextView(this);
        tvDate.setTextSize(15);
        tvDate.setTextColor(Color.LTGRAY);
        tvDate.setText("Date: " + date);

        row.addView(rowTop);
        row.addView(tvDate);

        transactionContainer.addView(row);
    }

    @Override
    protected void onResume() {

        super.onResume();

        SyncManager.syncData(this);

        loadAllTransactions();
    }
}