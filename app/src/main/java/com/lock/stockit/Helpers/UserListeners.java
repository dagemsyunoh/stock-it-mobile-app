package com.lock.stockit.Helpers;

import com.lock.stockit.Models.UserModel;

public interface UserListeners {
    void onClickRight(UserModel item, int position);

    void onRetainSwipe(UserModel item, int position);
}
