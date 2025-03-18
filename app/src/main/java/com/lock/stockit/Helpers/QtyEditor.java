package com.lock.stockit.Helpers;

import com.google.android.material.textfield.TextInputEditText;

public class QtyEditor {
    public static void qtyEditor(TextInputEditText inputQty, int val) {
        int qty = Integer.parseInt(inputQty.getText().toString());
        if (val == 0) return;
        if (qty + val > 0) inputQty.setText(String.valueOf(qty + val));
    }
}
