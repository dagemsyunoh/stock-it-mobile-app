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
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;
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
import com.lock.stockit.Helpers.sizeComparator;
import com.lock.stockit.Models.ReceiptModel;

import java.util.ArrayList;
import java.util.Locale;

public class ReceiptFragment extends Fragment implements ReceiptListeners {
    private final CollectionReference colRef = FirebaseFirestore.getInstance().collection("receipts");
    private final CollectionReference colRefStock = FirebaseFirestore.getInstance().collection("stocks");
    private final ArrayList<String> names = new ArrayList<>();
    private final ArrayList<String> namesUnique = new ArrayList<>();
    private final ArrayList<String> sizes = new ArrayList<>();
    private final ArrayList<String> sizesUnique = new ArrayList<>();
    private final ArrayList<Integer> qty = new ArrayList<>();
    private final ArrayList<Double> unitPrice = new ArrayList<>();
    private final ArrayList<Double> grandTotal = new ArrayList<>();
    private final String[] item = new String[5];
    protected RecyclerView recyclerView;
    protected FloatingActionButton addButton, saveButton, plusOne, minusOne;
    private NumberPicker itemName, itemSize;
    private TextInputEditText itemQty;
    private TextView title, itemUnitPrice, itemTotalPrice, noItem, grandTotalPrice;
    private LinearLayout grandTotalLayout;
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
        grandTotalLayout = view.findViewById(R.id.grand_total_layout);
        grandTotalPrice = view.findViewById(R.id.grand_total_val);
        receiptList = new ArrayList<>();

        addButton.setOnClickListener(v -> popUp());

        return view;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        fetchData();
        fetchTransNo();
    }

    private void fetchTransNo() {
        colRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            receiptList.clear();
            for (DocumentSnapshot documentSnapshot : value.getDocuments()) transactionNo++;
            String titleText = "Receipt #" + transactionNo;
            title.setText(titleText);
            setRecyclerView();
            setLayout(!receiptList.isEmpty());
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
        addPopUp.setCancelable(false);
        addPopUp.show();

        itemName = addPopUp.findViewById(R.id.add_name);
        itemSize = addPopUp.findViewById(R.id.add_size);
        itemQty = addPopUp.findViewById(R.id.add_qty);
        itemUnitPrice = addPopUp.findViewById(R.id.unit_price_val_text);
        itemTotalPrice = addPopUp.findViewById(R.id.total_price_val_text);
        AppCompatImageView back = addPopUp.findViewById(R.id.back);
        plusOne = addPopUp.findViewById(R.id.add_plus_one);
        minusOne = addPopUp.findViewById(R.id.add_minus_one);
        saveButton = addPopUp.findViewById(R.id.add_item);

        fetchData();
        namesUnique.sort(String::compareToIgnoreCase);
        String[] nameDisplay = namesUnique.toArray(new String[0]);
        setNumberPicker(itemName, nameDisplay, namesUnique);
        changeSizeValues(itemName.getValue());

        itemName.setOnValueChangedListener((np, oldPos, newPos) -> changeSizeValues(newPos));

        itemSize.setOnValueChangedListener((np, oldPos, newPos) -> setItem());

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
            setLayout(!receiptList.isEmpty());
            getSum();
            for (int i = 0; i < 5; i++) item[i] = "";
            adapter.setReceipts(receiptList);
            adapter.notifyDataSetChanged();
            addPopUp.dismiss();
        });

        back.setOnClickListener(v -> addPopUp.cancel());
    }

    private void setLayout(boolean show) {
        if (show) {
            grandTotalLayout.setVisibility(View.VISIBLE);
            noItem.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            grandTotalLayout.setVisibility(View.INVISIBLE);
            noItem.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        }
    }

    private void changeSizeValues(int pos) {
        String nameVal = namesUnique.get(pos);
        sizesUnique.clear();
        for (int i = 0; i < names.size() -1; i++) {
            if (names.get(i).equals(nameVal)) {
                sizesUnique.add(sizes.get(i));
            }
        }
        sizesUnique.sort(new sizeComparator());
        String[] sizeDisplay = sizesUnique.toArray(new String[0]);
        setNumberPicker(itemSize, sizeDisplay, sizesUnique);
        setItem();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchData() {
        // to fetch data for number pickers
        colRefStock.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            names.clear();
            sizes.clear();
            qty.clear();
            namesUnique.clear();
            sizesUnique.clear();
            for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                names.add(documentSnapshot.getString("item name"));
                sizes.add(documentSnapshot.getString("item size"));
                qty.add(documentSnapshot.getDouble("qty").intValue());
                unitPrice.add(documentSnapshot.getDouble("price"));
                if (!namesUnique.contains(documentSnapshot.getString("item name")))
                    namesUnique.add(documentSnapshot.getString("item name"));
                if (!sizesUnique.contains(documentSnapshot.getString("item size")))
                    sizesUnique.add(documentSnapshot.getString("item size"));
            }
        });
    }
    
    private boolean checkStock() {
        String iName = namesUnique.get(itemName.getValue());
        String iSize = sizesUnique.get(itemSize.getValue());
        for (int i = 0; i < names.size(); i++)
            if (iName.equals(names.get(i)) && iSize.equals(sizes.get(i))) flag = i;

        if (Integer.parseInt(itemQty.getText().toString()) > qty.get(flag)) {
            Toast.makeText(getActivity(), "Not enough stock", Toast.LENGTH_SHORT).show();
            return true;
        } return false;
    }

    private void setItem() {
        item[0] = namesUnique.get(itemName.getValue());
        item[1] = sizesUnique.get(itemSize.getValue());
        item[2] = itemQty.getText().toString();
        for (int i = 0; i < names.size(); i++) {
            if (item[0].equals(names.get(i)) && item[1].equals(sizes.get(i))) {
                flag = i;
                item[3] = String.valueOf(unitPrice.get(i));
                item[4] = String.valueOf(Double.parseDouble(item[2]) * Double.parseDouble(item[3]));
            }
        }
        String unitPriceText = "₱" + String.format(Locale.getDefault(), "%.2f", Double.parseDouble(item[3]));
        String totalPriceText = "₱" + String.format(Locale.getDefault(), "%.2f", Double.parseDouble(item[4]));
        itemUnitPrice.setText(unitPriceText);
        itemTotalPrice.setText(totalPriceText);
    }

    private void setNumberPicker(NumberPicker numberPicker, String[] array, ArrayList<String> arrayList) {
        numberPicker.setDisplayedValues(null);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(arrayList.size() - 1);
        numberPicker.setDisplayedValues(array);
    }

    private void getSum() {
        Double sum = 0.0;
        if (!item[4].isEmpty()) grandTotal.add(Double.parseDouble(item[4]));
        for (Double total : grandTotal) {
            sum += total;
            Log.d("TAG", String.valueOf(sum));
        }
        String sumText = "₱" + String.format(Locale.getDefault(), "%.2f", sum);
        grandTotalPrice.setText(sumText);
    }

    @Override
    public void onClickRight(ReceiptModel item, int position) {
        grandTotal.remove(position);
        getSum();
        receiptList.remove(position);
        adapter.setReceipts(receiptList);
        adapter.notifyItemRemoved(position);
        setLayout(!receiptList.isEmpty());
    }

    @Override
    public void onRetainSwipe(ReceiptModel item, int position) {

    }
}