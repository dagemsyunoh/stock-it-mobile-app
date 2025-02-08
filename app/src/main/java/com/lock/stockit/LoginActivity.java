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

public class LoginActivity extends AppCompatActivity {
    TextInputEditText editTextEmail, editTextPassword;
    Button buttonSignIn;
    FirebaseAuth mAuth;
    ProgressBar progressBar;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            sign_in();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.sign_in_email);
        editTextPassword = findViewById(R.id.sign_in_password);
        buttonSignIn = findViewById(R.id.sign_in_button);
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

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Login successful.",
                                    Toast.LENGTH_SHORT).show();
                            sign_in();
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
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
    private void sign_in () {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }
}