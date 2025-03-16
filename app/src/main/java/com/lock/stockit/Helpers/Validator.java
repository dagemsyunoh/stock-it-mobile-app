package com.lock.stockit.Helpers;

import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Validator {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseUser user = auth.getCurrentUser();
    private final CollectionReference userRef = FirebaseFirestore.getInstance().collection("users");
    private final ArrayList<String> emails = new ArrayList<>();
    public void fetchEmail() {
        userRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            QuerySnapshot querySnapshot = task.getResult();
            if (querySnapshot == null) return;
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                emails.add(document.getString("email"));
            }
        });
    }
    public boolean emailChecker(Context context, EditText editTextEmail) {
        boolean emailExists = false; // returns true if email exists in database
        String email = String.valueOf(editTextEmail.getText());
        String userEmail = ""; // for new users
        if (context.getClass().getSimpleName().equals("ChangeActivity")) userEmail = user.getEmail(); // for existing users
        for (String e : emails)
            if (e.equals(email)) {
                emailExists = true;
                break;
            }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(context, "Email is required.", Toast.LENGTH_SHORT).show();
            editTextEmail.setError("Email is required.");
            return false;
        } if (userEmail.equals(email)) {
            Toast.makeText(context, "New email cannot be the same as the old email.", Toast.LENGTH_SHORT).show();
            editTextEmail.setError("New email cannot be the same as the old email.");
            return false;
        } if (!email.contains("@") || !email.contains(".") || email.contains(" ")){
            Toast.makeText(context, "Invalid email.", Toast.LENGTH_SHORT).show();
            editTextEmail.setError("Invalid email.");
            return false;
        } if (emailExists) {
            Toast.makeText(context, "Email already exists.", Toast.LENGTH_SHORT).show();
            editTextEmail.setError("Email already exists.");
        } return !emailExists; // inverted, so returns true if email doesn't exist in database
    }

    public boolean passwordChecker(Context context,EditText editTextPassword, EditText editTextConfirmPassword, String email, String oldPassword) {
        String password = String.valueOf(editTextPassword.getText());
        String confirmPassword = String.valueOf(editTextConfirmPassword.getText());
        Pattern specialChar = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Pattern upperCase = Pattern.compile("[A-Z ]");
        Pattern lowerCase = Pattern.compile("[a-z ]");
        Pattern digitCase = Pattern.compile("[0-9 ]");

        boolean containsElement = split(email, password, specialChar);

        if (TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(context, "Password is required.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password is required.");
            return false;
        } if (password.length() < 8) {
            Toast.makeText(context, "Password must be at least 8 characters.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password must be at least 8 characters.");
            return false;
        } if (!specialChar.matcher(password).find()) {
            Toast.makeText(context, "Password must contain a special character.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password must contain a special character.");
            return false;
        } if (!upperCase.matcher(password).find()) {
            Toast.makeText(context, "Password must contain an uppercase letter.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password must contain an uppercase letter.");
            return false;
        } if (!lowerCase.matcher(password).find()) {
            Toast.makeText(context, "Password must contain a lowercase letter.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password must contain a lowercase letter.");
            return false;
        } if (!digitCase.matcher(password).find()) {
            Toast.makeText(context, "Password must contain a digit.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password must contain a digit.");
            return false;
        } if (password.contains(" ") || confirmPassword.contains(" ")) {
            Toast.makeText(context, "Password cannot contain spaces.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password cannot contain spaces.");
            return false;
        } if (containsElement){
            Toast.makeText(context, "Password cannot contain email.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password cannot contain email.");
            return false;
        } if (!password.equals(confirmPassword)) {
            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            editTextConfirmPassword.setError("Passwords do not match.");
            return false;
        } if (password.equals(oldPassword)) {
            Toast.makeText(context, "New password cannot be the same as the old password.", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("New password cannot be the same as the old password.");
            editTextPassword.setText("");
            editTextConfirmPassword.setText("");
            return false;
        }
        return true;
    }

    private boolean split(String email, String password, Pattern specialChar) {
        ArrayList<String> emailSplit = new ArrayList<>(Arrays.asList(email.split(String.valueOf(specialChar))));
        ArrayList<String> passwordSplit = new ArrayList<>(Arrays.asList(password.split(String.valueOf(specialChar))));
        for (String e : emailSplit) if (passwordSplit.contains(e)) return true;
        for (String p : passwordSplit) if (emailSplit.contains(p)) return true;
        return false;
    }

}
