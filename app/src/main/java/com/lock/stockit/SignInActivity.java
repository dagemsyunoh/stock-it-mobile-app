package com.lock.stockit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class SignInActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    protected Button buttonSignIn;
    protected TextView signUp;
    private TextInputEditText editTextEmail, editTextPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signin);

        editTextEmail = findViewById(R.id.sign_in_email);
        editTextPassword = findViewById(R.id.sign_in_password);
        buttonSignIn = findViewById(R.id.sign_in_button);
        signUp = findViewById(R.id.sign_up);
        progressBar = findViewById(R.id.progress_bar);

        editTextEmail.setOnFocusChangeListener((v, hasFocus) -> {
            String email;
            email = String.valueOf(editTextEmail.getText());
            if (TextUtils.isEmpty(email)) editTextEmail.setError("Email is required.");
        });

        editTextPassword.setOnFocusChangeListener((v, hasFocus) -> {
            String password;
            password = String.valueOf(editTextPassword.getText());
            if (TextUtils.isEmpty(password)) editTextPassword.setError("Password is required.");
        });

        buttonSignIn.setOnClickListener(v -> {
            String email, password;
            email = String.valueOf(editTextEmail.getText());
            password = String.valueOf(editTextPassword.getText());
            if (TextUtils.isEmpty(email)) {
                editTextEmail.setError("Email is required.");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                editTextPassword.setError("Password is required.");
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            authUser(email, password);
        });

        signUp.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), SignUpActivity.class);
            startActivity(i);
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void authUser(String email, String password) {
        buttonSignIn.setActivated(false);
        signUp.setClickable(false);

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(SignInActivity.this, "Sign in successful.", Toast.LENGTH_SHORT).show();
                return;
            }
            Exception exception = task.getException();
            if (exception instanceof FirebaseAuthException) {
                String errorCode = ((FirebaseAuthException) exception).getErrorCode();
                Log.d("TAG", errorCode);
                switch (errorCode) {
                    case "ERROR_INVALID_CREDENTIAL":
                        Toast.makeText(this, "Incorrect email or password. Please try again.", Toast.LENGTH_SHORT).show();
                        break;
                    case "ERROR_INVALID_EMAIL":
                        Toast.makeText(this, "Invalid email address. Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(this, "Authentication failed: " + errorCode, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
            else if (exception instanceof FirebaseNetworkException)
                Toast.makeText(SignInActivity.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Unknown error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        });
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
        if (firebaseAuth.getCurrentUser() == null) return;
        Intent i = new Intent(getApplicationContext(), LoaderActivity.class);
        startActivity(i);
        finish();
    }
}