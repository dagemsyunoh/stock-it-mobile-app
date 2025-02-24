package com.lock.stockit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoaderActivity extends AppCompatActivity {

    public static String uid;
    public static boolean admin, activated;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    FirebaseUser user = auth.getCurrentUser();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (user == null) {
            sign_out();
        }
        try {
            uid = user.getUid();
            DocumentReference docRef = firestore.collection("users").document(uid);
            if (SignUpActivity.create) {
                data_create(uid, user.getEmail(), docRef);
                SignUpActivity.create = false;
            }

            new Handler().postDelayed(() -> {
                docRef.get().addOnSuccessListener(doc -> {
                            if (!doc.exists()) {
                                sign_out();
                            }
                            else {
                                admin = Boolean.TRUE.equals(doc.getBoolean("admin"));
                                activated = Boolean.TRUE.equals(doc.getBoolean("activated"));
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.wtf("TAG", "Error Code " + e);
                        })
                        .addOnCompleteListener(task -> {
                            if (activated) {
                                sign_in();
                            } else {
                                sign_inactive();
                            }
                        });
            }, 1000);
        } catch (Exception e) {
            sign_out();
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loader);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void data_create(String uid, String email, DocumentReference docRef) {
        Map<String, Object> data = new HashMap<>();
        data.put("activated", false);
        data.put("admin", false);
        data.put("email", email);
        docRef.set(data);
    }
    public void sign_in() {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
        finish();
    }
    public void sign_inactive() {
        Intent i = new Intent(getApplicationContext(), InactiveActivity.class);
        startActivity(i);
        finish();
    }

    public void sign_out() {
        Intent i = new Intent(getApplicationContext(), SignInActivity.class);
        startActivity(i);
        finish();
    }
}