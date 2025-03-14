package com.lock.stockit;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Helpers.PrinterCommands;
import com.lock.stockit.Models.ReceiptModel;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class PrintPreviewActivity extends Activity implements Runnable {
    private final CollectionReference colRef = FirebaseFirestore.getInstance().collection("format");
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private String header, body, footer;
    private ArrayList<String> headerArray;
    private ArrayList<ReceiptModel> receiptList;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    private final UUID applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Button mScan, mPrint, mDisc;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mBluetoothDevice;
    private TextView printerName, printHeader, printBody, printFooter;
    private BluetoothSocket mBluetoothSocket;
    private AlertDialog dialog;
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
    private OutputStream os;
    private String text;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_preview);

        checkForPermission();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String savedPrinterAddress = sharedPreferences.getString("bluetooth_address", null);
        //Connect directly if there's a saved address
        checkSavedDevice(savedPrinterAddress);

        printerName = findViewById(R.id.printer_name);
        printHeader = findViewById(R.id.print_header);
        printBody = findViewById(R.id.print_body);
        printFooter = findViewById(R.id.print_footer);
        Button cancelButton = findViewById(R.id.cancel_button);
        Button printButton = findViewById(R.id.print_button);
        receiptList = getIntent().getExtras().getParcelableArrayList("receiptList");
        headerArray = getIntent().getExtras().getStringArrayList("header");

        getHeaderBodyFooter();
        setHeaderBodyFooter();

        cancelButton.setOnClickListener(v -> {
            try {
                if (mBluetoothSocket != null)
                    mBluetoothSocket.close();
            } catch (Exception e) {
                Log.e("Tag", "Exe ", e);
            }
            Intent i = new Intent(this, ReceiptFragment.class);
            i.putExtra("receiptList", receiptList);
            setResult(RESULT_CANCELED,i);
            finish();
        });

        printButton.setOnClickListener(v -> {
            //size 1 = large, size 2 = medium, size 3 = small
            //style 1 = Regular, style 2 = Bold
            //align 0 = left, align 1 = center, align 2 = right
            Thread t = new Thread() {
                public void run() {
                    try {
                        os = mBluetoothSocket.getOutputStream();

                        printConfig(header, 2, 1, 1);
                        printConfig(body, 2, 1, 0);
                        printConfig(footer, 2, 1, 1);

                    } catch (Exception e) {
                        Log.e("MainActivity", "Exe ", e);
                    }
                }
            };
            t.start();
            Intent i = new Intent(this, ReceiptFragment.class);
            setResult(RESULT_OK);
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkForPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH},
                    5);
        }
        else if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.BLUETOOTH_SCAN},
                        1);
            }
        }
        else if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},
                        1);
            }
        }
    }

    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
//    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
//            new ActivityResultContracts.StartActivityForResult(),
//            result -> {
//                if (result.getResultCode() == Activity.RESULT_OK) {
//                    Intent data = result.getData();
//                }
//            });

    private void checkSavedDevice(String savedPrinterAddress) {
        checkForPermission();
        if(savedPrinterAddress != null)
        {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(savedPrinterAddress);
                // Show loading dialog
                loadDialog();
            } else {
                // Bluetooth not enabled, ask the user to turn it on.
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    public void onActivityResult(int mRequestCode, int mResultCode, Intent mDataIntent) {
        super.onActivityResult(mRequestCode, mResultCode, mDataIntent);
        checkForPermission();
        switch (mRequestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (mResultCode == Activity.RESULT_OK) {
                    Bundle mExtra = mDataIntent.getExtras();
                    String mDeviceAddress = Objects.requireNonNull(mExtra).getString("DeviceAddress");
                    Log.v("TAG", "Coming incoming address " + mDeviceAddress);
                    mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    loadDialog();
                    // pairToDevice(mBluetoothDevice); This method is replaced by
                    // progress dialog with thread
                }
                break;

            case REQUEST_ENABLE_BT:
                if (mResultCode == Activity.RESULT_OK) {
                    ListPairedDevices();
                    Intent connectIntent = new Intent(PrintPreviewActivity.this,
                            DeviceListActivity.class);
                    startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    Toast.makeText(PrintPreviewActivity.this, "Message", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void ListPairedDevices() {
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

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        try {
            if (mBluetoothSocket != null)
                mBluetoothSocket.close();
        } catch (Exception e) {
            Log.e("Tag", "Exe ", e);
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (mBluetoothSocket != null)
                mBluetoothSocket.close();
        } catch (Exception e) {
            Log.e("Tag", "Exe ", e);
        }
        setResult(RESULT_CANCELED);
        finish();
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
                        Toast.makeText(PrintPreviewActivity.this, "Connection Timeout", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (InterruptedException e) {
                // Timeout thread interrupted
                Log.e("TAG", "Timeout thread interrupted", e);
            }
        });
        mTimeoutThread.start();
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


    protected void printConfig(String bill, int size, int style, int align) {
        //size 1 = large, size 2 = medium, size 3 = small
        //style 1 = Regular, style 2 = Bold
        //align 0 = left, align 1 = center, align 2 = right

        try {

            byte[] format = new byte[]{27,33, 0};
            byte[] change = new byte[]{27,33, 0};

            os.write(format);

            //different sizes, same style Regular

            if (size==1 && style==1) {
                Log.d("TAG", "size==1 && style==1");
                change[2] = (byte) (0x10); //large
                os.write(change);
            } else if(size==2 && style==1) {
                Log.d("TAG", "size==2 && style==1");
            } else if(size==3 && style==1) {
                Log.d("TAG", "size==3 && style==1");
                change[2] = (byte) (0x3); //small
                os.write(change);
            }

            //different sizes, same style Bold
            if (size==1 && style==2) {
                Log.d("TAG", "size==1 && style==1");
                change[2] = (byte) (0x10 | 0x8); //large
                os.write(change);
            } else if(size==2 && style==2) {
                Log.d("TAG", "size==2 && style==1");
                change[2] = (byte) (0x8);
                os.write(change);
            } else if(size==3 && style==2) {
                Log.d("TAG", "size==3 && style==1");
                change[2] = (byte) (0x3 | 0x8); //small
                os.write(change);
            }


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
        printBody.setText(body);
        printFooter.setText(footer);
    }

    private void getHeaderBodyFooter() {
        header = getHeader();
        body = getBody();
        footer = getFooter();
    }

    private String getHeader() {
        StringBuilder text = new StringBuilder();
        for (String s : headerArray) text.append(s.toUpperCase()).append("\n");
        String separator = "-".repeat(32);
        text.append(separator).append("\n");
        return text.toString();
    }

    private String getBody() {
        StringBuilder text = new StringBuilder();
        for (ReceiptModel receipt : receiptList) {
            String row1 = String.format(Locale.US, "%-22s %9.2f",
                    receipt.getItemName().toUpperCase(),
                    receipt.getItemTotalPrice());

            String row2 = String.format(Locale.US, "     %-6s     %4d @ %8.2f",
                    receipt.getItemSize(),
                    receipt.getItemQuantity(),
                    receipt.getItemUnitPrice());
            text.append(row1).append("\n")
                    .append(row2).append("\n");
        }
        return text.toString();
    }

    private String getFooter(){
        String separator = "-".repeat(32); // 32-character wide separator
        String thankYouMessage = "THANK YOU!";
        String visitAgain = "Please Visit Again!";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        String dateTime = formatter.format(new Date());


        return separator + "\n" +
                thankYouMessage + "\n" +
                visitAgain + "\n" +
                separator + "\n" +
                dateTime + "\n".repeat(2);
    }
}