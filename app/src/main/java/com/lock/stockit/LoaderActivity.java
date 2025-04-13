package com.lock.stockit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Helpers.SecurePreferences;

import java.util.ArrayList;

public class LoaderActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {

    protected static String uid, sid;
    public static boolean admin, activated, verified;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseUser user = auth.getCurrentUser();
    private SecurePreferences preferences;

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

    private void delayLoad(DocumentReference docRef) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> getData(docRef), 1000);
    }

    private void getData(DocumentReference docRef) {
        docRef.get().addOnSuccessListener(this::findDoc)
                .addOnFailureListener(e -> Log.wtf("TAG", "Error Code " + e))
                .addOnCompleteListener(task -> checkUserStatus());
    }

    private void findDoc(DocumentSnapshot doc) {
        if (!doc.exists()) auth.signOut();
        else {
            admin = Boolean.TRUE.equals(doc.getBoolean("admin"));
            activated = Boolean.TRUE.equals(doc.getBoolean("activated"));
            verified = Boolean.TRUE.equals(user.isEmailVerified());
        }
    }

    private void checkUserStatus() {
        Intent i;
        if (activated && verified) i = new Intent(getApplicationContext(), MainActivity.class);
        else i = new Intent(getApplicationContext(), InactiveActivity.class);
        startActivity(i);
        finish();
    }

    private void fetchData() {
        String cashierName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.getString("store") != null) sid = documentSnapshot.getString("store");
            else sid = "";
            try {
                preferences = new SecurePreferences(getApplicationContext(), "store-preferences", "store-key", true);
                preferences.put("sid", sid);
                preferences.put("cashier name", cashierName);
            } catch (Exception e) {
                Log.e("TAG", e.getMessage());
            }
            if (!sid.isEmpty()) fetchHeader();
        });
    }

    private void fetchHeader() {
        FirebaseFirestore.getInstance().collection("stores").document(sid).get().addOnSuccessListener(documentSnapshot -> {
            ArrayList<String> header = new ArrayList<>();
            header.add(documentSnapshot.getString("name"));
            header.add(documentSnapshot.getString("address 1"));
            header.add(documentSnapshot.getString("address 2"));
            header.add(documentSnapshot.getString("contact"));
            try {
                preferences.put("name", header.get(0));
                preferences.put("address 1", header.get(1));
                preferences.put("address 2", header.get(2));
                preferences.put("contact", header.get(3));
            } catch (Exception e) {
                Log.e("TAG", e.getMessage());
            }
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
        if (firebaseAuth.getCurrentUser() == null) {
            Log.wtf("TAG", "AuthStateChanged:signed_out");
            Intent i = new Intent(getApplicationContext(), SignInActivity.class);
            startActivity(i);
            finish();
            return;
        }
        uid = user.getUid();
        fetchData();
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(uid);
        docRef.update("email", user.getEmail());
        docRef.get().addOnSuccessListener(documentSnapshot -> sid = documentSnapshot.getString("store"));

        delayLoad(docRef);
    }

}