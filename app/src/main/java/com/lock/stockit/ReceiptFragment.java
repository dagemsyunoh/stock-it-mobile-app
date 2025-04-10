package com.lock.stockit;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.lock.stockit.Adapters.ReceiptAdapter;
import com.lock.stockit.Helpers.CustomLinearLayoutManager;
import com.lock.stockit.Helpers.QtyEditor;
import com.lock.stockit.Helpers.ReceiptListeners;
import com.lock.stockit.Helpers.SecurePreferences;
import com.lock.stockit.Helpers.SwipeState;
import com.lock.stockit.Helpers.sizeComparator;
import com.lock.stockit.Helpers.stockComparator;
import com.lock.stockit.Models.ReceiptModel;
import com.lock.stockit.Models.StockModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class ReceiptFragment extends Fragment implements ReceiptListeners {

    private final DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(LoaderActivity.uid);
    private final CollectionReference colRef = FirebaseFirestore.getInstance().collection("receipts");
    private final CollectionReference colRefStock = FirebaseFirestore.getInstance().collection("stocks");
    private final CollectionReference colRefCustomer = FirebaseFirestore.getInstance().collection("customers");
    private final DocumentReference docRef = FirebaseFirestore.getInstance().collection("format").document("store info");
    public static final ArrayList<StockModel> stockList = new ArrayList<>();
    protected RecyclerView recyclerView;
    protected FloatingActionButton addButton, printButton, saveButton, plusOne, minusOne;
    private NumberPicker itemName, itemSize;
    private TextInputEditText itemQty;
    private TextView title, itemUnitPrice, itemTotalPrice, noItem, grandTotalPrice;
    private LinearLayout grandTotalLayout;
    private ReceiptAdapter adapter;
    private final ArrayList<String> customerList = new ArrayList<>();
    private final ArrayList<String> namesUnique = new ArrayList<>();
    private final ArrayList<String> sizesUnique = new ArrayList<>();
    private final ArrayList<Double> grandTotal = new ArrayList<>();
    private final static ArrayList<ReceiptModel> receiptList = new ArrayList<>();
    private final String[] item = new String[5];
    private final ArrayList<String> header = new ArrayList<>();
    private int transactionNo, flag;
    private boolean cancelled, discountFlag, recyclerViewFlag = true;
    private String invoice;
    private double cash = 0, sum;

    private Dialog addPopUp;
    private SecurePreferences preferences;

    public static Intent newIntent(Context context) {
        return new Intent(context, ReceiptListeners.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @SuppressLint("NotifyDataSetChanged")
    ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Toast.makeText(getActivity(), "Print successful", Toast.LENGTH_SHORT).show();
            receiptList.clear();
            grandTotal.clear();
            setSum();
            adapter.setReceipts(receiptList);
            setLayout(!receiptList.isEmpty());
        }
        if (result.getResultCode() == RESULT_CANCELED) {
            receiptList.clear();
            receiptList.addAll(result.getData().getExtras().getParcelableArrayList("receiptList"));
            getSum();
            adapter.setReceipts(receiptList);
            adapter.notifyDataSetChanged();
            cancelled = true;
            Toast.makeText(getActivity(), "Print cancelled", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_receipt, container, false);
        title = view.findViewById(R.id.receipt_title);
        recyclerView = view.findViewById(R.id.receipt_view);
        addButton = view.findViewById(R.id.add_button);
        printButton = view.findViewById(R.id.print_button);
        noItem = view.findViewById(R.id.no_item);
        grandTotalLayout = view.findViewById(R.id.grand_total_layout);
        grandTotalPrice = view.findViewById(R.id.grand_total_val);
        SwitchCompat discountSwitch = view.findViewById(R.id.discount_switch);
        discountSwitch.setChecked(false);
        discountFlag = false;
        cancelled = false;
        addButton.setClickable(false);

        discountSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            discountFlag = isChecked;
            resetPrice();
        });

        addButton.setOnClickListener(v -> addItemPopUp());

        printButton.setOnClickListener(v -> printPreview());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchData();
        fetchTransNo();
        fetchHeader();
        fetchCustomerData();
    }

    private void fetchCustomerData() {
        colRefCustomer.addSnapshotListener((value, error) -> {
           if (error != null || value == null) return;
           for (DocumentSnapshot documentSnapshot : value.getDocuments())
               customerList.add(documentSnapshot.getId());
        });
    }

    private void fetchData() {
        colRefStock.orderBy("item name", Query.Direction.ASCENDING).addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            stockList.clear();
            namesUnique.clear();
            sizesUnique.clear();
            for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                stockList.add(new StockModel(documentSnapshot.getString("item name"),
                        documentSnapshot.getString("item size"),
                        documentSnapshot.getDouble("qty").intValue(),
                        documentSnapshot.getDouble("reg price"),
                        documentSnapshot.getDouble("dsc price")));
                if (!namesUnique.contains(documentSnapshot.getString("item name")))
                    namesUnique.add(documentSnapshot.getString("item name"));
                if (!sizesUnique.contains(documentSnapshot.getString("item size")))
                    sizesUnique.add(documentSnapshot.getString("item size"));
            }
            stockList.sort(new stockComparator());
            namesUnique.sort(String::compareToIgnoreCase);
            sizesUnique.sort(new sizeComparator());
        });

    }

    private void fetchTransNo() {
        transactionNo = 1;
        colRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!cancelled) receiptList.clear();
                for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments())
                    transactionNo++;
                invoice = "INVOICE #" + transactionNo;
                title.setText(invoice);
                while (recyclerViewFlag) {
                    setRecyclerView();
                    recyclerViewFlag = false;
                }
                addButton.setClickable(true);
            }
        });
    }

    private void fetchHeader() {
        String cashierName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        try {
            preferences = new SecurePreferences(getContext(), "store-preferences", "store-key", true);
            preferences.put("cashier name", cashierName);
        } catch (Exception e) {
            Log.e("TAG", e.getMessage());
        }
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    header.clear();
                    header.add(documentSnapshot.getString("name"));
                    header.add(documentSnapshot.getString("address 1"));
                    header.add(documentSnapshot.getString("address 2"));
                    header.add(documentSnapshot.getString("contact"));
                    try {
                        preferences.put("name", header.get(0));
                        preferences.put("address 1", header.get(1));
                        preferences.put("address 2", header.get(2));
                        preferences.put("contact", header.get(3));
                    } catch (Exception e) {
                        Log.e("TAG", e.getMessage());
                    }
                }
            });
    }

    private void setRecyclerView() {
        adapter = new ReceiptAdapter(this, SwipeState.LEFT);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    private void addItemPopUp() {
        addPopUp  = new Dialog(getActivity());
        addPopUp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        addPopUp.setContentView(R.layout.receipt_add);
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
        String[] nameDisplay = namesUnique.toArray(new String[0]);
        setNumberPicker(itemName, nameDisplay, namesUnique);
        changeSizeValues(itemName.getValue());

        itemName.setOnValueChangedListener((np, oldPos, newPos) -> changeSizeValues(newPos));

        itemSize.setOnValueChangedListener((np, oldPos, newPos) -> {
            itemQty.setText(String.valueOf(0));
            setItemText();
        });

        plusOne.setOnClickListener(view -> checkMinMax(1));

        minusOne.setOnClickListener(view -> checkMinMax(-1));

        saveButton.setOnClickListener(v -> checkMinMax(0));

        back.setOnClickListener(v -> addPopUp.cancel());
    }

    private void printPreview() {
        Intent i = new Intent(getActivity(), PrintPreviewActivity.class);
        i.putExtra("receiptList", receiptList);
        i.putExtra("invoice", invoice);

        Dialog inputDialog = new Dialog(getActivity());
        inputDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        inputDialog.setContentView(R.layout.dialog_box_input);
        inputDialog.setCancelable(false);
        inputDialog.create();


        if (discountFlag) {
            Dialog discountDialog = new Dialog(getActivity());
            discountDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            discountDialog.setContentView(R.layout.dialog_box_input);
            discountDialog.setCancelable(false);
            discountDialog.create();
            discountDialog.show();

            TextView header = discountDialog.findViewById(R.id.header);
            header.setText(R.string.pick_customer_name);

            TextView dialogText = discountDialog.findViewById(R.id.dialog_text);
            dialogText.setText(R.string.please_pick_customer_name);

            TextInputLayout inputLayout = discountDialog.findViewById(R.id.input_layout);
            inputLayout.setVisibility(View.GONE);

            NumberPicker picker = discountDialog.findViewById(R.id.picker);
            picker.setVisibility(View.VISIBLE);
            picker.setMinValue(0);
            picker.setMaxValue(customerList.size() - 1);
            picker.setDisplayedValues(customerList.toArray(new String[0]));

            String value = customerList.get(picker.getValue());

            Button buttonCancel = discountDialog.findViewById(R.id.cancel_button);
            Button buttonOk = discountDialog.findViewById(R.id.ok_button);
            buttonCancel.setOnClickListener(v -> discountDialog.dismiss());
            buttonOk.setOnClickListener(v -> {
                i.putExtra("customer", value);
                discountDialog.dismiss();
                inputDialog.show();
            });
        } else inputDialog.show();

        TextView header = inputDialog.findViewById(R.id.header);
        header.setText(R.string.print_receipt);
        header.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_print, 0, 0, 0);

        TextView dialogText = inputDialog.findViewById(R.id.dialog_text);
        dialogText.setText(R.string.please_enter_cash_amount);

        TextInputLayout inputLayout = inputDialog.findViewById(R.id.input_layout);
        inputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);

        TextInputEditText input = inputDialog.findViewById(R.id.input);
        input.setText(String.valueOf(0));
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        Button buttonCancel = inputDialog.findViewById(R.id.cancel_button);
        Button buttonPrint = inputDialog.findViewById(R.id.ok_button);
        buttonCancel.setOnClickListener(v -> inputDialog.dismiss());

        buttonPrint.setOnClickListener(v -> {
            if (input.getText().toString().isEmpty()) {
                Toast.makeText(getActivity(), "Please enter cash amount", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Double.parseDouble(input.getText().toString()) < sum) {
                Toast.makeText(getActivity(), "Cash amount cannot be less than total amount", Toast.LENGTH_SHORT).show();
                return;
            }
            cash = Double.parseDouble(String.format(Locale.getDefault(), "%.2f", Double.parseDouble(input.getText().toString())));
            i.putExtra("cash", cash);
            inputDialog.dismiss();
            launcher.launch(i);
        });
    }

    private void setLayout(boolean show) {
        int mDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics());
        if (show) {
            grandTotalLayout.setVisibility(View.VISIBLE);
            noItem.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
            // print button layout
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) printButton.getLayoutParams();
            layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.setMargins(mDp, mDp, mDp, mDp);
            printButton.setLayoutParams(layoutParams);

        } else {
            grandTotalLayout.setVisibility(View.INVISIBLE);
            noItem.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
            // print button layout
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) printButton.getLayoutParams();
            layoutParams.height = 0;
            layoutParams.setMargins(mDp, 0, mDp, mDp);
            printButton.setLayoutParams(layoutParams);
        }
    }

    private void changeSizeValues(int pos) {
        String nameVal = namesUnique.get(pos);
        sizesUnique.clear();
        for (int i = 0; i < stockList.size(); i++)
            if (stockList.get(i).getItemName().equals(nameVal)) sizesUnique.add(stockList.get(i).getItemSize());
        String[] sizeDisplay = sizesUnique.toArray(new String[0]);
        setNumberPicker(itemSize, sizeDisplay, sizesUnique);
        itemQty.setText(String.valueOf(0));
        setItemText();
    }

    private void checkMinMax(int val) {
        if (itemQty.getText() == null || itemQty.getText().toString().isEmpty()) itemQty.setText(String.valueOf(0));
        if (Integer.parseInt(itemQty.getText().toString()) < 1 && val < 1) {
            Toast.makeText(getActivity(), "Please enter quantity.", Toast.LENGTH_SHORT).show();
            return;
        }
        checkNameSize();
        if (stockList.get(flag).getItemQuantity() == 0) {
            Toast.makeText(getActivity(), "This item is out of stock.", Toast.LENGTH_SHORT).show();
            return;
        } if (Integer.parseInt(itemQty.getText().toString()) + val > stockList.get(flag).getItemQuantity()) {
            Toast.makeText(getActivity(), "Not enough stock. Automatically set to maximum.", Toast.LENGTH_SHORT).show();
            itemQty.setText(String.valueOf(stockList.get(flag).getItemQuantity()));
            return;
        } if (Integer.parseInt(itemQty.getText().toString()) + val < 1) {
            Toast.makeText(getActivity(), "You've reached the minimum quantity.", Toast.LENGTH_SHORT).show();
            return;
        }
        editQty(val);
    }

    private void editQty(int val) {
        QtyEditor.qtyEditor(itemQty, val);
        setItemText();
        if (val != 0) return;
        for (ReceiptModel receipt : receiptList)
            if (receipt.getItemName().equals(item[0]) && receipt.getItemSize().equals(item[1])) {
                Toast.makeText(getActivity(), "Item already exists", Toast.LENGTH_SHORT).show();
                return;
            }
        receiptList.add(new ReceiptModel(item[0], item[1], Integer.parseInt(item[2]), Double.parseDouble(item[3]), Double.parseDouble(item[4])));
        setLayout(true);
        getSum();
        for (int i = 0; i < 5; i++) item[i] = "";
        adapter.setReceipts(receiptList);
        adapter.notifyItemInserted(receiptList.size());
        addPopUp.dismiss();
    }
    private void checkNameSize() {
        String iName = namesUnique.get(itemName.getValue());
        String iSize = sizesUnique.get(itemSize.getValue());
        for (int i = 0; i < stockList.size(); i++)
            if (iName.equals(stockList.get(i).getItemName()) && iSize.equals(stockList.get(i).getItemSize())) flag = i;
    }

    private void resetPrice() {
        HashMap<String, StockModel> stockMap = new HashMap<>();

        for (StockModel stock : stockList) {
            String key = stock.getItemName() + "|" + stock.getItemSize();
            stockMap.put(key, stock);
        }

        for (ReceiptModel receipt : receiptList) {
            String key = receipt.getItemName() + "|" + receipt.getItemSize();
            if (stockMap.containsKey(key)) {
                StockModel stock = stockMap.get(key);
                double unitPrice = discountFlag ? stock.getItemDscPrice() : stock.getItemRegPrice();

                receipt.setItemUnitPrice(unitPrice);
                receipt.setItemTotalPrice(receipt.getItemQuantity() * unitPrice);
                adapter.notifyItemChanged(receiptList.indexOf(receipt)); // Update UI
            }
        }

        adapter.setReceipts(receiptList);
        getSum();
    }

    private void setItemText() {
        checkNameSize();
        item[0] = stockList.get(flag).getItemName();
        item[1] = stockList.get(flag).getItemSize();
        item[2] = itemQty.getText().toString();
        if (discountFlag) item[3] = String.valueOf(stockList.get(flag).getItemDscPrice());
        else item[3] = String.valueOf(stockList.get(flag).getItemRegPrice());
        item[4] = String.valueOf(Double.parseDouble(item[2]) * Double.parseDouble(item[3]));
        String unitPriceText = "₱" + String.format(Locale.getDefault(), "%.2f", Double.parseDouble(item[3]));
        String totalPriceText = "₱" + String.format(Locale.getDefault(), "%.2f", Double.parseDouble(item[4]));
        itemUnitPrice.setText(unitPriceText);
        itemTotalPrice.setText(totalPriceText);
    }

    private void setNumberPicker(NumberPicker numberPicker, String[] array, ArrayList<String> arrayList) {
        if (arrayList == null) return;
        numberPicker.setDisplayedValues(null);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(arrayList.size() - 1);
        numberPicker.setDisplayedValues(array);
    }

    private void getSum() {
        grandTotal.clear();
        for (ReceiptModel receipt : receiptList) grandTotal.add(receipt.getItemTotalPrice());
        setSum();
    }
    private void setSum() {
        sum = 0.0;
        for (Double total : grandTotal) sum += total;
        String sumText = "₱" + String.format(Locale.getDefault(), "%.2f", sum);
        grandTotalPrice.setText(sumText);
    }

    @Override
    public void changeQty(ReceiptModel item, int position) {
        receiptList.set(position, item);
        grandTotal.set(position, item.getItemTotalPrice());
        setSum();
        adapter.setReceipts(receiptList);
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onClickRight(ReceiptModel item, int position) {
        receiptList.remove(position);
        grandTotal.remove(position);
        setSum();
        adapter.setReceipts(receiptList);
        adapter.notifyItemRemoved(position);
        setLayout(!receiptList.isEmpty());
    }

    @Override
    public void onRetainSwipe(ReceiptModel item, int position) {
        adapter.retainSwipe(item, position);
    }
}