package com.lock.stockit;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Adapters.UserAdapter;
import com.lock.stockit.Helpers.CustomLinearLayoutManager;
import com.lock.stockit.Helpers.Logger;
import com.lock.stockit.Helpers.SwipeState;
import com.lock.stockit.Helpers.UserListeners;
import com.lock.stockit.Models.UserModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ManageUsersActivity extends AppCompatActivity implements UserListeners {

    private final CollectionReference userRef = FirebaseFirestore.getInstance().collection("users");
    private final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private RecyclerView recyclerView;
    private ArrayList<UserModel> usersList;
    private UserAdapter adapter;
    protected ExtendedFloatingActionButton buttonBack;
    protected AuthCredential credential;
    private AlertDialog dialog;
    private final Logger logger = new Logger();
    public static String sid = LoaderActivity.sid;

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

    private void setRecyclerView() {
        adapter = new UserAdapter(this, SwipeState.LEFT);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchData() {
        userRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            usersList.clear();

            for (DocumentSnapshot doc : value.getDocuments())
                if (doc.getString("store").equals(sid) || doc.getString("store").isEmpty())
                    usersList.add(new UserModel(doc.getString("name"),
                            doc.getString("email"),
                            Boolean.TRUE.equals(doc.getBoolean("admin")),
                            Boolean.TRUE.equals(doc.getBoolean("activated")),
                            doc.getString("store")));
            setRecyclerView();
            adapter.setUsers(usersList);
            adapter.notifyDataSetChanged();
        });
    }

    private void reAuth(EditText passwordInput, int pos) {
        String email = user.getEmail();
        String password = String.valueOf(passwordInput.getText());
        if (email == null) {
            Toast.makeText(ManageUsersActivity.this, "Internal Error. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(ManageUsersActivity.this, "Please enter your password.", Toast.LENGTH_SHORT).show();
            return;
        }
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

        String message;
        if (again) message = "User Data will be permanently deleted. Are you sure?\n\nPassword Incorrect.\nPlease try again.";
        else message = "User Data will be permanently deleted. Are you sure?\n\nPlease enter your password to confirm.";

        Dialog passDialog = new Dialog(ManageUsersActivity.this);
        passDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        passDialog.setContentView(R.layout.dialog_box_input);
        passDialog.setCancelable(false);
        passDialog.create();
        passDialog.show();

        TextView header = passDialog.findViewById(R.id.header);
        header.setText(R.string.warning_user_deletion);
        header.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0);

        TextView dialogText = passDialog.findViewById(R.id.dialog_text);
        dialogText.setText(message);

        TextInputLayout inputLayout = passDialog.findViewById(R.id.input_layout);
        inputLayout.setHint(getString(R.string.password));
        inputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

        TextInputEditText inputPassword = passDialog.findViewById(R.id.input);
        inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        Button buttonCancel = passDialog.findViewById(R.id.cancel_button);
        Button buttonDelete = passDialog.findViewById(R.id.ok_button);
        buttonDelete.setText(R.string.delete_user);

        buttonCancel.setOnClickListener(v -> passDialog.dismiss());
        buttonDelete.setOnClickListener(v -> {
            reAuth(inputPassword, position);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setView(R.layout.loading_dialog);
            dialog = builder.create();
            dialog.show();
            TextView loadingText = dialog.findViewById(R.id.loading_dialog_text);
            String show = "Deleting User " + usersList.get(position).getEmail();
            loadingText.setText(show);
            passDialog.dismiss();
        });
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            dialog.dismiss();
            Toast.makeText(ManageUsersActivity.this, "User deleted.", Toast.LENGTH_SHORT).show();
        }
    };

    public void run() {
        try {
            mHandler.sendEmptyMessage(0);
        } catch (Exception e) {
            Log.e("TAG", e.getMessage());
        }
    }

    private void deleteUser(int pos) {
        String email = usersList.get(pos).getEmail();
        userRef.whereEqualTo("email", email).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String uid = task.getResult().getDocuments().get(0).getId();
            userRef.document(uid).delete();
            requestDelete(email);
            logger.setUserLog("delete", email, user.getEmail());
            dialog.dismiss();
        });
    }
    private void requestDelete(String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("pending delete", true);
        FirebaseFirestore.getInstance().collection("user delete request").document(email).set(data);
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