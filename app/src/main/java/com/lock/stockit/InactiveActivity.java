package com.lock.stockit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class InactiveActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {

    private final Handler handler = new Handler();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseUser user = auth.getCurrentUser();
    protected TextView inactiveText, reload;
    protected Button buttonSignOut, buttonResend;
    private int i = 5; //temporary for testing, change back to 30 upon deployment
    private Runnable runnable;
    private boolean clicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inactive);

        clicked = false;
        inactiveText = findViewById(R.id.inactive_text);
        reload = findViewById(R.id.reload);
        buttonSignOut = findViewById(R.id.sign_out_button);
        buttonResend = findViewById(R.id.resend_button);

        if (!LoaderActivity.verified) {
            buttonResend.setVisibility(View.VISIBLE);
            inactiveText.setText(R.string.account_verify);
            reload.setText(R.string.reloadV);
            resendDelay();
        } else if (!LoaderActivity.activated){
            setLog();
            buttonResend.setVisibility(View.GONE);
            inactiveText.setText(R.string.account_inactive);
            reload.setText(R.string.reloadA);
        }

        buttonResend.setOnClickListener(v -> {
            i = 5; //to restart delay loop, change back to 30 upon deployment
            if (user == null || LoaderActivity.verified) return;
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful())
                    Toast.makeText(InactiveActivity.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(InactiveActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                    i = 3; //to restart delay loop early, do not change to 30
                }
            });
        });

        reload.setOnClickListener(v -> {
            clicked = true;
            //to inform AuthStateListener to recheck auth state
            auth.addAuthStateListener(this);
        });
        buttonSignOut.setOnClickListener(v -> auth.signOut());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    protected void setLog() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dateTime = formatter.format(new Date());
        Map<String, Object> log = new HashMap<>();
        log.put("action", "email verified");
        log.put("target", user.getEmail());
        log.put("user", user.getEmail());
        log.put("date-time", dateTime);
        FirebaseFirestore.getInstance().collection("user log").document().set(log);
    }
    private void resendDelay() {
        //delay loop will pause when i is 0, enabling resend button. Loop will continue if i value is changed.
        runnable = () -> {
            if (i == 0) {
                handler.removeCallbacks(runnable);
                buttonResend.setEnabled(true);
                buttonResend.setText(R.string.resend);
            } else {
                buttonResend.setEnabled(false);
                buttonResend.setText(String.valueOf(i));
                i--;
            }
            handler.postDelayed(runnable, 1000);
        };
        handler.postDelayed(runnable, 1000);
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
        if (firebaseAuth.getCurrentUser() != null && !clicked) return;
        Intent i = new Intent(getApplicationContext(), LoaderActivity.class);
        startActivity(i);
        finish();
    }
}