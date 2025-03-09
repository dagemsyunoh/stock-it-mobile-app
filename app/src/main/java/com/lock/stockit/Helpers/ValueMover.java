package com.lock.stockit.Helpers;

import com.google.android.material.textfield.TextInputEditText;

public class ValueMover {
    public static void onPlusOne(TextInputEditText inputQty) {
        int qty = Integer.parseInt(inputQty.getText().toString());
        inputQty.setText(String.valueOf(qty + 1));
    }
    public static void onMinusOne(TextInputEditText inputQty) {
        int qty = Integer.parseInt(inputQty.getText().toString());
        if (qty > 0) inputQty.setText(String.valueOf(qty - 1));
    }
}
