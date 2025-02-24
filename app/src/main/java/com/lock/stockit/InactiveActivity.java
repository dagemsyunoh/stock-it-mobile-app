package com.lock.stockit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class InactiveActivity extends AppCompatActivity {

    FirebaseAuth auth = FirebaseAuth.getInstance();
    Button reload_button, sign_out_button;
    FirebaseUser user = auth.getCurrentUser();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inactive);

        reload_button = findViewById(R.id.reload_button);
        sign_out_button = findViewById(R.id.sign_out_button);

        if (user == null) {
            loader();
        }

        reload_button.setOnClickListener(v -> loader());
        sign_out_button.setOnClickListener(v -> {
            auth.signOut();
            loader();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void loader() {
        Intent i = new Intent(getApplicationContext(), LoaderActivity.class);
        startActivity(i);
        finish();

    }
}