package com.lock.stockit;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
//    private final String type = LoaderActivity.admin ? "an Admin" : "a User";
    TableRow tableRow;

    TableLayout receiptTable;
    @SuppressLint("SetTextI18n") //remove later
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        receiptTable = view.findViewById(R.id.receipt_history);
        initializeReceiptHistory();

        return view;
    }

    private void initializeReceiptHistory() {
        FirebaseFirestore.getInstance().collection("receipts").orderBy("invoice no").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            getReceiptTableRow("Invoice #", "Date & Time", "Total", "Amount Rendered\nCash", "Items");

            for (var document : task.getResult()) {
                StringBuilder items = new StringBuilder();
                for (var item : (ArrayList<?>) document.get("items"))
                    items.append(item.toString()).append("\n");
                getReceiptTableRow(document.getString("invoice no"),
                        document.getString("date-time"),
                        "PHP " + document.getDouble("total"),
                        "PHP " + document.getDouble("amount rendered cash"),
                        items.toString());
            }
        });

    }

    private void getReceiptTableRow(String invoiceNo, String dateTime, String total, String amountRendered, String items) {
        TableRow tableRow = new TableRow(getActivity());
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        TextView textView1 = new TextView(getActivity());
        textView1.setText(invoiceNo);
        tableRow.addView(textView1);

        TextView textView2 = new TextView(getActivity());
        textView2.setText(dateTime);
        tableRow.addView(textView2);

        TextView textView3 = new TextView(getActivity());
        textView3.setText(total);
        tableRow.addView(textView3);

        TextView textView4 = new TextView(getActivity());
        textView4.setText(amountRendered);
        tableRow.addView(textView4);

        TextView textView5 = new TextView(getActivity());
        textView5.setText(items);
        tableRow.addView(textView5);

        receiptTable.addView(tableRow, new  TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
    }
}