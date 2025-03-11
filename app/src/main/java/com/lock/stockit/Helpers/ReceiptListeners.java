package com.lock.stockit.Helpers;

import com.lock.stockit.Models.ReceiptModel;

public interface ReceiptListeners {

    void onClickRight(ReceiptModel item, int position);

    void onRetainSwipe(ReceiptModel item, int position);
}
