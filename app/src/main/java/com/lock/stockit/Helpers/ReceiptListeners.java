package com.lock.stockit.Helpers;

import com.lock.stockit.Models.ReceiptModel;

public interface ReceiptListeners {

    void changeQty(ReceiptModel item, int position);
    void onClickRight(ReceiptModel item, int position);

    void onRetainSwipe(ReceiptModel item, int position);
}
