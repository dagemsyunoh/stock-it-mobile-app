package com.lock.stockit.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lock.stockit.Model.UserData;
import com.lock.stockit.R;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final Context context;
    List<UserData> usersList;

    public UserAdapter(List<UserData> usersList, Context context) {
        this.usersList = usersList;
        this.context = context;
    }

    @NonNull
    @Override
    public UserAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.UserViewHolder holder, int position) {
        UserData userData = usersList.get(position);
        holder.email.setText(userData.getEmail());
        holder.adminSwitch.setChecked(userData.isAdmin());
        holder.activatedSwitch.setChecked(userData.isActivated());
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView email;
        SwitchCompat adminSwitch;
        SwitchCompat activatedSwitch;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            email = itemView.findViewById(R.id.email);
            adminSwitch = itemView.findViewById(R.id.admin_switch);
            activatedSwitch = itemView.findViewById(R.id.activated_switch);
        }
    }
}