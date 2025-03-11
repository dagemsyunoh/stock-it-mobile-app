package com.lock.stockit;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Adapters.ReceiptAdapter;
import com.lock.stockit.Helpers.CustomLinearLayoutManager;
import com.lock.stockit.Helpers.QtyMover;
import com.lock.stockit.Helpers.ReceiptListeners;
import com.lock.stockit.Helpers.SwipeState;
import com.lock.stockit.Models.ReceiptModel;

import java.util.ArrayList;

public class ReceiptFragment extends Fragment implements ReceiptListeners {
    private final CollectionReference colRef = FirebaseFirestore.getInstance().collection("receipts");
    private final CollectionReference colRefStock = FirebaseFirestore.getInstance().collection("stocks");
    private final ArrayList<String> names = new ArrayList<>();
    private final ArrayList<String> namesNoRepeat = new ArrayList<>();
    private final ArrayList<String> sizes = new ArrayList<>();
    private final ArrayList<String> sizesNoRepeat = new ArrayList<>();
    private final ArrayList<Integer> qty = new ArrayList<>();
    private final ArrayList<Double> unitPrice = new ArrayList<>();
    private final String[] item = new String[5];
    protected RecyclerView recyclerView;
    protected FloatingActionButton addButton, saveButton, plusOne, minusOne;
    private NumberPicker itemName, itemSize;
    private TextInputEditText itemQty;
    private TextView title, itemUnitPrice, itemTotalPrice;
    private TextView noItem;
    private ArrayList<ReceiptModel> receiptList;
    private ReceiptAdapter adapter;
    private int transactionNo = 1, flag;

    public static Intent newIntent(Context context) {
        return new Intent(context, ReceiptListeners.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_receipt, container, false);
        title = view.findViewById(R.id.receipt_title);
        recyclerView = view.findViewById(R.id.receipt_view);
        addButton = view.findViewById(R.id.add_button);
        noItem = view.findViewById(R.id.no_item);
        receiptList = new ArrayList<>();

        noItem.setVisibility(View.VISIBLE);

        addButton.setOnClickListener(v -> popUp());

        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        fetchData();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchData() {
        // to fetch data for number pickers
        colRefStock.addSnapshotListener((value, error) -> {
           if (error != null || value == null) return;
           names.clear();
           sizes.clear();
           qty.clear();
           namesNoRepeat.clear();
           sizesNoRepeat.clear();
           for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
               names.add(documentSnapshot.getString("item name"));
               sizes.add(documentSnapshot.getString("item size"));
               qty.add(documentSnapshot.getDouble("qty").intValue());
               unitPrice.add(documentSnapshot.getDouble("price"));
               if (!namesNoRepeat.contains(documentSnapshot.getString("item name")))
                   namesNoRepeat.add(documentSnapshot.getString("item name"));
               if (!sizesNoRepeat.contains(documentSnapshot.getString("item size")))
                   sizesNoRepeat.add(documentSnapshot.getString("item size"));
           }
        });
        // to fetch transaction no.
        colRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            receiptList.clear();
            for (DocumentSnapshot documentSnapshot : value.getDocuments()) transactionNo++;
            String titleText = "Receipt #" + transactionNo;
            title.setText(titleText);
            setRecyclerView();
            if (receiptList.isEmpty()) noItem.setVisibility(View.VISIBLE);
        });
    }

    private void setRecyclerView() {
        adapter = new ReceiptAdapter(this, SwipeState.LEFT_RIGHT);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void popUp() {
        Dialog addPopUp = new Dialog(getActivity());
        addPopUp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        addPopUp.setContentView(R.layout.receipt_add);
        addPopUp.setTitle("Add Item to Receipt");
        addPopUp.show();

        itemName = addPopUp.findViewById(R.id.add_name);
        itemSize = addPopUp.findViewById(R.id.add_size);
        itemQty = addPopUp.findViewById(R.id.add_qty);
        itemUnitPrice = addPopUp.findViewById(R.id.unit_price_val_text);
        itemTotalPrice = addPopUp.findViewById(R.id.total_price_val_text);

        plusOne = addPopUp.findViewById(R.id.add_plus_one);
        minusOne = addPopUp.findViewById(R.id.add_minus_one);
        saveButton = addPopUp.findViewById(R.id.add_item);

        String[] nameDisplay = namesNoRepeat.toArray(new String[0]);
        setNumberPicker(itemName, nameDisplay, namesNoRepeat);

        String[] sizeDisplay = sizesNoRepeat.toArray(new String[0]);
        setNumberPicker(itemSize, sizeDisplay, sizesNoRepeat);

        itemQty.setOnFocusChangeListener((view, b) -> {
            if (checkStock()) return;
            setItem();
        });

        plusOne.setOnClickListener(view -> {
            if (checkStock()) return;
            QtyMover.onPlusOne(itemQty);
            setItem();
        });

        minusOne.setOnClickListener(view -> {
            QtyMover.onMinusOne(itemQty);
            setItem();
        });

        saveButton.setOnClickListener(v -> {
            setItem();
            receiptList.add(new ReceiptModel(item[0], item[1], Integer.parseInt(item[2]), Double.parseDouble(item[3]), Double.parseDouble(item[4])));
            adapter.setReceipts(receiptList);
            adapter.notifyDataSetChanged();
            noItem.setVisibility(View.GONE);
            addPopUp.dismiss();
        });
    }

    private boolean checkStock() {
        item[0] = namesNoRepeat.get(itemName.getValue());
        item[1] = sizesNoRepeat.get(itemSize.getValue());
        for (int i = 0; i < names.size(); i++) {
            if (item[0].equals(names.get(i)) && item[1].equals(sizes.get(i))) {
                flag = i;
            }
        }
        if (Integer.parseInt(itemQty.getText().toString()) > qty.get(flag)) {
            Toast.makeText(getActivity(), "Not enough stock", Toast.LENGTH_SHORT).show();
            itemQty.setText(qty.get(flag));
            return true;
        }
        return false;
    }

    private void setItem() {
        item[0] = namesNoRepeat.get(itemName.getValue());
        item[1] = sizesNoRepeat.get(itemSize.getValue());
        item[2] = itemQty.getText().toString();
        for (int i = 0; i < names.size(); i++) {
            if (item[0].equals(names.get(i)) && item[1].equals(sizes.get(i))) {
                flag = i;
                item[3] = String.valueOf(unitPrice.get(i));
                item[4] = String.valueOf(Double.parseDouble(item[2]) * Double.parseDouble(item[3]));
            }
        }
        itemUnitPrice.setText(item[3]);
        itemTotalPrice.setText(item[4]);
        Log.d("TAG", item[0] + " " + item[1] + " " + item[2] + " " + item[3] + " " + item[4]);
    }

    private void setNumberPicker(NumberPicker numberPicker, String[] array, ArrayList<String> arrayList) {
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(arrayList.size() - 1);
        numberPicker.setDisplayedValues(array);
        numberPicker.setWrapSelectorWheel(false);
    }


    @Override
    public void onClickRight(ReceiptModel item, int position) {

    }

    @Override
    public void onRetainSwipe(ReceiptModel item, int position) {

    }
}