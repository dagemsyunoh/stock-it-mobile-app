package com.lock.stockit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lock.stockit.Adapters.DeviceAdapter;
import com.lock.stockit.Models.DeviceModel;

import java.util.ArrayList;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private final ArrayList<DeviceModel> scannedDeviceList = new ArrayList<>();
    private final ArrayList<DeviceModel> bondedDeviceList = new ArrayList<>();
    private final ArrayList<DeviceModel> activeBondedList = new ArrayList<>();

    private BluetoothAdapter btAdapter;
    private DeviceAdapter scannedAdapter;
    private DeviceAdapter bondedAdapter;

    private LinearLayout scanLayout;
    private TextView noDevicesPaired;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;

                DeviceModel model = new DeviceModel(device.getName(), device.getAddress());

                if (scannedDeviceList.contains(model)) return;

                if (bondedDeviceList.contains(model) && !activeBondedList.contains(model)) {
                    activeBondedList.add(model);
                    bondedAdapter.notifyItemInserted(activeBondedList.size() - 1);
                } else if (!bondedDeviceList.contains(model)) {
                    scannedDeviceList.add(model);
                    scannedAdapter.notifyItemInserted(scannedDeviceList.size() - 1);
                }
            }
        }
    };
    private RecyclerView pairedView, scannedView;

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btAdapter != null) btAdapter.cancelDiscovery();
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);

        TextView scanDevices = findViewById(R.id.scan_devices);
        scanLayout = findViewById(R.id.scanned_devices_layout);
        noDevicesPaired = findViewById(R.id.no_devices_paired);
        pairedView = findViewById(R.id.paired_devices);
        scannedView = findViewById(R.id.scanned_devices);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        setupAdapters();
        loadBondedDevices();
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        scanDevices.setOnClickListener(v -> {
            Log.d("TAG", "Start scanning...");
            scannedDeviceList.clear();
            scannedAdapter.notifyDataSetChanged();
            scanLayout.setVisibility(View.VISIBLE);

            if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
            btAdapter.startDiscovery();
        });
    }

    private void setupAdapters() {
        scannedAdapter = new DeviceAdapter(this, scannedDeviceList, this::onDeviceSelected);
        bondedAdapter = new DeviceAdapter(this, activeBondedList, this::onDeviceSelected);

        scannedView.setLayoutManager(new LinearLayoutManager(this));
        scannedView.setAdapter(scannedAdapter);

        pairedView.setLayoutManager(new LinearLayoutManager(this));
        pairedView.setAdapter(bondedAdapter);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    private void loadBondedDevices() {
        Set<BluetoothDevice> bondedDevices = btAdapter.getBondedDevices();
        if (!bondedDevices.isEmpty()) {
            for (BluetoothDevice device : bondedDevices) {
                bondedDeviceList.add(new DeviceModel(device.getName(), device.getAddress()));
            }
            noDevicesPaired.setVisibility(View.GONE);
        } else {
            noDevicesPaired.setVisibility(View.VISIBLE);
        }
    }

    private void onDeviceSelected(DeviceModel item, int position) {
        try {
            Log.d("TAG", "Selected: " + item.getName() + " (" + item.getAddress() + ")");
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("DeviceName", item.getName());
            editor.putString("DeviceAddress", item.getAddress());
            editor.apply();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("DeviceAddress", item.getAddress());
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        } catch (Exception ex) {
            Log.e("TAG", "Failed to select device", ex);
        }
    }
}
