package com.lock.stockit.Helpers;

import com.lock.stockit.Models.CustomerModel;

public interface CustomerListeners {
    /**
     * @noinspection unused
     */
    void onClickRight(CustomerModel item, int position);

    void onRetainSwipe(CustomerModel item, int position);
}
