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
    Button button;
    TextView textView;
    FirebaseUser user = auth.getCurrentUser();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_more, container, false);
        button = view.findViewById(R.id.log_out_button);
        textView = view.findViewById(R.id.email_user);

        if (user == null) {
            log_out();
        }
        else {
            textView.setText(user.getEmail());
        }

        button.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            log_out();
        });

        return view;

    }
    private void log_out() {
        Intent i = new Intent(getActivity(), LogInActivity.class);
        startActivity(i);
        requireActivity().finish();
    }
}