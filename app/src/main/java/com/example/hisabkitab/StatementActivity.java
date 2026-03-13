package com.example.hisabkitab;

import android.os.Bundle;
import android.database.Cursor;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.components.XAxis;

import java.util.*;

public class StatementActivity extends AppCompatActivity {

    LineChart lineChart;
    RecyclerView recycler;

    Spinner spinnerDate, spinnerCategory;

    DatabaseHandler db;

    List<TransactionItem> transactions = new ArrayList<>();
    TransactionAdapter adapter;

    String userUid = "demo_user"; // replace with firebase uid

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statement);

        db = new DatabaseHandler(this);

        lineChart = findViewById(R.id.lineChart);
        recycler = findViewById(R.id.recyclerTransactions);

        spinnerDate = findViewById(R.id.spinnerDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(transactions);
        recycler.setAdapter(adapter);

        setupSpinners();

        loadGraph();
        loadRecentTransactions();
    }

    void setupSpinners(){

        String[] dateFilters = {"Last 7 Days","Last 30 Days","All Time"};
        String[] categories = {"All","Food","Transport","Shopping","Salary","Bills"};

        spinnerDate.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,dateFilters));

        spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,categories));

    }

    void loadGraph(){

        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();

        float monthIndex = 1;

        Cursor income = db.getIncome(userUid);

        while(income.moveToNext()){

            double amount = income.getDouble(income.getColumnIndexOrThrow("amount"));

            incomeEntries.add(new Entry(monthIndex,(float)amount));
            monthIndex++;
        }

        Cursor expense = db.getExpenses(userUid);

        monthIndex = 1;

        while(expense.moveToNext()){

            double amount = expense.getDouble(expense.getColumnIndexOrThrow("amount"));

            expenseEntries.add(new Entry(monthIndex,(float)amount));
            monthIndex++;
        }

        LineDataSet incomeSet = new LineDataSet(incomeEntries,"Income");
        incomeSet.setColor(0xFF2ECC71);
        incomeSet.setCircleColor(0xFF2ECC71);

        LineDataSet expenseSet = new LineDataSet(expenseEntries,"Expense");
        expenseSet.setColor(0xFFE74C3C);
        expenseSet.setCircleColor(0xFFE74C3C);

        LineData data = new LineData(incomeSet,expenseSet);

        lineChart.setData(data);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.invalidate();
    }

    void loadRecentTransactions(){

        transactions.clear();

        Cursor income = db.getIncome(userUid);

        while(income.moveToNext()){

            String title = income.getString(income.getColumnIndexOrThrow("title"));
            String category = income.getString(income.getColumnIndexOrThrow("category"));
            double amount = income.getDouble(income.getColumnIndexOrThrow("amount"));
            String date = income.getString(income.getColumnIndexOrThrow("date"));

            transactions.add(new TransactionItem(title, amount, date, true, 0, category));
        }

        Cursor expense = db.getExpenses(userUid);

        while(expense.moveToNext()){

            String title = expense.getString(expense.getColumnIndexOrThrow("title"));
            String category = expense.getString(expense.getColumnIndexOrThrow("category"));
            double amount = expense.getDouble(expense.getColumnIndexOrThrow("amount"));
            String date = expense.getString(expense.getColumnIndexOrThrow("date"));

            transactions.add(new TransactionItem(title, amount, date, false, 0, category));
        }

        adapter.notifyDataSetChanged();
    }
}