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

public class More extends Fragment {
    FirebaseAuth auth = FirebaseAuth.getInstance();
    public static String change;
    Button buttonManage, buttonChangeEmail, buttonChangePassword, buttonSignOUt;
    FirebaseUser user = auth.getCurrentUser();
    TextView emailUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_more, container, false);
        buttonManage = view.findViewById(R.id.manage_users);
        buttonChangeEmail = view.findViewById(R.id.change_email);
        buttonChangePassword = view.findViewById(R.id.change_password);
        buttonSignOUt = view.findViewById(R.id.sign_out_button);
        emailUser = view.findViewById(R.id.email_user);

        if (user == null) {
            loader();
        }
        else {
            emailUser.setText(user.getEmail());
        }
        if (LoaderActivity.admin) {
            buttonManage.setVisibility(View.VISIBLE);
        }
        else {
            buttonManage.setVisibility(View.GONE);
        }

        buttonManage.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ManageUsersActivity.class);
            startActivity(i);
        });

        buttonChangeEmail.setOnClickListener(v -> {
            change = "email";
            Intent i = new Intent(getActivity(), ChangeActivity.class);
            startActivity(i);
        });
        buttonChangePassword.setOnClickListener(v -> {
            change = "password";
            Intent i = new Intent(getActivity(), ChangeActivity.class);
            startActivity(i);
        });
        buttonSignOUt.setOnClickListener(v -> {
            auth.signOut();
            loader();
        });

        return view;

    }
    private void loader() {
        Intent i = new Intent(getActivity(), LoaderActivity.class);
        startActivity(i);
        requireActivity().finish();
    }
}