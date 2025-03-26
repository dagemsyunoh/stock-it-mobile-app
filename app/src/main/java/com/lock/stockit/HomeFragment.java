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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private final boolean type = LoaderActivity.admin;
    private final DecimalFormat df = new DecimalFormat("0.00");
    TableLayout receiptTable, userTable;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        receiptTable = view.findViewById(R.id.receipt_log);
        initializeReceiptLog();

        if (type) {
            LinearLayout userLogLayout = view.findViewById(R.id.user_log_layout);
            userLogLayout.setVisibility(View.VISIBLE);
            userTable = view.findViewById(R.id.user_log);
            initializeUserLog();
        }

        return view;
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

        if (typeface == Typeface.NORMAL) tableRow.setOnClickListener(v -> {
//            TODO: dialog
        });
        userTable.addView(tableRow, new  TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
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

            for (var document : task.getResult()) {
                StringBuilder items = new StringBuilder();
                for (var item : (ArrayList<?>) document.get("items"))
                    items.append(item.toString()).append("\n");
                getReceiptTableRow(document.getString("invoice no"),
                        document.getString("date-time"),
                        items.toString(),
                        "PHP " + df.format(document.getDouble("amount rendered cash")),
                        "PHP " + df.format(document.getDouble("total")),
                        "PHP " + df.format(document.getDouble("amount rendered cash") - document.getDouble("total")),
                        Gravity.START, Typeface.NORMAL);
            }
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
        dialog.setContentView(R.layout.rh_box);
        dialog.show();
        AppCompatImageView back = dialog.findViewById(R.id.back);
        back.setOnClickListener(v1 -> dialog.dismiss());
        TextView header = dialog.findViewById(R.id.header);
        TextView dateTimeText = dialog.findViewById(R.id.date_time_val_text);
        TextView amountRenderedText = dialog.findViewById(R.id.amount_rendered_val_text);
        TextView totalText = dialog.findViewById(R.id.total_val_text);
        TextView changeText = dialog.findViewById(R.id.change_val_text);
        TextView itemsText = dialog.findViewById(R.id.items_val_text);
        header.setText(invoiceNo);
        dateTimeText.setText(dateTime);
        amountRenderedText.setText(amountRendered);
        totalText.setText(total);
        changeText.setText(change);
        itemsText.setText(items);
    }

    private void addTextViewToRow(TableRow tableRow, String text,int gravity, int typeface) {
        TextView textView = new TextView(getActivity());
        textView.setText(text);
        textView.setPadding(20, 0, 20, 0);
        textView.setGravity(gravity);
        textView.setTypeface(null, typeface);

        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(params);
        tableRow.addView(textView);
    }
}