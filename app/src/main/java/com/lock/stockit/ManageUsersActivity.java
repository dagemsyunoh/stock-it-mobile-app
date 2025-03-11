package com.lock.stockit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Adapters.UserAdapter;
import com.lock.stockit.Helpers.CustomLinearLayoutManager;
import com.lock.stockit.Helpers.SwipeState;
import com.lock.stockit.Helpers.UserListeners;
import com.lock.stockit.Models.UserModel;

import java.util.ArrayList;

public class ManageUsersActivity extends AppCompatActivity implements UserListeners {

    private RecyclerView recyclerView;
    private ArrayList<UserModel> usersList;
    private UserAdapter adapter;

    public static Intent newIntent(Context context) {
        return new Intent(context, ManageUsersActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
    ExtendedFloatingActionButton buttonBack;
    CollectionReference colRef = FirebaseFirestore.getInstance().collection("users");
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    AuthCredential credential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_users);

        buttonBack = findViewById(R.id.back_button);
        recyclerView = findViewById(R.id.user_view);
        usersList = new ArrayList<>();

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
            if (error != null || value == null) return;
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

    private void setRecyclerView() {
        adapter = new UserAdapter(this, SwipeState.LEFT);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    private void reAuth(EditText passwordInput, int pos) {
        String email = user.getEmail();
        String password = String.valueOf(passwordInput.getText());
        assert email != null;
        credential = EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ManageUsersActivity.this, "Re-authentication successful.", Toast.LENGTH_SHORT).show();
                        deleteUser(pos);
                    } else {
                        Toast.makeText(ManageUsersActivity.this, "Re-authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                        alertPassword(pos, true);
                    }
                });
    }

    private void alertPassword(int position, boolean again) {
        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("Password");
        passwordInput.setSingleLine();
        passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

        String message;
        if (again) message = "User Data will be permanently deleted. Are you sure?\n\nPassword Incorrect.\nPlease try again.";
        else message = "User Data will be permanently deleted. Are you sure?\n\nPlease enter your password to confirm.";

        AlertDialog alertDialog = new AlertDialog.Builder(ManageUsersActivity.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle("Warning! User Deletion")
                .setMessage(message)
                .setPositiveButton("Delete User", (dialog, which) -> {
                    reAuth(passwordInput, position);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .create();
        alertDialog.setView(passwordInput);
        alertDialog.show();
    }

    private void deleteUser(int pos) {
        String email = usersList.get(pos).getEmail();
        colRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            for (DocumentSnapshot documentSnapshot : value.getDocuments())
                if (documentSnapshot.getString("email").equals(email)) {
                    documentSnapshot.getReference().delete();
                    Toast.makeText(ManageUsersActivity.this, "User Deleted", Toast.LENGTH_SHORT).show();
                }
        });
    }

    @Override
    public void onClickRight(UserModel item, int position) {
        alertPassword(position, false);
    }


    @Override
    public void onRetainSwipe(UserModel item, int position) {
        adapter.retainSwipe(item, position);
    }
}