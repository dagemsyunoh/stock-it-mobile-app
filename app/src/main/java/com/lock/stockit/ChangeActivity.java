package com.lock.stockit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

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
                String newPassword, confirmPassword;
                newPassword = String.valueOf(changePassword.getText());
                confirmPassword = String.valueOf(changeConfirmPassword.getText());
                newPassword(oldPassword, newPassword, confirmPassword);
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

    @SuppressWarnings("deprecation")
    private void changeLayout(String change) {
        String text = "Enter your new " + change;
        String button = "Change " + change;
        changeText.setText(text);
        changeButton.setText(button);

        passwordLayout.setPasswordVisibilityToggleEnabled(false);
        changeEmail.setText("");
        changePassword.setText("");
        changeConfirmPassword.setText("");

        if (change.equals("email")) {
            fetchEmail();
            changeEmail.setVisibility(View.VISIBLE);
            passwordLayout.setVisibility(View.GONE);
            confirmPasswordLayout.setVisibility(View.GONE);
        } else {
            changeEmail.setVisibility(View.GONE);
            changePassword.setVisibility(View.VISIBLE);
            confirmPasswordLayout.setVisibility(View.VISIBLE);
            changePassword.setHint(R.string.new_password);
            passwordLayout.setPasswordVisibilityToggleEnabled(true);
        }
    }
    private void fetchEmail() {
        colRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            QuerySnapshot querySnapshot = task.getResult();
            if (querySnapshot == null) return;
            for (DocumentSnapshot document : querySnapshot.getDocuments())
                emails.add(document.getString("email"));
        });
    }

    private void newEmail() {
        String email = String.valueOf(changeEmail.getText());
        if (!emailChecker(email)) return;
        user.verifyBeforeUpdateEmail(email).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(ChangeActivity.this, "Error. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Email verification required.")
                    .setMessage("Please verify your new email before signing in again.\n Email will only be changed after verification.")
                    .setNegativeButton("OK", (dialog, which) -> {
                        auth.signOut();
                        dialog.dismiss();
                    })
                    .create();
            alertDialog.show();
        });
        progressBar.setVisibility(View.GONE);
    }

    private void newPassword(String oldPassword, String password, String confirmPassword) {
        if (!passwordChecker(oldPassword, password, confirmPassword)) return;
        user.updatePassword(password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ChangeActivity.this, "Password successfully changed.", Toast.LENGTH_SHORT).show();
                finish();
            } else
                Toast.makeText(ChangeActivity.this, "Error. Please try again.", Toast.LENGTH_SHORT).show();
        });
        progressBar.setVisibility(View.GONE);
    }

    private boolean emailChecker(String email) {
        emailExists = false; //returns true if email exists in database
        for (String e : emails)
            if (e.equals(email)) {
                emailExists = true;
                break;
            }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(ChangeActivity.this, "Email is required.", Toast.LENGTH_SHORT).show();
            changeEmail.setError("Email is required.");
            return false;
        } if (!email.contains("@") || !email.contains(".") || email.contains(" ")) {
            Toast.makeText(ChangeActivity.this, "Invalid email.", Toast.LENGTH_SHORT).show();
            changeEmail.setError("Invalid email.");
            return false;
        } if (Objects.equals(user.getEmail(), email)) {
            Toast.makeText(ChangeActivity.this, "New email cannot be the same as the old email.", Toast.LENGTH_SHORT).show();
            changeEmail.setError("New email cannot be the same as the old email.");
            return false;
        } if (emailExists) {
            Toast.makeText(ChangeActivity.this, "Email already exists.", Toast.LENGTH_SHORT).show();
            changeEmail.setError("Email already exists.");
        }
        return !emailExists;
    }

    private boolean passwordChecker(String oldPassword, String newPassword,String confirmPassword) {
        Pattern specialChar = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Pattern upperCase = Pattern.compile("[A-Z ]");
        Pattern lowerCase = Pattern.compile("[a-z ]");
        Pattern digitCase = Pattern.compile("[0-9 ]");
        changePassword.setText("");
        changeConfirmPassword.setText("");

        if (TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(ChangeActivity.this, "Password is required.", Toast.LENGTH_SHORT).show();
            changePassword.setError("Password is required.");
            return false;
        } if (newPassword.length() < 8) {
            Toast.makeText(ChangeActivity.this, "Password must be at least 8 characters.", Toast.LENGTH_SHORT).show();
            changePassword.setError("Password must be at least 8 characters.");
            return false;
        } if (!specialChar.matcher(newPassword).find()) {
            Toast.makeText(ChangeActivity.this, "Password must contain a special character.", Toast.LENGTH_SHORT).show();
            changePassword.setError("Password must contain a special character.");
            return false;
        } if (!upperCase.matcher(newPassword).find()) {
            Toast.makeText(ChangeActivity.this, "Password must contain an uppercase letter.", Toast.LENGTH_SHORT).show();
            changePassword.setError("Password must contain an uppercase letter.");
            return false;
        } if (!lowerCase.matcher(newPassword).find()) {
            Toast.makeText(ChangeActivity.this, "Password must contain a lowercase letter.", Toast.LENGTH_SHORT).show();
            changePassword.setError("Password must contain a lowercase letter.");
            return false;
        } if (!digitCase.matcher(newPassword).find()) {
            Toast.makeText(ChangeActivity.this, "Password must contain a digit.", Toast.LENGTH_SHORT).show();
            changePassword.setError("Password must contain a digit.");
            return false;
        } if (newPassword.contains(" ") || confirmPassword.contains(" ")) {
            Toast.makeText(ChangeActivity.this, "Password cannot contain spaces.", Toast.LENGTH_SHORT).show();
            changePassword.setError("Password cannot contain spaces.");
            return false;
        } if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(ChangeActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            changeConfirmPassword.setError("Passwords do not match.");
            return false;
        } if (newPassword.equals(oldPassword)) {
            Toast.makeText(ChangeActivity.this, "New password cannot be the same as the old password.", Toast.LENGTH_SHORT).show();
            changePassword.setError("New password cannot be the same as the old password.");
            changePassword.setText("");
            changeConfirmPassword.setText("");
            return false;
        }
        return true;
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
