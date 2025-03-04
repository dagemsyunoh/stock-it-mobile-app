package com.lock.stockit.Helper;

import com.lock.stockit.Model.UserModel;

public interface UserListeners {
    void onClickLeft(UserModel item, int position);

    void onClickRight(UserModel item, int position);
}
