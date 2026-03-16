package com.example.hisabkitab;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.*;

public class AnalyticsActivity extends AppCompatActivity {

    PieChart pieChart;
    BarChart barChart;

    Button btnIncome, btnExpense;

    TextView txtPrediction, txtInsights;

    DatabaseHandler db;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userUid;

    boolean showIncome = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analytics);
        // Initialize Firebase auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish(); // exit if user not logged in
            return;
        }

        userUid = currentUser.getUid(); // correct UID

        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);

        btnIncome = findViewById(R.id.btnIncome);
        btnExpense = findViewById(R.id.btnExpense);

        txtPrediction = findViewById(R.id.txtPrediction);
        txtInsights = findViewById(R.id.txtInsights);

        db = new DatabaseHandler(this);

        btnIncome.setOnClickListener(v -> {

            showIncome = true;
            loadPieChart();

            // UI change
            btnIncome.setBackgroundResource(R.drawable.toggle_selected);
            btnIncome.setTextColor(getResources().getColor(android.R.color.white));

            btnExpense.setBackgroundResource(R.drawable.toggle_unselected);
            btnExpense.setTextColor(getResources().getColor(android.R.color.black));
        });
        btnExpense.setOnClickListener(v -> {

            showIncome = false;
            loadPieChart();
            // UI change
            btnExpense.setBackgroundResource(R.drawable.toggle_selected);
            btnExpense.setTextColor(getResources().getColor(android.R.color.white));

            btnIncome.setBackgroundResource(R.drawable.toggle_unselected);
            btnIncome.setTextColor(getResources().getColor(android.R.color.black));
        });
        loadPieChart();
        loadBarChart();
        calculatePrediction();
        generateInsights();
    }

    void loadPieChart(){

        Map<String, Float> categoryMap = new HashMap<>();

        Cursor cursor;

        if(showIncome)
            cursor = db.getIncome(userUid);
        else
            cursor = db.getExpenses(userUid);

        while(cursor.moveToNext()){

            String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));

            if(categoryMap.containsKey(category))
                categoryMap.put(category, categoryMap.get(category) + amount);
            else
                categoryMap.put(category, amount);
        }

        List<PieEntry> entries = new ArrayList<>();

        for(String key : categoryMap.keySet())
            entries.add(new PieEntry(categoryMap.get(key), key));

        PieDataSet dataSet = new PieDataSet(entries, "");

        dataSet.setColors(
                0xFF2ECC71,
                0xFF3498DB,
                0xFFF1C40F,
                0xFFE67E22,
                0xFFE74C3C
        );

        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.invalidate();
    }

    void loadBarChart(){

        Map<String, Float> monthMap = new HashMap<>();

        Cursor cursor = db.getExpenses(userUid);

        while(cursor.moveToNext()){

            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
            float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));

            String month = date.substring(0,7);

            if(monthMap.containsKey(month))
                monthMap.put(month, monthMap.get(month)+amount);
            else
                monthMap.put(month, amount);
        }

        List<BarEntry> entries = new ArrayList<>();

        int index = 0;

        for(String key : monthMap.keySet()){
            entries.add(new BarEntry(index, monthMap.get(key)));
            index++;
        }

        BarDataSet set = new BarDataSet(entries,"Monthly Expense");

        set.setColor(0xFF2ECC71);

        BarData data = new BarData(set);

        barChart.setData(data);
        barChart.invalidate();
    }

    void calculatePrediction(){

        Cursor cursor = db.getExpenses(userUid);

        Map<String, Float> monthMap = new HashMap<>();

        while(cursor.moveToNext()){

            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
            float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));

            String month = date.substring(0,7);

            if(monthMap.containsKey(month))
                monthMap.put(month, monthMap.get(month)+amount);
            else
                monthMap.put(month, amount);
        }

        float total = 0;

        for(Float val : monthMap.values())
            total += val;

        float prediction = total / monthMap.size();

        txtPrediction.setText("Rs "+prediction);
    }

    void generateInsights(){

        Cursor cursor = db.getExpenses(userUid);

        Map<String, Float> categoryMap = new HashMap<>();

        while(cursor.moveToNext()){

            String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));

            if(categoryMap.containsKey(category))
                categoryMap.put(category, categoryMap.get(category)+amount);
            else
                categoryMap.put(category, amount);
        }

        String topCategory = "";
        float max = 0;

        for(String key : categoryMap.keySet()){

            if(categoryMap.get(key) > max){
                max = categoryMap.get(key);
                topCategory = key;
            }
        }

        txtInsights.setText(
                "Highest spending category: "+topCategory+
                        "\nTotal spent: Rs "+max
        );
    }
}