package com.example.hisabkitab;

import android.os.Bundle;
import android.database.Cursor;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.components.XAxis;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.*;

public class StatementActivity extends AppCompatActivity {

    LineChart lineChart;

    RecyclerView recycler;

    Spinner spinnerDate, spinnerCategory;

    DatabaseHandler db;

    List<TransactionItem> transactions = new ArrayList<>();
    TransactionAdapter adapter;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userUid; // will hold actual Firebase UID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statement);

        // Initialize Firebase auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish(); // exit if user not logged in
            return;
        }

        userUid = currentUser.getUid(); // correct UID

        db = new DatabaseHandler(this);

        lineChart = findViewById(R.id.lineChart);
        recycler = findViewById(R.id.recyclerTransactions);

        spinnerDate = findViewById(R.id.spinnerDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        // Setup RecyclerView
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(transactions);
        recycler.setAdapter(adapter);

        setupSpinners();
        Button btnExportPDF = findViewById(R.id.btnExportPDF);
        btnExportPDF.setOnClickListener(v -> exportStatementAsPDF());
        loadGraph();
        loadRecentTransactions();
    }

    // Setup date and category filters
    void setupSpinners() {
        String[] dateFilters = {"Last 7 Days", "Last 30 Days", "All Time"};
        String[] categories = {"All", "Food", "Transport", "Shopping", "Salary", "Bills"};

        spinnerDate.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, dateFilters));

        spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories));
    }

    // Load Income & Expense chart
    void loadGraph() {
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();

        float index = 1;

        Cursor income = db.getIncome(userUid);
        if (income != null) {
            while (income.moveToNext()) {
                double amount = income.getDouble(income.getColumnIndexOrThrow("amount"));
                incomeEntries.add(new Entry(index, (float) amount));
                index++;
            }
            income.close();
        }

        index = 1;
        Cursor expense = db.getExpenses(userUid);
        if (expense != null) {
            while (expense.moveToNext()) {
                double amount = expense.getDouble(expense.getColumnIndexOrThrow("amount"));
                expenseEntries.add(new Entry(index, (float) amount));
                index++;
            }
            expense.close();
        }

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "Income");
        incomeSet.setColor(0xFF2ECC71);
        incomeSet.setCircleColor(0xFF2ECC71);

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "Expense");
        expenseSet.setColor(0xFFE74C3C);
        expenseSet.setCircleColor(0xFFE74C3C);

        LineData data = new LineData(incomeSet, expenseSet);

        lineChart.setData(data);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.invalidate();
    }

    //export as pdf
    private void exportStatementAsPDF() {
        if (transactions.isEmpty()) {
            Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();

        int pageWidth = 595; // A4 width in points
        int pageHeight = 842; // A4 height
        int y = 50; // starting vertical position

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        paint.setTextSize(20f);
        paint.setFakeBoldText(true);
        canvas.drawText("Statement Report", 200, y, paint);
        y += 40;

        paint.setTextSize(14f);
        paint.setFakeBoldText(false);

        canvas.drawText("Date: " + new SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(new Date()), 20, y, paint);
        y += 30;

        canvas.drawText("Title           Category        Type       Amount", 20, y, paint);
        y += 20;
        canvas.drawLine(20, y, pageWidth - 20, y, paint);
        y += 20;

        for (TransactionItem item : transactions) {
            if (y > pageHeight - 50) {
                // create new page if content exceeds
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            String type = item.isIncome ? "Income" : "Expense";
            String text = String.format(Locale.getDefault(), "%-15s %-12s %-8s Rs %.2f",
                    item.title, item.category, type, item.amount);

            canvas.drawText(text, 20, y, paint);
            y += 20;
        }

        pdfDocument.finishPage(page);

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/StatementReport.pdf";

        try {
            File file = new File(path);
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF saved to Downloads/StatementReport.pdf", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pdfDocument.close();
        }
    }

    // Load recent transactions into RecyclerView
    void loadRecentTransactions() {
        transactions.clear();

        // Load income
        Cursor income = db.getIncome(userUid);
        if (income != null) {
            while (income.moveToNext()) {
                String title = income.getString(income.getColumnIndexOrThrow("title"));
                String category = income.getString(income.getColumnIndexOrThrow("category"));
                double amount = income.getDouble(income.getColumnIndexOrThrow("amount"));
                String date = income.getString(income.getColumnIndexOrThrow("date"));
                int synced = income.getInt(income.getColumnIndexOrThrow("synced"));

                transactions.add(new TransactionItem(title, amount, date, true, synced, category));
            }
            income.close();
        }

        // Load expense
        Cursor expense = db.getExpenses(userUid);
        if (expense != null) {
            while (expense.moveToNext()) {
                String title = expense.getString(expense.getColumnIndexOrThrow("title"));
                String category = expense.getString(expense.getColumnIndexOrThrow("category"));
                double amount = expense.getDouble(expense.getColumnIndexOrThrow("amount"));
                String date = expense.getString(expense.getColumnIndexOrThrow("date"));
                int synced = expense.getInt(expense.getColumnIndexOrThrow("synced"));

                transactions.add(new TransactionItem(title, amount, date, false, synced, category));
            }
            expense.close();
        }

        // Sort by date descending
        Collections.sort(transactions, (a, b) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                Date d1 = sdf.parse(a.date);
                Date d2 = sdf.parse(b.date);
                return d2.compareTo(d1);
            } catch (Exception e) {
                return 0;
            }
        });

        adapter.notifyDataSetChanged(); // update RecyclerView
    }
}