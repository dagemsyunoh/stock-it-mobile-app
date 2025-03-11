package com.lock.stockit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MoreFragment extends Fragment {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseUser user = auth.getCurrentUser();
    protected Button buttonManage, buttonChangeEmail, buttonChangePassword, buttonSignOUt;
    private TextView emailUser;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_more, container, false);
        buttonManage = view.findViewById(R.id.manage_users);
        buttonChangeEmail = view.findViewById(R.id.change_email);
        buttonChangePassword = view.findViewById(R.id.change_password);
        buttonSignOUt = view.findViewById(R.id.sign_out_button);
        emailUser = view.findViewById(R.id.email_user);

        buttonManage.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ManageUsersActivity.class);
            startActivity(i);
        });

        buttonChangeEmail.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ChangeActivity.class);
            i.putExtra("change", "email");
            startActivity(i);
        });
        buttonChangePassword.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ChangeActivity.class);
            i.putExtra("change", "password");
            startActivity(i);
        });
        buttonSignOUt.setOnClickListener(v -> auth.signOut());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        String welcome = "Welcome, " + user.getEmail();
        emailUser.setText(welcome);
        if (LoaderActivity.admin) buttonManage.setVisibility(View.VISIBLE);
        else buttonManage.setVisibility(View.GONE);
    }
}