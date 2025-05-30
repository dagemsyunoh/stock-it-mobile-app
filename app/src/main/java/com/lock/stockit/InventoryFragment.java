package com.lock.stockit;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.NotificationCompat;
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
import java.util.Map;

public class InventoryFragment extends Fragment implements StockListeners {

    protected final CollectionReference stockRef = FirebaseFirestore.getInstance().collection("stores").document(LoaderActivity.sid).collection("stocks");
    protected RecyclerView recyclerView;
    protected FloatingActionButton addButton, plusOne, minusOne;
    protected Button addItem;
    protected SearchView searchView;
    private TextView noResult;
    private ArrayList<StockModel> stockList;
    private StockAdapter adapter;
    private final ArrayList<String> names = new ArrayList<>();
    private final ArrayList<String> sizes = new ArrayList<>();
    private static final long SEARCH_DELAY = 300; // milliseconds debounce time
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    public static final double LOW_STOCK_THRESHOLD = 5.0;  // for example, less or equal to 5 triggers alert
    private static final long NOTIFY_COOLDOWN = 30 * 60 * 1000; // 30 minutes in milliseconds
    private static final String PREFS_NAME = "low_stock_notifications";
    private static final String PREFS_KEY_PREFIX = "last_notified_";
    private final Map<String, Long> notifiedTimestamps = new HashMap<>();

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
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> filterStocks(newText);
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
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
            checkLowStockItems();
        });
    }

    private void saveNotificationTimestamp(String key, long timestamp) {
        getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putLong(PREFS_KEY_PREFIX + key, timestamp).apply();
    }

    private long getNotificationTimestamp(String key) {
        return getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(PREFS_KEY_PREFIX + key, 0);
    }

    private void checkLowStockItems() {
        long now = System.currentTimeMillis();
        for (StockModel item : stockList) {
            String key = item.getItemName() + "_" + item.getItemSize();
            // Stock replenished, remove timestamp to allow future notifications
            if (item.getItemQuantity() <= LOW_STOCK_THRESHOLD) {
                long lastNotified = getNotificationTimestamp(key);
                if (lastNotified == 0 || now - lastNotified >= NOTIFY_COOLDOWN) {
                    notifyLowStock(item);
                    saveNotificationTimestamp(key, now);
                }
            } else
                requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(PREFS_KEY_PREFIX + key).apply();
        }
    }

    private void notifyLowStock(StockModel item) {
        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "stock_alert_channel";

        // Create notification channel for Android 8.0+
        NotificationChannel channel = new NotificationChannel(
                channelId,
                "Stock Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), channelId);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentTitle("Low Stock Alert");
        String quantityStr = (item.getItemQuantity() % 1 == 0) ? String.valueOf((int) item.getItemQuantity()) : String.valueOf(item.getItemQuantity());
        builder.setContentText("Item \"" + item.getItemName() + " " + item.getItemSize() + "\" is low: " + quantityStr + " " + item.getItemQtyType() + " left.");
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setAutoCancel(true);

        // Use item hashcode as notification ID to prevent duplication
        int notificationId = item.hashCode();
        Intent intent = new Intent(getActivity(), MainActivity.class);
        // Key to identify inventory tab
        intent.putExtra("open_tab", "inventory");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.setContentIntent(pendingIntent);
        notificationManager.notify(notificationId, builder.build());
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

        itemName.setOnFocusChangeListener((view, b) -> setItemName(itemName));

        itemSize.setOnFocusChangeListener((view, b) -> setItemSize(itemSize));

        plusOne.setOnClickListener(view -> QtyEditor.qtyEditor(itemQty, 1));

        minusOne.setOnClickListener(view -> {
            QtyEditor.qtyEditor(itemQty, -1);
            if (Integer.parseInt(itemQty.getText().toString()) == 1)
                Toast.makeText(getActivity(), "Quantity cannot be less than 1", Toast.LENGTH_SHORT).show();
        });

        addItem.setOnClickListener(view -> {
            setItemName(itemName);
            setItemSize(itemSize);
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

    private void setItemName(TextInputEditText itemName) {
        String input = String.valueOf(itemName.getText());
        if (input.isEmpty()) return;
        String[] words = input.split("\\s");
        StringBuilder output = new StringBuilder();
        for (String word : words)
            output.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
        itemName.setText(output.toString().trim());
    }

    private void setItemSize(TextInputEditText itemSize) {
        String input = String.valueOf(itemSize.getText());
        String output = input.replaceAll("\\s+", "").toLowerCase();
        itemSize.setText(output);
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