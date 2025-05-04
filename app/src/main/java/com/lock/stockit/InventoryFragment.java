package com.lock.stockit;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Adapters.StockAdapter;
import com.lock.stockit.Helpers.CustomLinearLayoutManager;
import com.lock.stockit.Helpers.QtyEditor;
import com.lock.stockit.Helpers.StockComparator;
import com.lock.stockit.Helpers.StockListeners;
import com.lock.stockit.Helpers.SwipeState;
import com.lock.stockit.Models.StockModel;

import java.util.ArrayList;
import java.util.HashMap;

public class InventoryFragment extends Fragment implements StockListeners {

    protected final CollectionReference stockRef = FirebaseFirestore.getInstance().collection("stores").document(LoaderActivity.sid).collection("stocks");
    protected RecyclerView recyclerView;
    protected FloatingActionButton addButton, addItem, plusOne, minusOne;
    protected SearchView searchView;
    private TextView noResult;
    private ArrayList<StockModel> stockList;
    private StockAdapter adapter;
    private final ArrayList<String> names = new ArrayList<>();
    private final ArrayList<String> sizes = new ArrayList<>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_inventory, container, false);
        recyclerView = view.findViewById(R.id.stock_view);
        addButton = view.findViewById(R.id.add_stock);
        searchView = view.findViewById(R.id.stock_search);
        noResult = view.findViewById(R.id.no_result);
        stockList = new ArrayList<>();

        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterStocks(newText);
                return false;
            }
        });

        addButton.setOnClickListener(v -> addItemPopUp());

        return view;
    }

    private void filterStocks(String newText) {
        ArrayList<StockModel> filteredList = new ArrayList<>();
        for (StockModel item : stockList)
            if (item.getItemName().toLowerCase().contains(newText.toLowerCase()) || item.getItemSize().toLowerCase().contains(newText.toLowerCase()))
                filteredList.add(item);
        if (filteredList.isEmpty()) noResult.setVisibility(View.VISIBLE);
        else noResult.setVisibility(View.GONE);
        adapter.setStocks(filteredList);
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchData();
    }
    @SuppressLint("NotifyDataSetChanged")
    private void fetchData() {
        stockRef.addSnapshotListener((querySnapshot, error) -> {
            if (error != null || querySnapshot == null) return;
            stockList.clear();
            for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                String name = documentSnapshot.getString("item name");
                String size = documentSnapshot.getString("item size");
                double qty = documentSnapshot.getDouble("qty");
                String qtyType = documentSnapshot.getString("qty type");
                double regPrice = documentSnapshot.getDouble("reg price");
                double dscPrice = documentSnapshot.getDouble("dsc price");
                names.add(name);
                sizes.add(size);
                stockList.add(new StockModel(name, size, qty, qtyType, regPrice, dscPrice));
            }
            setRecyclerView();
            stockList.sort(new StockComparator());
            adapter.setStocks(stockList);
            adapter.notifyDataSetChanged();
        });
    }

    private void setRecyclerView() {
        adapter = new StockAdapter(this, SwipeState.LEFT_RIGHT);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    private void deleteItem(int pos) {
        String name = stockList.get(pos).getItemName();
        String size = stockList.get(pos).getItemSize();
        stockRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            for (DocumentSnapshot documentSnapshot : value.getDocuments())
                if (documentSnapshot.getString("item name").equals(name) && documentSnapshot.getString("item size").equals(size)) {
                    documentSnapshot.getReference().delete();
                    Toast.makeText(getActivity(), "Item Deleted", Toast.LENGTH_SHORT).show();
                }
        });
    }

    private void addItemPopUp() {
        Dialog addPopUp = new Dialog(getActivity());
        addPopUp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        addPopUp.setContentView(R.layout.stock_add);
        addPopUp.setTitle("Add Item to Inventory");
        addPopUp.show();
        TextInputEditText itemName = addPopUp.findViewById(R.id.add_name);
        TextInputEditText itemSize = addPopUp.findViewById(R.id.add_size);
        TextInputEditText itemQty = addPopUp.findViewById(R.id.add_qty);
        RadioGroup itemQtyType = addPopUp.findViewById(R.id.add_qty_type);
        TextInputEditText itemRegPrice = addPopUp.findViewById(R.id.add_reg_price);
        TextInputEditText itemDscPrice = addPopUp.findViewById(R.id.add_dsc_price);

        AppCompatImageView back = addPopUp.findViewById(R.id.back);
        plusOne = addPopUp.findViewById(R.id.add_plus_one);
        minusOne = addPopUp.findViewById(R.id.add_minus_one);
        addItem = addPopUp.findViewById(R.id.add_item);

        // Auto-capitalize first letter of each word of item name
        itemName.setOnFocusChangeListener((view, b) -> {
            String input = String.valueOf(itemName.getText());
            if (input.isEmpty()) return;
            String[] words = input.split("\\s");
            StringBuilder output = new StringBuilder();
            for (String word : words)
                output.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            itemName.setText(output.toString().trim());
        });
        // auto-change item size to lowercase without spaces
        itemSize.setOnFocusChangeListener((view, b) -> {
            String input = String.valueOf(itemSize.getText());
            String output = input.replaceAll("\\s+", "").toLowerCase();
            itemSize.setText(output);
        });

        plusOne.setOnClickListener(view -> QtyEditor.qtyEditor(itemQty, 1));

        minusOne.setOnClickListener(view -> {
            QtyEditor.qtyEditor(itemQty, -1);
            if (Integer.parseInt(itemQty.getText().toString()) == 1)
                Toast.makeText(getActivity(), "Quantity cannot be less than 1", Toast.LENGTH_SHORT).show();
        });

        addItem.setOnClickListener(view -> {
            int selectedId = itemQtyType.getCheckedRadioButtonId();
            RadioButton selectedQtyType = addPopUp.findViewById(selectedId);
            HashMap<String, Object> data = new HashMap<>();
            data.put("item name", itemName.getText().toString());
            data.put("item size", itemSize.getText().toString());
            data.put("qty", Double.parseDouble(itemQty.getText().toString()));
            data.put("qty type", selectedQtyType.getText().toString());
            data.put("reg price", Double.parseDouble(itemRegPrice.getText().toString()));
            data.put("dsc price", Double.parseDouble(itemDscPrice.getText().toString()));

            if (isInvalid(data)) {
                Toast.makeText(getActivity(), "Invalid input", Toast.LENGTH_SHORT).show();
                return;
            } if (exists(data)) {
                Toast.makeText(getActivity(), "Item already exists. Please edit the existing item instead", Toast.LENGTH_SHORT).show();
                addPopUp.dismiss();
                return;
            }

            stockRef.add(data).addOnSuccessListener(documentReference -> {
                Toast.makeText(getActivity(), "Item added", Toast.LENGTH_SHORT).show();
                addPopUp.dismiss();
            }).addOnFailureListener(e -> Toast.makeText(getActivity(), "Error adding item", Toast.LENGTH_SHORT).show());
        });

        back.setOnClickListener(v -> addPopUp.cancel());
    }

    private boolean isInvalid(HashMap<String, Object> data) {
        // check if name and size are not empty
        if (data.get("item name").toString().isEmpty() ||
                data.get("item size").toString().isEmpty() ||
                data.get("qty type").toString().isEmpty())
            return true;
        // check if qty and price are greater than 0
        return Double.parseDouble(data.get("qty").toString()) <= 0 &&
                Double.parseDouble(data.get("reg price").toString()) <= 0 &&
                Double.parseDouble(data.get("dsc price").toString()) <= 0;
    }

    private boolean exists(HashMap<String, Object> data) {
        boolean nameExists = false;
        boolean sizeExists = false;
        ArrayList<Boolean> nameFlag = new ArrayList<>();
        // check if name exists
        for (String n : names)
            if (n.equals(data.get("item name").toString())) {
                nameExists = true;
                nameFlag.add(true);
            } else nameFlag.add(false);
        // check if size exists
        for (int i = 0; i < sizes.size(); i++) {
            if (nameFlag.get(i)) if (sizes.get(i).equals(data.get("item size").toString())) {
                sizeExists = true;
                break;
            }
            sizeExists = false;
        }
        // return true if both name and size exist to prevent duplication
        return nameExists && sizeExists;
    }

    @Override
    public void onClickLeft(StockModel item, int position) { }

    @Override
    public void onClickRight(StockModel item, int position) {
        deleteItem(position);
    }

    @Override
    public void onRetainSwipe(StockModel item, int position) {
        adapter.retainSwipe(item, position);
    }

}