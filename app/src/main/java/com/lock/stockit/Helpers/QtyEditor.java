package com.lock.stockit.Helpers;

import com.google.android.material.textfield.TextInputEditText;

public class QtyEditor {
    public static void changeQty(TextInputEditText inputQty, int val) {
        if (inputQty == null) inputQty.setText(String.valueOf(0));
        int qty = Integer.parseInt(inputQty.getText().toString());
        if (Math.abs(val) != 1) {
            inputQty.setText(String.valueOf(val));
            return;
        }
        if (qty + val > 0) inputQty.setText(String.valueOf(qty + val));
    }
}
