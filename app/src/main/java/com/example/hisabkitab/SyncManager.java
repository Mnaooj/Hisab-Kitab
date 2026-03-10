package com.example.hisabkitab;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SyncManager {

    public static void syncData(Context context) {

        if (!isInternetAvailable(context)) {
            return;
        }

        DatabaseHandler db = new DatabaseHandler(context);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // ======================
        // SYNC EXPENSES
        // ======================

        Cursor expenseCursor = db.getUnsyncedExpenses();

        while (expenseCursor.moveToNext()) {

            int id = expenseCursor.getInt(
                    expenseCursor.getColumnIndexOrThrow("id"));

            String userUid = expenseCursor.getString(
                    expenseCursor.getColumnIndexOrThrow("user_uid"));

            String title = expenseCursor.getString(
                    expenseCursor.getColumnIndexOrThrow("title"));

            double amount = expenseCursor.getDouble(
                    expenseCursor.getColumnIndexOrThrow("amount"));

            String category = expenseCursor.getString(
                    expenseCursor.getColumnIndexOrThrow("category"));

            String description = expenseCursor.getString(
                    expenseCursor.getColumnIndexOrThrow("description"));

            String date = expenseCursor.getString(
                    expenseCursor.getColumnIndexOrThrow("date"));

            Map<String, Object> map = new HashMap<>();

            map.put("userId", userUid);
            map.put("title", title);
            map.put("amount", amount);
            map.put("category", category);
            map.put("description", description);
            map.put("date", date);
            map.put("type", "expense");
            map.put("timestamp", System.currentTimeMillis());

            firestore.collection("users")
                    .document(userUid)
                    .collection("transactions")
                    .add(map)
                    .addOnSuccessListener(documentReference -> {

                        db.markExpenseAsSynced(id,
                                documentReference.getId());

                    });
        }

        expenseCursor.close();

        // ======================
        // SYNC INCOME
        // ======================

        Cursor incomeCursor = db.getUnsyncedIncome();

        while (incomeCursor.moveToNext()) {

            int id = incomeCursor.getInt(
                    incomeCursor.getColumnIndexOrThrow("id"));

            String userUid = incomeCursor.getString(
                    incomeCursor.getColumnIndexOrThrow("user_uid"));

            String title = incomeCursor.getString(
                    incomeCursor.getColumnIndexOrThrow("title"));

            double amount = incomeCursor.getDouble(
                    incomeCursor.getColumnIndexOrThrow("amount"));

            String category = incomeCursor.getString(
                    incomeCursor.getColumnIndexOrThrow("category"));

            String description = incomeCursor.getString(
                    incomeCursor.getColumnIndexOrThrow("description"));

            String date = incomeCursor.getString(
                    incomeCursor.getColumnIndexOrThrow("date"));

            Map<String, Object> map = new HashMap<>();

            map.put("userId", userUid);
            map.put("title", title);
            map.put("amount", amount);
            map.put("category", category);
            map.put("description", description);
            map.put("date", date);
            map.put("type", "income");
            map.put("timestamp", System.currentTimeMillis());

            firestore.collection("users")
                    .document(userUid)
                    .collection("transactions")
                    .add(map)
                    .addOnSuccessListener(documentReference -> {

                        db.markIncomeAsSynced(id,
                                documentReference.getId());

                    });
        }

        incomeCursor.close();
    }

    // INTERNET CHECK
    private static boolean isInternetAvailable(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnected();
    }
}