package com.lock.stockit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Models.ReceiptModel;

import java.util.ArrayList;
import java.util.Locale;

public class PrintPreviewActivity extends AppCompatActivity {
    private final CollectionReference colRef = FirebaseFirestore.getInstance().collection("format");
    private TextView printHeader, printBody, printFooter;
    private String header, body, footer;
    private ArrayList<String> headerArray;
    private ArrayList<ReceiptModel> receiptList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_print_preview);

        printHeader = findViewById(R.id.print_header);
        printBody = findViewById(R.id.print_body);
        printFooter = findViewById(R.id.print_footer);
        Button cancelButton = findViewById(R.id.cancel_button);
        Button printButton = findViewById(R.id.print_button);
        receiptList = getIntent().getExtras().getParcelableArrayList("receiptList");
        headerArray = getIntent().getExtras().getStringArrayList("header");

        getHeaderBodyFooter();
        setHeaderBodyFooter();

        cancelButton.setOnClickListener(v -> {
            Intent i = new Intent(this, ReceiptFragment.class);
            i.putExtra("receiptList", receiptList);
            setResult(RESULT_CANCELED,i);
            finish();
        });

        printButton.setOnClickListener(v -> {
            Intent i = new Intent(this, ReceiptFragment.class);
            setResult(RESULT_OK);
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setHeaderBodyFooter() {
        printHeader.setText(header);
        printBody.setText(body);
        printFooter.setText(footer);
    }

    private void getHeaderBodyFooter() {
        header = getHeader();
        body = getBody();
        footer = "footer";
    }

    private String getHeader() {
        StringBuilder text = new StringBuilder();
        for (String s : headerArray) text.append(s.toUpperCase()).append("\n");
        return text.toString();
    }

    private String getBody() {
        StringBuilder text = new StringBuilder();
        for (ReceiptModel receipt : receiptList) {
            String row1 = String.format(Locale.US, "%-22s %10.2f",
                    receipt.getItemName().toUpperCase(),
                    receipt.getItemTotalPrice());

            String row2 = String.format(Locale.US, "     %-6s     %4d @ %8.2f",
                    receipt.getItemSize(),
                    receipt.getItemQuantity(),
                    receipt.getItemUnitPrice());
            text.append(row1).append("\n")
                    .append(row2).append("\n");
        }
        return text.toString();
    }
}