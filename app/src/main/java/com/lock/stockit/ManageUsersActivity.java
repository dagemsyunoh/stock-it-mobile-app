package com.lock.stockit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Adapter.UserAdapter;
import com.lock.stockit.Helper.CustomLinearLayoutManager;
import com.lock.stockit.Helper.SwipeState;
import com.lock.stockit.Helper.UserListeners;
import com.lock.stockit.Model.UserModel;

import java.util.ArrayList;

public class ManageUsersActivity extends AppCompatActivity implements UserListeners {

//    ActivityManageUsersBinding binding;
    RecyclerView recyclerView;
    private ArrayList<UserModel> usersList;
    private UserAdapter adapter;

    public static Intent newIntent(Context context) {
        return new Intent(context, ManageUsersActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
    ExtendedFloatingActionButton buttonBack;
    CollectionReference colRef = FirebaseFirestore.getInstance().collection("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_users);
//        binding = ActivityManageUsersBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());

        buttonBack = findViewById(R.id.back_button);
        recyclerView = findViewById(R.id.user_view);
        usersList = new ArrayList<>();
        setRecyclerView();
        fetchData();
//        setItems();

        buttonBack.setOnClickListener(v -> finish());


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setRecyclerView() {
        adapter = new UserAdapter(this, SwipeState.LEFT_RIGHT);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.setUsers(usersList);
        fetchData();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchData() {
        colRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) {
                return;
            }
            usersList.clear();

            for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                String email = documentSnapshot.getString("email");
                boolean admin = Boolean.TRUE.equals(documentSnapshot.getBoolean("admin"));
                boolean activated = Boolean.TRUE.equals(documentSnapshot.getBoolean("activated"));
                usersList.add(new UserModel(email, admin, activated));
            }
            adapter.notifyDataSetChanged();
        });
    }
//    private void setItems() {
//        usersList = new ArrayList<UserModel>();
//        usersList.clear();
//        usersList.add(new UserModel("A", true, true));
//        usersList.add(new UserModel("B", true, true));
//        usersList.add(new UserModel("C", true, true));
//        usersList.add(new UserModel("D", true, true));
//        usersList.add(new UserModel("E", true, true));
//        usersList.add(new UserModel("F", true, true));
//        usersList.add(new UserModel("G", true, true));
//        usersList.add(new UserModel("H", true, true));
//        usersList.add(new UserModel("I", true, true));
//        usersList.add(new UserModel("J", true, true));
//        usersList.add(new UserModel("K", true, true));
//        usersList.add(new UserModel("L", true, true));
//        usersList.add(new UserModel("M", true, true));
//        usersList.add(new UserModel("N", true, true));
//        usersList.add(new UserModel("O", true, true));
//        usersList.add(new UserModel("P", true, true));
//        usersList.add(new UserModel("Q", true, true));
//        usersList.add(new UserModel("R", true, true));
//        usersList.add(new UserModel("S", true, true));
//        usersList.add(new UserModel("T", true, true));
//        usersList.add(new UserModel("U", true, true));
//        usersList.add(new UserModel("V", true, true));
//        usersList.add(new UserModel("W", true, true));
//        usersList.add(new UserModel("X", true, true));
//        usersList.add(new UserModel("Y", true, true));
//        usersList.add(new UserModel("Z", true, true));
//    }

    @Override
    public void onClickLeft(UserModel item, int position) {

    }

    @Override
    public void onClickRight(UserModel item, int position) {

    }
}