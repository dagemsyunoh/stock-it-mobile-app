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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {
    static Boolean create = false;
    TextInputEditText editTextEmail, editTextPassword, editTextConfirmPassword;
    Button buttonSignUp;
    TextView signIn;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    ProgressBar progressBar;
    ArrayList<String> emails = new ArrayList<>();
    boolean flagExists;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null){
            loader();
        }
    }

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
        progressBar = findViewById(R.id.progressBar);

        firestore.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        emails.add(document.getString("email"));
                    }
                }
            }
        });

        buttonSignUp.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String email = String.valueOf(editTextEmail.getText());
            String password = String.valueOf(editTextPassword.getText());
            String confirmPassword = String.valueOf(editTextConfirmPassword.getText());
            Boolean valid = checker(email, password, confirmPassword);

            if (valid) {
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                Toast.makeText(SignUpActivity.this, "Account successfully created.",
                                        Toast.LENGTH_SHORT).show();
                                create = true;
                                loader();
                            } else {
                                Toast.makeText(SignUpActivity.this, "Account creation failed. Please try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
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
    private Boolean checker(String email, String password, String confirmPassword) {
        boolean emailValid = emailChecker(email);
        if (!emailValid) {
            return false;
        }
        else if (password.contains(email) || email.contains(password)){
            Toast.makeText(SignUpActivity.this, "Password cannot contain email.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password cannot contain email.");
            return false;
        }
        else {
            return passwordChecker(password, confirmPassword);
        }
    }

    private boolean emailChecker(String email) {
        flagExists = false;
        for (String e : emails) {
            if (e.equals(email)) {
                flagExists = true;
                break;
            }
        }
        if (TextUtils.isEmpty(email)){
            Toast.makeText(SignUpActivity.this, "Email is required.", Toast.LENGTH_SHORT).show();
            editTextEmail.setError("Email is required.");
            return false;
        }
        if (!email.contains("@") || !email.contains(".") || email.contains(" ")){
            Toast.makeText(SignUpActivity.this, "Invalid email.", Toast.LENGTH_SHORT).show();
            editTextEmail.setError("Invalid email.");
            return false;
        }
        if (flagExists) {
            Toast.makeText(SignUpActivity.this, "Email already exists.", Toast.LENGTH_SHORT).show();
            editTextEmail.setError("Email already exists.");
        }
        return !flagExists;
    }

    private boolean passwordChecker(String password,String confirmPassword) {
        Pattern specialChar = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Pattern upperCase = Pattern.compile("[A-Z ]");
        Pattern lowerCase = Pattern.compile("[a-z ]");
        Pattern digitCase = Pattern.compile("[0-9 ]");

        if (TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)){
            Toast.makeText(SignUpActivity.this, "Password is required.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password is required.");
            return false;
        }
        if (password.length() < 8){
            Toast.makeText(SignUpActivity.this, "Password must be at least 8 characters.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password must be at least 8 characters.");
            return false;
        }
        if (!specialChar.matcher(password).find()){
            Toast.makeText(SignUpActivity.this, "Password must contain a special character.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password must contain a special character.");
            return false;
        }
        if (!upperCase.matcher(password).find()){
            Toast.makeText(SignUpActivity.this, "Password must contain an uppercase letter.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password must contain an uppercase letter.");
            return false;
        }
        if (!lowerCase.matcher(password).find()){
            Toast.makeText(SignUpActivity.this, "Password must contain a lowercase letter.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password must contain a lowercase letter.");
            return false;
        }
        if (!digitCase.matcher(password).find()){
            Toast.makeText(SignUpActivity.this, "Password must contain a digit.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password must contain a digit.");
            return false;
        }
        if (password.contains(" ") || confirmPassword.contains(" ")){
            Toast.makeText(SignUpActivity.this, "Password cannot contain spaces.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password cannot contain spaces.");
            return false;
        }
        if (!password.equals(confirmPassword)){
            Toast.makeText(SignUpActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            editTextConfirmPassword.setError("Passwords do not match.");
            return false;
        }
        return true;
    }

    private void loader() {
        Intent i = new Intent(getApplicationContext(), LoaderActivity.class);
        startActivity(i);
        finish();
    }
}