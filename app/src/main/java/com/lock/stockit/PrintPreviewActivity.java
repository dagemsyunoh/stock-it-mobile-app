package com.lock.stockit;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class PrintPreviewActivity extends AppCompatActivity implements Runnable {
    private final CollectionReference receiptRef = FirebaseFirestore.getInstance().collection("receipts");
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2,CONNECTION_TIMEOUT = 5000; // 5 seconds
    private final String sSeparator = "-".repeat(32);
    private final String dSeparator = "=".repeat(32);
    private final ArrayList<String> headerList = new ArrayList<>();
    private ArrayList<ReceiptModel> receiptList;
    private final DecimalFormat df = new DecimalFormat("0.00");
    private final UUID applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String header, body, footer, invoice, dateTime;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView printerName, printHeader, printBody, printFooter;
    private BluetoothSocket mBluetoothSocket;
    private AlertDialog dialog;
    private BluetoothDevice mBluetoothDevice;
    private OutputStream os;
    private double cash, total;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_preview);
        checkPreferences();

        printerName = findViewById(R.id.printer_name);
        printHeader = findViewById(R.id.print_header);
        printBody = findViewById(R.id.print_body);
        printFooter = findViewById(R.id.print_footer);
        Button cancelButton = findViewById(R.id.cancel_button);
        Button printButton = findViewById(R.id.print_button);
        receiptList = getIntent().getExtras().getParcelableArrayList("receiptList");
        invoice = getIntent().getExtras().getString("invoice");
        cash = getIntent().getExtras().getDouble("cash");

        getHeaderBodyFooter();
        setHeaderBodyFooter();

        cancelButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        printButton.setOnClickListener(v -> {
            Thread t = new Thread() {
                public void run() {
                    try {
                        os = mBluetoothSocket.getOutputStream();

                        printConfig(header, 1);
                        printConfig(body, 0);
                        printConfig(getFooter(), 1);

                    } catch (Exception e) {
                        Log.e("MainActivity", "Exe ", e);
                    }
                }
            };
            t.start();
            saveReceipt();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                checkSocket();
                Intent i = new Intent(PrintPreviewActivity.this, ReceiptFragment.class);
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
    }    private final ActivityResultLauncher<Intent> connectLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        checkForPermission();
        if (result.getResultCode() == RESULT_CANCELED) {
            Toast.makeText(PrintPreviewActivity.this, "Bluetooth connection cancelled", Toast.LENGTH_SHORT).show();
            getOnBackPressedDispatcher().onBackPressed();
            return;
        }
        Intent data = result.getData();
        if (data == null || data.getExtras() == null) {
            return;
        }
        Bundle mExtra = data.getExtras();
        String mDeviceAddress = Objects.requireNonNull(mExtra).getString("DeviceAddress");
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        loadDialog();
    });

    private void checkPreferences() {
        checkForPermission();
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String savedPrinterAddress = sharedPreferences.getString("bluetooth_address", null);
            checkSavedDevice(savedPrinterAddress); //connect directly if there's a saved address
        } catch (Exception e) {
            Log.e("TAG", e.getMessage());
        }
    }    private final ActivityResultLauncher<Intent> enableLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        checkForPermission();
        if (result.getResultCode() == RESULT_OK) {
            Log.d("TAG", "Bluetooth enabled");
            Toast.makeText(PrintPreviewActivity.this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            checkPreferences();
        } else {
            Log.d("TAG", "Bluetooth disabled");
            Toast.makeText(PrintPreviewActivity.this, "Bluetooth disabled. Turn on bluetooth to print.", Toast.LENGTH_SHORT).show();
            getOnBackPressedDispatcher().onBackPressed();
        }
    });

    private void saveReceipt() {
        ArrayList<String> items = new ArrayList<>();
        for (ReceiptModel val : receiptList) {
            items.add(val.getItemName() + " " + val.getItemSize() + ", " + val.getItemQuantity() + " pcs");
        }
        Map<String, Object> receiptMap = new HashMap<>();
        receiptMap.put("invoice no", invoice);
        receiptMap.put("date-time", dateTime);
        receiptMap.put("total", total);
        receiptMap.put("amount rendered cash", cash);
        receiptMap.put("items", items);
        try {
            receiptRef.add(receiptMap);
            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            Log.e("TAG", "Error Code " + e);
        }
    }

    private void checkSavedDevice(String savedPrinterAddress) {
        checkForPermission();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth not enabled, show dialog before requesting enable
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableLauncher.launch(enableBtIntent);
        } else {
            if (savedPrinterAddress != null) {
                mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(savedPrinterAddress);
                loadDialog();
                return;
            }
            listPairedDevices();
            launchDeviceList();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        checkSocket();
    }

    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            dialog.dismiss();
            checkForPermission();
            String pName = "Printer Name: " + mBluetoothDevice.getName();
            printerName.setText(pName);
            Toast.makeText(PrintPreviewActivity.this, "Device Connected", Toast.LENGTH_SHORT).show();
            return true;
        }
    });

    private void launchDeviceList() {
        Toast.makeText(PrintPreviewActivity.this,"Select printer from paired devices", Toast.LENGTH_SHORT).show();
        Intent connectIntent = new Intent(PrintPreviewActivity.this, DeviceListActivity.class);
        connectLauncher.launch(connectIntent);
    }

    private void checkForPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Log.d("TAG", "Bluetooth Permission not granted");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH}, 5);
        } else if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d("TAG", "Bluetooth Scan Permission not granted");
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_SCAN}, 1);
            }
        }
        else if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d("TAG", "Bluetooth Connect Permission not granted");
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }
        }
    }

    private void listPairedDevices() {
        checkForPermission();
        Set<BluetoothDevice> mPairedDevices = mBluetoothAdapter
                .getBondedDevices();
        if (!mPairedDevices.isEmpty()) {
            for (BluetoothDevice mDevice : mPairedDevices) {
                Log.v("TAG", "PairedDevices: " + mDevice.getName() + "  "
                        + mDevice.getAddress());
            }
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
                        closeSocket(mBluetoothSocket);
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

                        okButton.setOnClickListener(v -> checkPreferences());
                        cancelButton.setOnClickListener(v -> {
                            listPairedDevices();
                            launchDeviceList();
                        });
                    });
                }
            } catch (InterruptedException e) {
                // Timeout thread interrupted
                Log.e("TAG", "Timeout thread interrupted", e);
            }
        });
        mTimeoutThread.start();
    }

    private void checkSocket() {
        try {
            if (mBluetoothSocket != null)
                mBluetoothSocket.close();
        } catch (Exception e) {
            Log.e("Tag", "Exe ", e);
        }
    }

    private void loadDialog() {
        checkForPermission();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.loading_dialog);
        dialog = builder.create();
        dialog.show();

        TextView loadingText = dialog.findViewById(R.id.loading_dialog_text);
        String connect = "Connecting to " + mBluetoothDevice.getName();
        loadingText.setText(connect);

        Thread mBluetoothConnectThread = new Thread(this);
        mBluetoothConnectThread.start();
        startTimeout();
    }





    private void closeSocket(BluetoothSocket nOpenSocket) {
        try {
            nOpenSocket.close();
            Log.d("TAG", "SocketClosed");
        } catch (IOException ex) {
            Log.d("TAG", "CouldNotCloseSocket");
        }
    }

    public void run() {
        try {
            checkForPermission();
            mBluetoothSocket = mBluetoothDevice
                    .createRfcommSocketToServiceRecord(applicationUUID);
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothSocket.connect();
            isConnected = true;
            mHandler.sendEmptyMessage(0);
        } catch (IOException eConnectException) {
            Log.d("TAG", "CouldNotConnectToSocket", eConnectException);
            closeSocket(mBluetoothSocket);
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
        } catch(Exception ex) {
            Log.e("error", ex.toString());
        }
    }

    private void setHeaderBodyFooter() {
        printHeader.setText(header);
        printHeader.setTypeface(Typeface.MONOSPACE);
        printBody.setText(body);
        printBody.setTypeface(Typeface.MONOSPACE);
        printFooter.setText(footer);
        printFooter.setTypeface(Typeface.MONOSPACE);
    }

    private void getHeaderBodyFooter() {
        SecurePreferences preferences = new SecurePreferences(getApplicationContext(), "store-preferences", "store-key", true);
        header = getHeader(preferences);
        body = getBody(preferences);
        footer = getFooter();
    }

    private String getHeader(SecurePreferences preferences) {
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

    private String getBody(SecurePreferences preferences) {
        StringBuilder text = new StringBuilder();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        dateTime = formatter.format(new Date());
        String cashierName = preferences.getString("cashier name").toUpperCase();
        text.append("DATE & TIME: ").append(dateTime).append("\n");
        text.append(invoice).append("\n");
        text.append("CASHIER: ").append(cashierName).append("\n");
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

    private String formatType(String name, double price) {
        name = name.toUpperCase();
        int nameWidth = 22;
        int priceWidth = 9;
        int spaces = (nameWidth + priceWidth) - (name.length() + String.valueOf(price).length()) - 1;
        if (name.length() > nameWidth) name = name.substring(0, nameWidth);
        return name + " ".repeat(spaces) + df.format(price);
    }


    private String getFooter(){
        return dSeparator + "\n" +
                "THANK YOU!" + "\n" +
                "PLEASE VISIT AGAIN!" + "\n" +
                dSeparator + "\n".repeat(2);
    }
}