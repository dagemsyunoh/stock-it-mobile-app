package com.lock.stockit;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Helpers.Logger;
import com.lock.stockit.Helpers.Validator;

import java.util.ArrayList;

public class ChangeActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseUser user = auth.getCurrentUser();
    private final CollectionReference colRef = FirebaseFirestore.getInstance().collection("users");
    private final ArrayList<String> emails = new ArrayList<>();
    protected AuthCredential credential;
    protected ExtendedFloatingActionButton buttonBack;
    protected boolean authenticated = false, emailExists;
    private TextView changeText;
    private TextInputLayout passwordLayout, confirmPasswordLayout;
    private TextInputEditText changeEmail, changePassword, changeConfirmPassword;
    private ProgressBar progressBar;
    private Button changeButton;
    private String oldPassword;
    private final Logger logger = new Logger();
    private final Validator validator = new Validator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change);

        changeText = findViewById(R.id.change_text);
        changeEmail = findViewById(R.id.change_email);
        changePassword = findViewById(R.id.change_password);
        changeConfirmPassword = findViewById(R.id.change_confirm_password);
        passwordLayout = findViewById(R.id.password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        progressBar = findViewById(R.id.progress_bar);
        buttonBack = findViewById(R.id.back_button);
        changeButton = findViewById(R.id.change_button);

        String text = "Confirm it's you\nChange " + getIntent().getStringExtra("change");
        changeText.setText(text);

        changeButton.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            // Check if user is authenticated
            if (!authenticated) reAuth();
            // If user wants to change email
            else if(getIntent().getStringExtra("change").equals("email")) newEmail();
            // If user wants to change password
            else {
                newPassword(oldPassword, changePassword, changeConfirmPassword);
            }
        });

        buttonBack.setOnClickListener(v -> finish());


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void reAuth() {
        String email = user.getEmail();
        oldPassword = String.valueOf(changePassword.getText());
        assert email != null;
        credential = EmailAuthProvider.getCredential(email, oldPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        authenticated = true;
                        Toast.makeText(ChangeActivity.this, "Re-authentication successful.", Toast.LENGTH_SHORT).show();
                        changeLayout(getIntent().getStringExtra("change"));
                    } else
                        Toast.makeText(ChangeActivity.this, "Re-authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void changeLayout(String change) {
        String text = "Enter your new " + change;
        String button = "Change " + change;
        changeText.setText(text);
        changeButton.setText(button);

        passwordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        changeEmail.setText("");
        changePassword.setText("");
        changeConfirmPassword.setText("");

        if (change.equals("email")) {
            validator.fetchEmail();
            changeEmail.setVisibility(View.VISIBLE);
            passwordLayout.setVisibility(View.GONE);
            confirmPasswordLayout.setVisibility(View.GONE);
        } else {
            changeEmail.setVisibility(View.GONE);
            changePassword.setVisibility(View.VISIBLE);
            confirmPasswordLayout.setVisibility(View.VISIBLE);
            changePassword.setHint(R.string.new_password);
            passwordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }
    }

    private void newEmail() {
        String email = String.valueOf(changeEmail.getText());
        if (!validator.emailChecker(this, changeEmail)) return;
        user.verifyBeforeUpdateEmail(email).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(ChangeActivity.this, "Error. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }
            Dialog changeDialog = new Dialog(this);
            changeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            changeDialog.setContentView(R.layout.dialog_box);
            changeDialog.setCancelable(false);
            changeDialog.create();
            changeDialog.show();
            TextView header = changeDialog.findViewById(R.id.header);
            header.setText(R.string.email_verification_required);
            TextView text = changeDialog.findViewById(R.id.text);
            text.setText(R.string.email_verification_required_text);
            Button buttonOk = changeDialog.findViewById(R.id.ok_button);
            Button buttonCancel = changeDialog.findViewById(R.id.cancel_button);
            buttonCancel.setVisibility(View.GONE);

            buttonOk.setOnClickListener(v -> {
                logger.setUserLog("email changed", user.getEmail(), user.getEmail());
                auth.signOut();
                changeDialog.dismiss();
            });
        });
        progressBar.setVisibility(View.GONE);
    }

    private void newPassword(String oldPassword, EditText password, EditText confirmPassword) {
        if (!validator.passwordChecker(this, password, confirmPassword, user.getEmail(), oldPassword)) return;
        user.updatePassword(password.getText().toString()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ChangeActivity.this, "Password successfully changed. Please sign in again.", Toast.LENGTH_SHORT).show();
                logger.setUserLog("password changed", user.getEmail(), user.getEmail());
                auth.signOut();
                finish();
            } else
                Toast.makeText(ChangeActivity.this, "Error. Please try again.", Toast.LENGTH_SHORT).show();
        });
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(this);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        if (firebaseAuth.getCurrentUser() != null) return;
        Intent i = new Intent(getApplicationContext(), LoaderActivity.class);
        startActivity(i);
        finish();
    }
}
