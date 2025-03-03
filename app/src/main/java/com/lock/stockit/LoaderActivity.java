package com.lock.stockit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoaderActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {

    public static String uid;
    public static boolean admin, activated, verified;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    CollectionReference colRef = FirebaseFirestore.getInstance().collection("users");
    FirebaseUser user = auth.getCurrentUser();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loader);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void data_create(String email, DocumentReference docRef) {
        Map<String, Object> data = new HashMap<>();
        data.put("activated", false);
        data.put("admin", false);
        data.put("email", email);
        docRef.set(data);
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
        if (firebaseAuth.getCurrentUser() == null) {
            Log.wtf("TAG", "AuthStateChanged:signed_out");
            Intent i = new Intent(getApplicationContext(), SignInActivity.class);
            startActivity(i);
            finish();
        }
        else {
            uid = user.getUid();
            DocumentReference docRef = colRef.document(uid);
            if (SignUpActivity.create) {
                data_create(user.getEmail(), docRef);
                SignUpActivity.create = false;
            }
            docRef.update("email", user.getEmail());

            new Handler().postDelayed(() -> docRef.get().addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            auth.signOut();
                        }
                        else {
                            admin = Boolean.TRUE.equals(doc.getBoolean("admin"));
                            activated = Boolean.TRUE.equals(doc.getBoolean("activated"));
                            verified = Boolean.TRUE.equals(user.isEmailVerified());
                        }
                    })
                    .addOnFailureListener(e -> Log.wtf("TAG", "Error Code " + e))
                    .addOnCompleteListener(task -> {
                        Log.wtf("TAG", "OnAuthStateChanged:signed_in: " + user.getEmail());
                        Log.wtf("TAG", "Verified: " + user.isEmailVerified());
                        Log.wtf("TAG", "Admin: " + admin);
                        Log.wtf("TAG", "Activated: " + activated);
                        Intent i;
                        if (activated && verified) {
                            i = new Intent(getApplicationContext(), MainActivity.class);
                        } else {
                            i = new Intent(getApplicationContext(), InactiveActivity.class);
                        }
                        startActivity(i);
                        finish();
                    }), 1000);
        }
    }
}