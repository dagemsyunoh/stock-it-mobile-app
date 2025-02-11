package com.lock.stockit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LogInActivity extends AppCompatActivity {
    TextInputEditText editTextEmail, editTextPassword;
    Button buttonSignIn;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    ProgressBar progressBar;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null){
            log_in();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.log_in_email);
        editTextPassword = findViewById(R.id.log_in_password);
        buttonSignIn = findViewById(R.id.log_in_button);
        progressBar = findViewById(R.id.progressBar);

        editTextEmail.setOnFocusChangeListener((v, hasFocus) -> {
            String email;
            email = String.valueOf(editTextEmail.getText());
            if (TextUtils.isEmpty(email)){
                editTextEmail.setError("Email is required.");
            }
        });

        editTextPassword.setOnFocusChangeListener((v, hasFocus) -> {
            String password;
            password = String.valueOf(editTextPassword.getText());
            if (TextUtils.isEmpty(password)){
                editTextPassword.setError("Password is required.");
            }
        });
        buttonSignIn.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String email, password;
            email = String.valueOf(editTextEmail.getText());
            password = String.valueOf(editTextPassword.getText());

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(LogInActivity.this, "Login successful.",
                                    Toast.LENGTH_SHORT).show();
                            log_in();
                        } else {
                            Toast.makeText(LogInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void log_in () {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }
}