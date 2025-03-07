package com.lock.stockit.Helpers;

import com.lock.stockit.Models.StockModel;

public interface StockListeners {

    void onClickLeft(StockModel item, int position);

    void onClickRight(StockModel item, int position);

    void onRetainSwipe(StockModel item, int position);
}
