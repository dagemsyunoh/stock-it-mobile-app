package com.lock.stockit.Helper;

import com.lock.stockit.Model.UserModel;

public interface UserListeners {

    void onClickRight(UserModel item, int position);

    void onRetainSwipe(UserModel item, int position);
}
