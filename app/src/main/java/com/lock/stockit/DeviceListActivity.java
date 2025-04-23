package com.lock.stockit;

import android.Manifest;
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
    private final ArrayList<DeviceModel> scannedDeviceList = new ArrayList<>(), bondedDeviceList = new ArrayList<>(), activeBondedList = new ArrayList<>();
    private BluetoothAdapter btAdapter;
    private DeviceAdapter deviceAdapter;
    private LinearLayout scanLayout;
    private TextView noDevicesPaired;
    private RecyclerView pairedView;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                DeviceModel deviceModel = new DeviceModel(device.getName(), device.getAddress());
                Log.d("TAG", "onReceive: " + deviceModel.getName() + " " + deviceModel.getAddress());
                if (scannedDeviceList.contains(deviceModel)) return;
                if (bondedDeviceList.contains(deviceModel) && !activeBondedList.contains(deviceModel)) activeBondedList.add(deviceModel);
                if (!bondedDeviceList.contains(deviceModel)) scannedDeviceList.add(deviceModel);
                setDeviceAdapter(activeBondedList);
                pairedView.setLayoutManager(new LinearLayoutManager(DeviceListActivity.this));
                pairedView.setAdapter(deviceAdapter);
                deviceAdapter.notifyItemInserted(activeBondedList.size()-1);
            }
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btAdapter != null) btAdapter.cancelDiscovery();
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    protected void onCreate(Bundle mSavedInstanceState) {
        super.onCreate(mSavedInstanceState);
        setContentView(R.layout.device_list);

        TextView scanDevices = findViewById(R.id.scan_devices);
        scanLayout = findViewById(R.id.scanned_devices_layout);
        noDevicesPaired = findViewById(R.id.no_devices_paired);
        pairedView = findViewById(R.id.paired_devices);
        RecyclerView scannedView = findViewById(R.id.scanned_devices);

        setupBTScan();

        scanDevices.setOnClickListener(v -> {
            Log.d("TAG", "onCreate: Scanning");
            if (btAdapter.isDiscovering()) {
                btAdapter.cancelDiscovery();
                btAdapter.startDiscovery();
                registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }

            if (scannedDeviceList.isEmpty()) return;
            scanLayout.setVisibility(View.VISIBLE);
            setDeviceAdapter(scannedDeviceList);
            scannedView.setLayoutManager(new LinearLayoutManager(DeviceListActivity.this));
            scannedView.setAdapter(deviceAdapter);
            deviceAdapter.notifyItemInserted(scannedDeviceList.size()-1);
        });
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    private void setupBTScan() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.startDiscovery();
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        setDeviceList();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void setDeviceAdapter(ArrayList<DeviceModel> arrayList) {
        deviceAdapter = new DeviceAdapter(this, arrayList, (item, position) -> {
            try {
                String deviceName = item.getName();
                String deviceAddress = item.getAddress();

                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("DeviceName", deviceName);
                editor.putString("DeviceAddress", deviceAddress);
                editor.apply();

                Bundle mBundle = new Bundle();
                mBundle.putString("DeviceAddress", deviceAddress);
                Intent mBackIntent = new Intent();
                mBackIntent.putExtras(mBundle);
                setResult(Activity.RESULT_OK, mBackIntent);
                finish();
            } catch (Exception ex) {
                Log.e("TAG", "Exception Code: ", ex);
            }
        });
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    private void setDeviceList() {
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (!pairedDevices.isEmpty()) for (BluetoothDevice device : pairedDevices)
            bondedDeviceList.add(new DeviceModel(device.getName(), device.getAddress()));

        if (bondedDeviceList.isEmpty()) noDevicesPaired.setVisibility(View.VISIBLE);
        else noDevicesPaired.setVisibility(View.GONE);
    }
}
