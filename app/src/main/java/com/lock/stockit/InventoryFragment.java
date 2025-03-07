package com.lock.stockit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Adapters.StockAdapter;
import com.lock.stockit.Helpers.CustomLinearLayoutManager;
import com.lock.stockit.Helpers.StockListeners;
import com.lock.stockit.Helpers.SwipeState;
import com.lock.stockit.Models.StockModel;

import java.util.ArrayList;

public class InventoryFragment extends Fragment implements StockListeners {

    RecyclerView recyclerView;
    FloatingActionButton addButton;
    CollectionReference colRef = FirebaseFirestore.getInstance().collection("stocks");
    private ArrayList<StockModel> stockList;
    private StockAdapter adapter;

    public static Intent newIntent(Context context) {
        return new Intent(context, InventoryFragment.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_inventory, container, false);
        recyclerView = view.findViewById(R.id.stock_view);
        addButton = view.findViewById(R.id.add_stock);
        stockList = new ArrayList<>();

        addButton.setOnClickListener(v -> {
            //code for add item
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchData();
    }
    private void setRecyclerView() {
        adapter = new StockAdapter(this, SwipeState.LEFT_RIGHT);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }
    @SuppressLint("NotifyDataSetChanged")
    private void fetchData() {
        colRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            stockList.clear();

            for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                String name = documentSnapshot.getString("item name");
                String size = documentSnapshot.getString("item size");
                int qty = documentSnapshot.getDouble("qty").intValue();
                double price = documentSnapshot.getDouble("price");
                stockList.add(new StockModel(name, size, qty, price));
            }
            setRecyclerView();
            adapter.setStocks(stockList);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onClickLeft(StockModel item, int position) {
    }

    @Override
    public void onClickRight(StockModel item, int position) {

    }

    @Override
    public void onRetainSwipe(StockModel item, int position) {
        adapter.retainSwipe(item, position);
    }
}