package com.lock.stockit;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.databinding.ActivityManageUsersBinding;

import java.util.ArrayList;

public class ManageUsersActivity extends AppCompatActivity {

    ActivityManageUsersBinding binding;
    UserListAdapter adapter;
    ArrayList<UserData> dataArrayList = new ArrayList<>();
    UserData userData;
    ExtendedFloatingActionButton buttonBack;
    CollectionReference colRef = FirebaseFirestore.getInstance().collection("users");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityManageUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        buttonBack = findViewById(R.id.back_button);
        dataArrayList.clear();

        colRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                    String email = queryDocumentSnapshots.getDocuments().get(i).getString("email");
                    boolean admin = Boolean.TRUE.equals(queryDocumentSnapshots.getDocuments().get(i).getBoolean("admin"));
                    boolean activated = Boolean.TRUE.equals(queryDocumentSnapshots.getDocuments().get(i).getBoolean("activated"));
                    userData = new UserData(email, admin, activated);
                    dataArrayList.add(userData);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        adapter = new UserListAdapter(this, dataArrayList);
        binding.users.setAdapter(adapter);

        buttonBack.setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}