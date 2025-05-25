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
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Adapters.CustomerAdapter;
import com.lock.stockit.Helpers.CustomLinearLayoutManager;
import com.lock.stockit.Helpers.CustomerListeners;
import com.lock.stockit.Helpers.Logger;
import com.lock.stockit.Helpers.SwipeState;
import com.lock.stockit.Models.CustomerModel;

import java.util.ArrayList;
import java.util.HashMap;

public class ManageCustomersActivity extends AppCompatActivity implements CustomerListeners {

    public static String sid = LoaderActivity.sid;
    private final CollectionReference customerRef = FirebaseFirestore.getInstance().collection("stores").document(sid).collection("customers");
    private final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private final Logger logger = new Logger();
    private final ArrayList<String> names = new ArrayList<>();
    protected FloatingActionButton buttonAdd;
    protected ExtendedFloatingActionButton buttonBack;
    protected Button addCustomer;
    protected AuthCredential credential;
    private RecyclerView recyclerView;
    private ArrayList<CustomerModel> customersList;
    private CustomerAdapter adapter;
    private AlertDialog dialog;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            dialog.dismiss();
            Toast.makeText(ManageCustomersActivity.this, "Customer deleted.", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_customers);
        buttonBack = findViewById(R.id.back_button);
        buttonAdd = findViewById(R.id.add_button);
        recyclerView = findViewById(R.id.customer_view);
        customersList = new ArrayList<>();

        buttonBack.setOnClickListener(v -> finish());

        buttonAdd.setOnClickListener(v -> addCustomerPopUp());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void addCustomerPopUp() {
        Dialog addPopUp = new Dialog(this);
        addPopUp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        addPopUp.setContentView(R.layout.customer_add);
        addPopUp.setTitle("Add Customer");
        addPopUp.show();
        TextInputEditText customerName = addPopUp.findViewById(R.id.add_name);

        AppCompatImageView back = addPopUp.findViewById(R.id.back);
        addCustomer = addPopUp.findViewById(R.id.add_item);

        customerName.setOnFocusChangeListener((view, b) -> {
            String input = String.valueOf(customerName.getText());
            if (input.isEmpty()) return;
            String[] words = input.split("\\s");
            StringBuilder output = new StringBuilder();
            for (String word : words)
                output.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            customerName.setText(output.toString().trim());
        });

        addCustomer.setOnClickListener(view -> {
            HashMap<String, Object> data = new HashMap<>();
            data.put("name", customerName.getText().toString());
            data.put("transactions", 0);

            if (data.get("name").toString().isEmpty()) {
                Toast.makeText(this, "Invalid input.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (exists(data)) {
                Toast.makeText(this, "Customer already exists.", Toast.LENGTH_SHORT).show();
                addPopUp.dismiss();
                return;
            }

            customerRef.add(data).addOnSuccessListener(documentReference -> {
                Toast.makeText(this, "Customer added.", Toast.LENGTH_SHORT).show();
                addPopUp.dismiss();
            }).addOnFailureListener(e -> Toast.makeText(this, "Error adding customer.", Toast.LENGTH_SHORT).show());
        });

        back.setOnClickListener(v -> addPopUp.cancel());
    }

    private boolean exists(HashMap<String, Object> data) {
        for (String n : names) if (n.equals(data.get("name").toString())) return true;
        return false;
    }


    @Override
    protected void onStart() {
        super.onStart();
        fetchData();
    }

    private void setRecyclerView() {
        adapter = new CustomerAdapter(this, SwipeState.LEFT);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchData() {
        customerRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            customersList.clear();

            for (DocumentSnapshot doc : value.getDocuments()) {
                names.add(doc.getString("name"));
                customersList.add(new CustomerModel(doc.getString("name"), doc.getDouble("transactions").intValue()));
            }
            setRecyclerView();
            adapter.setCustomers(customersList);
            adapter.notifyDataSetChanged();
        });
    }

    private void reAuth(EditText passwordInput, int pos) {
        String email = user.getEmail();
        String password = String.valueOf(passwordInput.getText());
        if (email == null) {
            Toast.makeText(ManageCustomersActivity.this, "Internal Error. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(ManageCustomersActivity.this, "Please enter your password.", Toast.LENGTH_SHORT).show();
            return;
        }
        credential = EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ManageCustomersActivity.this, "Re-authentication successful.", Toast.LENGTH_SHORT).show();
                        deleteCustomer(pos);
                    } else {
                        Toast.makeText(ManageCustomersActivity.this, "Re-authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                        alertPassword(pos, true);
                    }
                });
    }

    private void alertPassword(int position, boolean again) {

        String message;
        if (again)
            message = "Customer Data will be permanently deleted. Are you sure?\n\nPassword Incorrect.\nPlease try again.";
        else
            message = "Customer Data will be permanently deleted. Are you sure?\n\nPlease enter your password to confirm.";

        Dialog passDialog = new Dialog(ManageCustomersActivity.this);
        passDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        passDialog.setContentView(R.layout.dialog_box_input);
        passDialog.setCancelable(false);
        passDialog.create();
        passDialog.show();

        TextView header = passDialog.findViewById(R.id.header);
        header.setText(R.string.warning_customer_deletion);
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
        buttonDelete.setText(R.string.delete_customer);

        buttonCancel.setOnClickListener(v -> passDialog.dismiss());
        buttonDelete.setOnClickListener(v -> {
            reAuth(inputPassword, position);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setView(R.layout.loading_dialog);
            dialog = builder.create();
            dialog.show();
            TextView loadingText = dialog.findViewById(R.id.loading_dialog_text);
            String show = "Deleting Customer " + customersList.get(position).getName();
            loadingText.setText(show);
            passDialog.dismiss();
        });
    }

    public void run() {
        try {
            mHandler.sendEmptyMessage(0);
        } catch (Exception e) {
            Log.e("TAG", e.getMessage());
        }
    }

    private void deleteCustomer(int pos) {
        String name = customersList.get(pos).getName();
        customerRef.whereEqualTo("name", name).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String uid = task.getResult().getDocuments().get(0).getId();
            customerRef.document(uid).delete();
            logger.setCustomerLog("delete", name, user.getEmail());
            dialog.dismiss();
        });
    }

    @Override
    public void onClickRight(CustomerModel item, int position) {
        alertPassword(position, false);
    }

    @Override
    public void onRetainSwipe(CustomerModel item, int position) {
        adapter.retainSwipe(item, position);
    }
}