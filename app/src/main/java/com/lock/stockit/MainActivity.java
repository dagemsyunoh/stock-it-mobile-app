package com.lock.stockit;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Adapters.ViewPagerAdapter;


public class MainActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(LoaderActivity.uid);
    protected ViewPager2 viewPager2;
    protected ViewPagerAdapter viewPagerAdapter;
    protected BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_nav);
        viewPager2 = findViewById(R.id.view_pager);
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager2.setAdapter(viewPagerAdapter);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (LoaderActivity.admin) switch (id) {
                    case R.id.receipt:
                        viewPager2.setCurrentItem(1);
                        break;
                    case R.id.inventory:
                        viewPager2.setCurrentItem(2);
                        break;
                    case R.id.more:
                        viewPager2.setCurrentItem(3);
                        break;
                    case R.id.home:
                    default:
                        viewPager2.setCurrentItem(0);
                        break;
                }
                else switch (id) {
                    case R.id.receipt:
                        viewPager2.setCurrentItem(1);
                        break;
                    case R.id.more:
                        viewPager2.setCurrentItem(2);
                        break;
                    case R.id.home:
                    default:
                        viewPager2.setCurrentItem(0);
                        break;
                }
                return false;
            }
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (LoaderActivity.admin) switch (position) {
                    case 1:
                        bottomNavigationView.getMenu().findItem(R.id.receipt).setChecked(true);
                        break;
                    case 2:
                        bottomNavigationView.getMenu().findItem(R.id.inventory).setChecked(true);
                        break;
                    case 3:
                        bottomNavigationView.getMenu().findItem(R.id.more).setChecked(true);
                        break;
                    case 0:
                    default:
                        bottomNavigationView.getMenu().findItem(R.id.home).setChecked(true);
                        break;
                }
                else switch (position) {
                    case 1:
                        bottomNavigationView.getMenu().findItem(R.id.receipt).setChecked(true);
                        break;
                    case 2:
                        bottomNavigationView.getMenu().findItem(R.id.more).setChecked(true);
                        break;
                    case 0:
                    default:
                        bottomNavigationView.getMenu().findItem(R.id.home).setChecked(true);
                        break;
                }
                super.onPageSelected(position);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        docRef.get().addOnSuccessListener(doc -> LoaderActivity.admin = Boolean.TRUE.equals(doc.getBoolean("admin")))
                .addOnFailureListener(e -> LoaderActivity.admin = false)
                .addOnCompleteListener(task -> bottomNavigationView.getMenu().findItem(R.id.inventory).setVisible(LoaderActivity.admin));
        auth.addAuthStateListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(this);
    }
    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        if (firebaseAuth.getCurrentUser() != null) return;
        Intent i = new Intent(getApplicationContext(), LoaderActivity.class);
        startActivity(i);
        finish();
    }
}