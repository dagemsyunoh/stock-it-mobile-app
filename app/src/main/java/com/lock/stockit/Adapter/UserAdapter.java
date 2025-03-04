package com.lock.stockit.Adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Helper.BaseViewHolder;
import com.lock.stockit.Helper.SwipeState;
import com.lock.stockit.Helper.UserListeners;
import com.lock.stockit.Helper.UserViewHolder;
import com.lock.stockit.Model.UserModel;
import com.lock.stockit.R;

import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<BaseViewHolder> {

    private final UserListeners userListeners;
    private final SwipeState swipeState;
    private final ArrayList<UserModel> usersList;
    CollectionReference colRef = FirebaseFirestore.getInstance().collection("users");

    public UserAdapter(UserListeners userListeners, SwipeState swipeState) {
        super();
        this.userListeners = userListeners;
        this.swipeState = swipeState;
        usersList = new ArrayList<>();
    }

    @NonNull @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view, userListeners);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
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

    @SuppressLint("NotifyDataSetChanged")
    private void fetchData() {
        colRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            this.usersList.clear();

            for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                String email = documentSnapshot.getString("email");
                boolean admin = Boolean.TRUE.equals(documentSnapshot.getBoolean("admin"));
                boolean activated = Boolean.TRUE.equals(documentSnapshot.getBoolean("activated"));
                usersList.add(new UserModel(email, admin, activated));
            }
            notifyDataSetChanged();
        });
    }
}