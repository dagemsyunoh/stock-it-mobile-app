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

import com.lock.stockit.Models.ReceiptModel;

import java.util.ArrayList;

public class PrintPreviewActivity extends AppCompatActivity {
    private TextView printHeader, printBody, printFooter;
    private String header, body, footer;
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
        header = "Header";
        body = getBody();
        footer = "Footer";
    }

    private String getBody() {
        StringBuilder builder = new StringBuilder();
        for (ReceiptModel receipt : receiptList) {
            builder.append(receipt.getItemName()).append(" ")
                    .append(receipt.getItemSize()).append(" ")
                    .append(receipt.getItemQuantity()).append(" ")
                    .append(receipt.getItemUnitPrice()).append(" ")
                    .append(receipt.getItemTotalPrice()).append(" \n");
        }
        return builder.toString();
    }
}