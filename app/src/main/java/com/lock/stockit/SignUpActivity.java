package com.lock.stockit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Helpers.Logger;
import com.lock.stockit.Helpers.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference colRef = FirebaseFirestore.getInstance().collection("users");
    private final ArrayList<String> emails = new ArrayList<>();
    protected Button buttonSignUp;
    protected TextView signIn;
    protected boolean emailExists;
    private TextInputEditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private ProgressBar progressBar;
    private final Logger logger = new Logger();
    private final Validator validator = new Validator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        editTextEmail = findViewById(R.id.sign_up_email);
        editTextPassword = findViewById(R.id.sign_up_password);
        editTextConfirmPassword = findViewById(R.id.sign_up_confirm_password);
        buttonSignUp = findViewById(R.id.sign_up_button);
        signIn = findViewById(R.id.sign_in);
        progressBar = findViewById(R.id.progress_bar);

        buttonSignUp.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String email = String.valueOf(editTextEmail.getText());
            String password = String.valueOf(editTextPassword.getText());
            String confirmPassword = String.valueOf(editTextConfirmPassword.getText());
            Boolean valid = checker(editTextEmail, editTextPassword, editTextConfirmPassword);

            if (!valid) {
                progressBar.setVisibility(View.GONE);
                return;
            }
            buttonSignUp.setActivated(false);
            signIn.setClickable(false);
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(SignUpActivity.this, "Account successfully created.", Toast.LENGTH_SHORT).show();
                            Log.d("TAG", task.getResult().getUser().getUid());
                            createData(email, colRef.document(task.getResult().getUser().getUid()));
                            verifyEmail();
                            return;
                        }
                        Toast.makeText(SignUpActivity.this, "Account creation failed. Please try again.", Toast.LENGTH_SHORT).show();
                        buttonSignUp.setActivated(true);
                        signIn.setClickable(true);
                    });
            progressBar.setVisibility(View.GONE);
        });

        signIn.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), SignInActivity.class);
            startActivity(i);
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private Boolean checker(TextInputEditText email, TextInputEditText password, TextInputEditText confirmPassword) {
        boolean emailValid = validator.emailChecker(this, email);
        if (!emailValid) return false;
        else return validator.passwordChecker(this, password, confirmPassword, email.getText().toString(), null);
    }

    private void verifyEmail() {
        if (auth.getCurrentUser() == null) return;
        auth.getCurrentUser().sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(SignUpActivity.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(SignUpActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
            buttonSignUp.setActivated(true);
            signIn.setClickable(true);
        });
    }

    private void createData(String email, DocumentReference docRef) {
        Map<String, Object> data = new HashMap<>();
        data.put("activated", false);
        data.put("admin", false);
        data.put("email", email);
        data.put("name", "");
        data.put("store", "");
        docRef.set(data);
        logger.setUserLog("create", email, email);
    }

    @Override
    protected void onStart() {
        super.onStart();
        validator.fetchEmail();
        auth.addAuthStateListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(this);
    }
    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        if (firebaseAuth.getCurrentUser() == null) return;
        Intent i = new Intent(getApplicationContext(), LoaderActivity.class);
        startActivity(i);
        finish();
    }
}