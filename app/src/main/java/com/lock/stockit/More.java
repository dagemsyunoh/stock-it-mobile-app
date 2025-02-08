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

    FirebaseAuth auth;
    Button button;
    TextView textView;
    FirebaseUser user;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_more, container, false);
        auth = FirebaseAuth.getInstance();
        button = view.findViewById(R.id.sign_out_button);
        textView = view.findViewById(R.id.email_user);
        user = auth.getCurrentUser();

        if (user == null) {
            sign_out();
        }
        else {
            textView.setText(user.getEmail());
        }

        button.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            sign_out();
        });

        return view;

    }
    private void sign_out () {
        Intent i = new Intent(getActivity(), LoginActivity.class);
        startActivity(i);
        requireActivity().finish();
    }
}