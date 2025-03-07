package com.lock.stockit.Adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lock.stockit.Helpers.SwipeState;
import com.lock.stockit.Helpers.UserBaseViewHolder;
import com.lock.stockit.Helpers.UserListeners;
import com.lock.stockit.Helpers.UserViewHolder;
import com.lock.stockit.Models.UserModel;
import com.lock.stockit.R;

import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserBaseViewHolder> {

    private final UserListeners userListeners;
    private final SwipeState swipeState;
    private final ArrayList<UserModel> usersList;

    public UserAdapter(UserListeners userListeners, SwipeState swipeState) {
        super();
        this.userListeners = userListeners;
        this.swipeState = swipeState;
        usersList = new ArrayList<>();
    }

    @NonNull @Override
    public UserBaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view, userListeners);
    }

    @Override
    public void onBindViewHolder(@NonNull UserBaseViewHolder holder, int position) {
        holder.bindDataToViewHolder(usersList.get(position), position, swipeState);
    }

    @Override
    public int getItemCount() { return usersList.size(); }

    public void retainSwipe(UserModel model, int position) {
        // Check if swipe is enabled in the current state
        final boolean isEnabled = swipeState == SwipeState.LEFT || swipeState == SwipeState.RIGHT || swipeState == SwipeState.LEFT_RIGHT;
        // If swipe is enabled, reset the swipe state for other cells
        if (!isEnabled) return;
        for (int index = 0; index < getItemCount(); index++) {
            final boolean isNotSwiped = usersList.get(index).getState() != SwipeState.NONE;
            if (index != position && isNotSwiped) {
                usersList.get(index).setState(SwipeState.NONE);
                notifyItemChanged(index);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setUsers(ArrayList<UserModel> users) {
        usersList.clear();
        usersList.addAll(users);
        notifyDataSetChanged();
    }
}