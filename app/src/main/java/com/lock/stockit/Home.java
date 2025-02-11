package com.lock.stockit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class Home extends Fragment {
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    String type;
    TextView textView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        textView = view.findViewById(R.id.homeText);
        String uid = MainActivity.uid;

        DocumentReference docRef = firestore.collection("users").document(uid);
        docRef.get().addOnSuccessListener(doc -> type = doc.getString("type"))
                .addOnFailureListener(e -> type = "Error")
                .addOnCompleteListener(task -> textView.setText("You are an " + type));

        return view;
    }
}