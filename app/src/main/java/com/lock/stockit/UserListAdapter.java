package com.lock.stockit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;

public class UserListAdapter extends ArrayAdapter<UserData> {
    public UserListAdapter(@NonNull Context context, ArrayList<UserData> dataArrayList) {
        super(context, R.layout.user_item, dataArrayList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        UserData userData = getItem(position);

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.user_item, parent, false);
        }

        TextView listEmail = view.findViewById(R.id.email);
        SwitchCompat listAdmin = view.findViewById(R.id.admin_switch);
        SwitchCompat listActivated = view.findViewById(R.id.activated_switch);

        assert userData != null;
        String email = "Email: " + userData.email;
        listEmail.setText(email);
        listAdmin.setChecked(userData.admin);
        listActivated.setChecked(userData.activated);

        return view;
    }
}
