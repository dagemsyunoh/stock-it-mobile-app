package com.lock.stockit;

import android.annotation.SuppressLint;
import android.graphics.Color;
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
import com.lock.stockit.Helper.UserSwipeHelper;
import com.lock.stockit.Model.UserData;
import com.lock.stockit.databinding.ActivityManageUsersBinding;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    ActivityManageUsersBinding binding;
    RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<UserData> usersList;
    ExtendedFloatingActionButton buttonBack;
    CollectionReference colRef = FirebaseFirestore.getInstance().collection("users");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityManageUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        recyclerView = findViewById(R.id.user_view);
        buttonBack = findViewById(R.id.back_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        new UserSwipeHelper(this, recyclerView, 200) {

            @Override
            public void instantiateUserButton(RecyclerView.ViewHolder viewHolder, List<UserSwipeHelper.UserButton> buffer) {
                buffer.add(new UserButton(ManageUsersActivity.this,
                        "",
                        R.drawable.ic_delete,
                        0,
                        Color.parseColor("#FF3C30"),
                        pos -> Toast.makeText(ManageUsersActivity.this, "Remove Click", Toast.LENGTH_SHORT).show()));
            }
        };

        usersList = new ArrayList<>();
        adapter = new UserAdapter(usersList, this);
        recyclerView.setAdapter(adapter);

        buttonBack.setOnClickListener(v -> finish());


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
                usersList.add(new UserData(email, admin, activated));
            }
            adapter.notifyDataSetChanged();
        });
    }

}