package com.lock.stockit;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private final boolean type = LoaderActivity.admin;
    private final DecimalFormat df = new DecimalFormat("0.00");
    private final List<String> dateList = new ArrayList<>();
    private final List<Double> salesList = new ArrayList<>();
    private TableLayout receiptTable, userTable;
    private LineChart lineChart;
    private double max = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        lineChart = view.findViewById(R.id.chart);

        receiptTable = view.findViewById(R.id.receipt_log);
        initializeReceiptLog();

        LinearLayout userLogLayout = view.findViewById(R.id.user_log_layout);
        userTable = view.findViewById(R.id.user_log);
        initializeUserLog();
        if (type) userLogLayout.setVisibility(View.VISIBLE);

        return view;
    }

    private void setChart() {
        Description description = new Description();
        description.setText("");

        int digits = (int) Math.log10(max) + 1;
        float maxLimit = (float) Math.pow(10, digits);
        if (maxLimit / max > 2) maxLimit /= 2;

        lineChart.setDescription(description);
        lineChart.getAxisRight().setDrawLabels(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dateList));
        xAxis.setLabelCount(dateList.size());
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-90);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(maxLimit);
        yAxis.setAxisLineWidth(2f);
        yAxis.setLabelCount(5);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < salesList.size(); i++)
            entries.add(new Entry(i, salesList.get(i).floatValue()));

        LineDataSet lineDataSet = new LineDataSet(entries, "Total Sales");
        lineDataSet.setColor(Color.GREEN);

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
        lineChart.setVisibleXRangeMinimum(3);
        lineChart.setVisibleXRangeMaximum(30);
        lineChart.invalidate();
    }

    private void initializeUserLog() {
        FirebaseFirestore.getInstance().collection("user log").orderBy("date-time", Query.Direction.DESCENDING).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            getUserTableRow("Date & Time",
                    "Action",
                    "Target",
                    "User",
                    Typeface.BOLD);

            for (var document : task.getResult()) {
                getUserTableRow(document.getString("date-time"),
                        document.getString("action"),
                        document.getString("target"),
                        document.getString("user"),
                        Typeface.NORMAL);
            }
        });
    }

    private void getUserTableRow(String dateTime, String action, String target, String user,int typeface) {
        TableRow tableRow = new TableRow(getActivity());
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));

        addTextViewToRow(tableRow, dateTime, Gravity.CENTER, typeface);
        addTextViewToRow(tableRow, action, Gravity.CENTER, typeface);
        addTextViewToRow(tableRow, target, Gravity.CENTER, typeface);
        addTextViewToRow(tableRow, user, Gravity.CENTER, typeface);

        if (typeface == Typeface.NORMAL) tableRow.setOnClickListener(v -> showUserDialog(dateTime, action, target, user));
        userTable.addView(tableRow, new  TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
    }

    private void showUserDialog(String dateTime, String action, String target, String user) {
        Dialog dialog = new Dialog(getActivity());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.user_log_box);
        dialog.show();
        AppCompatImageView back = dialog.findViewById(R.id.back);
        back.setOnClickListener(v1 -> dialog.dismiss());

        TextView dateTimeText = dialog.findViewById(R.id.date_time_text);
        TextView actionText = dialog.findViewById(R.id.action_text);
        TextView targetText = dialog.findViewById(R.id.target_text);
        TextView userText = dialog.findViewById(R.id.user_text);

        String newDateTime = dateTimeText.getText() + ":\t\t" + dateTime;
        String newAction = actionText.getText() + ":\t\t" + action;
        String newTarget = targetText.getText() + ":\t\t" + target;
        String newUser = userText.getText() + ":\t\t" + user;

        dateTimeText.setText(newDateTime);
        actionText.setText(newAction);
        targetText.setText(newTarget);
        userText.setText(newUser);
    }

    private void initializeReceiptLog() {
        FirebaseFirestore.getInstance().collection("receipts").orderBy("invoice no", Query.Direction.DESCENDING).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            getReceiptTableRow("Invoice #",
                    "Date & Time",
                    "Items Purchased",
                    "Amount Rendered\nCash",
                    "Total Price",
                    "Change",
                    Gravity.CENTER,
                    Typeface.BOLD);

            String startDate = task.getResult().getDocuments().get(task.getResult().size()-1).getString("date-time").split(" ")[0];
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String endDate = LocalDate.now().toString();

            LocalDate localStartDate = LocalDate.parse(startDate);
            LocalDate localEndDate = LocalDate.parse(endDate);

            long days = ChronoUnit.DAYS.between(localStartDate, localEndDate);

            for (int i = 0; i <= days; i++) {
                dateList.add(localStartDate.plusDays(i).toString());
                salesList.add((double) 0);
            }

            for (var document : task.getResult()) {
                String date = document.getString("date-time").split(" ")[0];

                if (dateList.contains(date)) {
                    double dailyTotal = salesList.get(dateList.indexOf(date)) + document.getDouble("total");
                    salesList.set(dateList.indexOf(date), dailyTotal);
                    if (dailyTotal > max) max = dailyTotal;
                }

                StringBuilder items = new StringBuilder();
                for (var item : (ArrayList<?>) document.get("items"))
                    items.append(item.toString().replaceAll(", ", "\t@")).append("\n");

                getReceiptTableRow(document.getString("invoice no"),
                        document.getString("date-time"),
                        items.toString(),
                        "PHP " + df.format(document.getDouble("amount rendered cash")),
                        "PHP " + df.format(document.getDouble("total")),
                        "PHP " + df.format(document.getDouble("amount rendered cash") - document.getDouble("total")),
                        Gravity.START, Typeface.NORMAL);
            }

            setChart();
        });

    }

    private void getReceiptTableRow(String invoiceNo, String dateTime, String items, String amountRendered, String total, String change, int gravity, int typeface) {
        TableRow tableRow = new TableRow(getActivity());
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));

        addTextViewToRow(tableRow, invoiceNo, gravity, typeface);
        addTextViewToRow(tableRow, dateTime, Gravity.CENTER, typeface);
        addTextViewToRow(tableRow, items, gravity, typeface);
        addTextViewToRow(tableRow, amountRendered, Gravity.CENTER, typeface);
        addTextViewToRow(tableRow, total, Gravity.CENTER, typeface);
        addTextViewToRow(tableRow, change, Gravity.CENTER, typeface);

        if (typeface == Typeface.NORMAL) tableRow.setOnClickListener(v -> showReceiptDialog(invoiceNo, dateTime, items, amountRendered, total, change));
        receiptTable.addView(tableRow, new  TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
    }

    private void showReceiptDialog(String invoiceNo, String dateTime, String items, String amountRendered, String total, String change) {
        Dialog dialog = new Dialog(getActivity());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.receipt_log_box);
        dialog.show();
        AppCompatImageView back = dialog.findViewById(R.id.back);
        back.setOnClickListener(v1 -> dialog.dismiss());

        TextView header = dialog.findViewById(R.id.header);
        header.setText(invoiceNo);

        TextView dateTimeText = dialog.findViewById(R.id.date_time_text);
        TextView amountRenderedText = dialog.findViewById(R.id.amount_rendered_text);
        TextView totalText = dialog.findViewById(R.id.total_text);
        TextView changeText = dialog.findViewById(R.id.change_text);
        TextView itemsText = dialog.findViewById(R.id.items_text);

        String newDateTime = dateTimeText.getText() + ":";
        String newAmountRendered = amountRenderedText.getText() + ":";
        String newTotal = totalText.getText() + ":";
        String newChange = changeText.getText() + ":";
        String newItems = itemsText.getText() + ":";

        dateTimeText.setText(newDateTime);
        amountRenderedText.setText(newAmountRendered);
        totalText.setText(newTotal);
        changeText.setText(newChange);
        itemsText.setText(newItems);

        TextView dateTimeValText = dialog.findViewById(R.id.date_time_val_text);
        TextView amountRenderedValText = dialog.findViewById(R.id.amount_rendered_val_text);
        TextView totalValText = dialog.findViewById(R.id.total_val_text);
        TextView changeValText = dialog.findViewById(R.id.change_val_text);
        TextView itemsValText = dialog.findViewById(R.id.items_val_text);

        String newItemsText = items.replaceAll("\t@", "\n@");

        dateTimeValText.setText(dateTime);
        amountRenderedValText.setText(amountRendered);
        totalValText.setText(total);
        changeValText.setText(change);
        itemsValText.setText(newItemsText);
    }

    private void addTextViewToRow(TableRow tableRow, String text,int gravity, int typeface) {
        TextView textView = new TextView(getActivity());
        textView.setText(text);
        textView.setPadding(20, 10, 20, 10);
        textView.setGravity(gravity);
        textView.setTypeface(null, typeface);

        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(params);
        tableRow.addView(textView);
    }
}