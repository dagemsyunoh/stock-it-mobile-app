package com.lock.stockit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Helpers.PrinterCommands;
import com.lock.stockit.Helpers.SecurePreferences;
import com.lock.stockit.Models.ReceiptModel;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PrintPreviewActivity extends AppCompatActivity implements Runnable {

    private static int cancelCount = 0;
    private final DocumentReference storeRef = FirebaseFirestore.getInstance().collection("stores").document(LoaderActivity.sid);
    private final CollectionReference receiptRef = storeRef.collection("receipts");
    private final CollectionReference customerRef = storeRef.collection("customers");
    private final int CONNECTION_TIMEOUT = 5000;
    private final String sSeparator = "-".repeat(32);
    private final String dSeparator = "=".repeat(32);
    private final ArrayList<String> headerList = new ArrayList<>();
    private ArrayList<ReceiptModel> receiptList;
    private final DecimalFormat df = new DecimalFormat("0.00");
    private final UUID applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final ActivityResultLauncher<Intent> enableLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_CANCELED) {
            Toast.makeText(this, "Bluetooth disabled. Turn on bluetooth to print.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
    });
    private SharedPreferences sharedPreferences;
    private SecurePreferences preferences;
    private String header;
    private String body;
    private TextView printerName, printHeader, printBody, printFooter;
    private String footer;
    private AlertDialog dialog;
    private OutputStream os;
    private double cash, total;
    private String customer;
    private String invoice;
    private String dateTime;
    private String deviceAddress;
    private Button printButton;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private BluetoothDevice btDevice;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(@NonNull Message msg) {
            dialog.dismiss();
            @SuppressLint("MissingPermission") String pName = "Printer Name: " + btDevice.getName();
            printerName.setText(pName);
            Toast.makeText(PrintPreviewActivity.this, "Device Connected", Toast.LENGTH_SHORT).show();
        }
    };
    private boolean isConnected = false, permanentlyDenied;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_preview);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        permanentlyDenied = sharedPreferences.getBoolean("permanently_denied", false);
        preferences = new SecurePreferences(this, "store-preferences", "store-key", true);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTPermissions();


        printerName = findViewById(R.id.printer_name);
        printHeader = findViewById(R.id.print_header);
        printBody = findViewById(R.id.print_body);
        printFooter = findViewById(R.id.print_footer);
        Button cancelButton = findViewById(R.id.cancel_button);
        printButton = findViewById(R.id.print_button);
        receiptList = getIntent().getExtras().getParcelableArrayList("receiptList");
        invoice = getIntent().getExtras().getString("invoice");
        cash = getIntent().getExtras().getDouble("cash");
        if (getIntent().getExtras().containsKey("customer"))
            customer = getIntent().getExtras().getString("customer");

        getHeaderBodyFooter();
        setHeaderBodyFooter();

        cancelButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        printButton.setOnClickListener(v -> {
            Thread t = new Thread(() -> {
                try {
                    os = btSocket.getOutputStream();
                    printConfig(header, 1);
                    printConfig(body, 0);
                    printConfig(getFooter(), 1);
                } catch (Exception e) {
                    Log.e("TAG", "print ", e);
                }
            });
            t.start();
            saveReceipt();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent i = new Intent(PrintPreviewActivity.this, MainActivity.class);
                i.putExtra("receiptList", receiptList);
                setResult(RESULT_CANCELED,i);
                finish();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        boolean connectGranted = false, scanGranted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            connectGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
            scanGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);
        }
        if (connectGranted && scanGranted) {
            checkBTEnabled();
            return;
        }
        cancelCount++;
        checkBTPermissions();
    });

    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Dialog checkdialog = new Dialog(this);
                checkdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                checkdialog.setContentView(R.layout.dialog_box);
                checkdialog.setCancelable(false);

                TextView header = checkdialog.findViewById(R.id.header);
                header.setText(R.string.app_requires_bluetooth_permissions);
                header.setGravity(View.TEXT_ALIGNMENT_CENTER);

                if (permanentlyDenied) cancelCount = 2;

                TextView text = checkdialog.findViewById(R.id.text);
                String message = "Bluetooth permissions are required to print receipt. Please grant permissions.";
                if (cancelCount == 1)
                    message = "Bluetooth permissions were not granted. It is required to print receipt. Please grant permissions.";
                if (cancelCount > 1) {
                    message = "Bluetooth permissions were permanently denied. It is required to print receipt. Please grant permissions manually in settings.";
                    permanentlyDenied = true;
                    sharedPreferences.edit().putBoolean("permanently_denied", permanentlyDenied).apply();
                }
                text.setText(message);

                Button buttonCancel = checkdialog.findViewById(R.id.cancel_button);
                buttonCancel.setOnClickListener(v -> {
                    getOnBackPressedDispatcher().onBackPressed();
                    checkdialog.dismiss();
                });

                Button buttonOk = checkdialog.findViewById(R.id.ok_button);
                buttonOk.setText(R.string.grant_permissions);
                buttonOk.setOnClickListener(v -> {
                    permissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN});
                    checkdialog.dismiss();
                    if (cancelCount > 1) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                });
                checkdialog.show();
                return;
            }
        }
        checkBTEnabled();
    }

    private void checkBTEnabled() {
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            printButton.setClickable(false);
            return;
        }
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableLauncher.launch(enableBtIntent);
        }
        checkRecentConnection();
    }

    private void checkRecentConnection() {
        deviceAddress = sharedPreferences.getString("DeviceAddress", null);
        if (deviceAddress == null) {
            Toast.makeText(this, "No recent connection found. Please connect to printer.", Toast.LENGTH_SHORT).show();
            findDevices();
            return;
        }
        Dialog connectDialog = new Dialog(this);
        connectDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        connectDialog.setContentView(R.layout.dialog_box);
        connectDialog.setCancelable(false);

        TextView header = connectDialog.findViewById(R.id.header);
        header.setText(R.string.saved_device_found);
        header.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

        @SuppressLint("MissingPermission") String deviceName = btAdapter.getRemoteDevice(deviceAddress).getName();
        TextView text = connectDialog.findViewById(R.id.text);
        String message = "Recently connected device found. Would you like to connect to " + deviceName + "?";
        text.setText(message);

        Button buttonCancel = connectDialog.findViewById(R.id.cancel_button);
        buttonCancel.setText(R.string.no);
        buttonCancel.setOnClickListener(v -> {
            findDevices();
            connectDialog.dismiss();
        });

        Button buttonOk = connectDialog.findViewById(R.id.ok_button);
        buttonOk.setText(R.string.yes);
        buttonOk.setOnClickListener(v -> {
            connectDialog.dismiss();
            connectDevice();
        });

        if (deviceAddress != null) connectDialog.show();
        else findDevices();
    }

    @SuppressLint("MissingPermission")
    private void findDevices() {
        Toast.makeText(PrintPreviewActivity.this,"Select printer from paired devices", Toast.LENGTH_SHORT).show();
        Intent connectIntent = new Intent(PrintPreviewActivity.this, DeviceListActivity.class);
        connectLauncher.launch(connectIntent);
    }

    private void connectDevice() {
        btDevice = btAdapter.getRemoteDevice(deviceAddress);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.loading_dialog);
        dialog = builder.create();
        dialog.show();

        TextView loadingText = dialog.findViewById(R.id.loading_dialog_text);
        @SuppressLint("MissingPermission") String connect = "Connecting to " + btDevice.getName();
        loadingText.setText(connect);

        Thread mBluetoothConnectThread = new Thread(this);
        mBluetoothConnectThread.start();
        startTimeout();

    }    private final ActivityResultLauncher<Intent> connectLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_CANCELED) {
            Toast.makeText(PrintPreviewActivity.this, "Bluetooth connection cancelled", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent data = result.getData();
        if (data == null || data.getExtras() == null) return;
        Bundle mExtra = data.getExtras();
        deviceAddress = data.getStringExtra("DeviceAddress");
        connectDevice();
    });

    @SuppressLint("MissingPermission")
    public void run() {
        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(applicationUUID);
            btAdapter.cancelDiscovery();
            btSocket.connect();
            isConnected = true;
            sharedPreferences.edit().putString("DeviceName", btDevice.getName()).apply();
            sharedPreferences.edit().putString("DeviceAddress", btDevice.getAddress()).apply();
            mHandler.sendEmptyMessage(0);
        } catch (IOException eConnectException) {
            Log.e("TAG", "CouldNotConnectToSocket", eConnectException);
            closeSocket(btSocket);
        }
    }

    private void startTimeout() {
        Thread mTimeoutThread = new Thread(() -> {
            try {
                Thread.sleep(CONNECTION_TIMEOUT);
                if (!isConnected) { // If still not connected after timeout
                    runOnUiThread(() -> {
                        // Timeout occurred
                        dialog.dismiss();
                        closeSocket(btSocket);
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setCancelable(false);
                        builder.setView(R.layout.dialog_box);
                        dialog = builder.create();
                        dialog.show();

                        TextView header = dialog.findViewById(R.id.header);
                        TextView text = dialog.findViewById(R.id.text);
                        Button okButton = dialog.findViewById(R.id.ok_button);
                        Button cancelButton = dialog.findViewById(R.id.cancel_button);

                        header.setText(R.string.connection_timeout);
                        text.setText(R.string.failed_to_connect_to_printer);
                        okButton.setText(R.string.retry);
                        cancelButton.setText(R.string.cancel);

                        okButton.setOnClickListener(v -> connectDevice());
                        cancelButton.setOnClickListener(v -> findDevices());
                    });
                }
            } catch (InterruptedException e) {
                Log.e("TAG", "Timeout thread interrupted", e);
            }
        });
        mTimeoutThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        checkSocket();
    }

    private void checkSocket() {
        try {
            if (btSocket != null)
                btSocket.close();
        } catch (Exception e) {
            Log.e("TAG", "checkSocket ", e);
        }
    }

    private void saveReceipt() {
        ArrayList<String> items = new ArrayList<>();
        for (ReceiptModel val : receiptList)
            items.add(val.getItemName() + " " + val.getItemSize() + ", " + val.getItemQuantity() + " pcs");
        Map<String, Object> receiptMap = new HashMap<>();
        receiptMap.put("invoice no", invoice);
        receiptMap.put("date-time", dateTime);
        receiptMap.put("total", total);
        receiptMap.put("amount rendered cash", cash);
        receiptMap.put("items", items);
        if (customer != null || !customer.isEmpty()) {
            receiptMap.put("customer", customer);
            addCustomerReceipt();
        }
        try {
            receiptRef.add(receiptMap);
            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            Log.e("TAG", "saveReceipt " + e);
        }
    }

    private void addCustomerReceipt() {
        DocumentReference customerDocRef = customerRef.document(customer);
        customerDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) return;
            int transactions = 1;
            if (documentSnapshot.getDouble("transactions") != null)
                transactions = documentSnapshot.getDouble("transactions").intValue() + 1;
            Map<String, Object> customerMap = new HashMap<>();
            customerMap.put("transactions", transactions);
            customerMap.put("transaction #" + transactions, invoice);
            customerDocRef.update(customerMap);
        });
    }

    private void closeSocket(BluetoothSocket nOpenSocket) {
        try {
            nOpenSocket.close();
            Log.d("TAG", "SocketClosed");
        } catch (IOException ex) {
            Log.d("TAG", "CouldNotCloseSocket");
        }
    }

    protected void printConfig(String bill, int align) {
        //size 1 = large, size 2 = medium, size 3 = small
        //style 1 = Regular, style 2 = Bold
        //align 0 = left, align 1 = center, align 2 = right
        try {
            byte[] format = new byte[]{27,33, 0};
            os.write(format);
            switch (align) {
                case 0:
                    //left align
                    os.write(PrinterCommands.ESC_ALIGN_LEFT);
                    break;
                case 1:
                    //center align
                    os.write(PrinterCommands.ESC_ALIGN_CENTER);
                    break;
                case 2:
                    //right align
                    os.write(PrinterCommands.ESC_ALIGN_RIGHT);
                    break;
            }
            os.write(bill.getBytes());
            os.write(PrinterCommands.LF);
        } catch(Exception e) {
            Log.e("TAG", "printConfig " + e);
        }
    }

    private void getHeaderBodyFooter() {
        header = getHeader();
        body = getBody();
        footer = getFooter();
    }

    private String getHeader() {
        StringBuilder text = new StringBuilder();
        headerList.clear();
        headerList.add(preferences.getString("name"));
        headerList.add(preferences.getString("address 1"));
        headerList.add(preferences.getString("address 2"));
        headerList.add(preferences.getString("contact"));
        for (String s : headerList) text.append(s.toUpperCase()).append("\n");
        text.append(dSeparator);
        return text.toString();
    }

    private void setHeaderBodyFooter() {
        printHeader.setText(header);
        printHeader.setTypeface(Typeface.MONOSPACE);
        printBody.setText(body);
        printBody.setTypeface(Typeface.MONOSPACE);
        printFooter.setText(footer);
        printFooter.setTypeface(Typeface.MONOSPACE);
    }

    private String getBody() {
        StringBuilder text = new StringBuilder();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        dateTime = formatter.format(new Date());
        String cashierName = preferences.getString("cashier name").toUpperCase();
        text.append("DATE & TIME: ").append(dateTime).append("\n");
        text.append(invoice).append("\n");
        text.append("CASHIER: ").append(cashierName).append("\n");
        if (!customer.isEmpty()) text.append("CUSTOMER: ").append(customer).append("\n");
        text.append(sSeparator).append("\n");

        total = 0;
        for (ReceiptModel receipt : receiptList) {
            total += receipt.getItemTotalPrice();
            text.append(formatType(receipt.getItemName(), receipt.getItemTotalPrice())).append("\n");
            text.append(String.format(Locale.US, "  %-6s   %d @ %.2f", receipt.getItemSize(), receipt.getItemQuantity(), receipt.getItemUnitPrice())).append("\n");
        }
        double cashChange = cash - total;
        text.append("\n").append(formatType("TOTAL", total));
        text.append("\n").append("AMOUNT TENDERED");
        text.append("\n").append(formatType("CASH", cash));

        text.append("\n".repeat(2));
        text.append(formatType("TOTAL PAYMENT", total));
        text.append("\n");
        text.append(formatType("CHANGE", cashChange));

        return text.toString();
    }

    private String getFooter(){
        return dSeparator + "\n" +
                "THANK YOU!" + "\n" +
                "PLEASE VISIT AGAIN!" + "\n" +
                dSeparator + "\n".repeat(2);
    }





    private String formatType(String name, double price) {
        name = name.toUpperCase();
        int nameWidth = 22;
        int priceWidth = 9;
        int spaces = (nameWidth + priceWidth) - (name.length() + String.valueOf(price).length()) - 1;
        if (name.length() > nameWidth) name = name.substring(0, nameWidth);
        return name + " ".repeat(spaces) + df.format(price);
    }
}