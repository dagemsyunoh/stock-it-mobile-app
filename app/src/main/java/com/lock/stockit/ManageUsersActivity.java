package com.lock.stockit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

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

        buttonBack = findViewById(R.id.back_button);
        recyclerView = findViewById(R.id.user_view);
        usersList = new ArrayList<>();
        fetchData();
//        setItems();

        buttonBack.setOnClickListener(v -> fetchData());


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setRecyclerView() {
        adapter = new UserAdapter(this, SwipeState.LEFT);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
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
            setRecyclerView();
            adapter.setUsers(usersList);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onClickLeft(UserModel item, int position) {

    }

    @Override
    public void onClickRight(UserModel item, int position) {
        Toast.makeText(this, "Delete", Toast.LENGTH_SHORT).show();
    }
}