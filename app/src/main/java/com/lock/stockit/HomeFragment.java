package com.lock.stockit;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    String type;
    TextView textView;
    @SuppressLint("SetTextI18n") //remove later
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        textView = view.findViewById(R.id.home_text);
        if (LoaderActivity.admin) type = "an Admin";
        else type = "a User";
        textView.setText("You are " + type);

        return view;
    }
}