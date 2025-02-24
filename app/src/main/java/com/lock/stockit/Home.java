package com.lock.stockit;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class Home extends Fragment {
    String type;
    TextView textView;
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        textView = view.findViewById(R.id.homeText);
        if (LoaderActivity.admin) {
            type = "Admin";
        }
        else {
            type = "User";
        }
        textView.setText("You are an " + type);

        return view;
    }
}