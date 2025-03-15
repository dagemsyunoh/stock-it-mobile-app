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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoaderActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {

    protected static String uid;
    public static boolean admin, activated, verified;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final CollectionReference colRef = FirebaseFirestore.getInstance().collection("users");
    private final FirebaseUser user = auth.getCurrentUser();
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
        new Handler().postDelayed(() -> getData(docRef), 1000);
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
        DocumentReference docRef = colRef.document(uid);
        docRef.update("email", user.getEmail());

        delayLoad(docRef);
    }

}