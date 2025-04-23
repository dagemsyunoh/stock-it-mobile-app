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
    private final ArrayList<DeviceModel> scannedDeviceList = new ArrayList<>(), bondedDeviceList = new ArrayList<>();
    private BluetoothAdapter btAdapter;
    private DeviceAdapter deviceAdapter;
    private LinearLayout scanLayout;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                DeviceModel deviceModel = new DeviceModel(device.getName(), device.getAddress());
                Log.d("TAG", "onReceive: " + deviceModel.getName() + " " + deviceModel.getAddress());
                if (!scannedDeviceList.contains(deviceModel)) scannedDeviceList.add(deviceModel);
                if (scannedDeviceList.isEmpty()) scanLayout.setVisibility(View.GONE);
                setDeviceAdapter(scannedDeviceList);
                RecyclerView scannedView = findViewById(R.id.scanned_devices);
                scannedView.setAdapter(deviceAdapter);
                scannedView.setLayoutManager(new LinearLayoutManager(DeviceListActivity.this));
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

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    @Override
    protected void onCreate(Bundle mSavedInstanceState) {
        super.onCreate(mSavedInstanceState);
        setContentView(R.layout.device_list);

        RecyclerView pairedView = findViewById(R.id.paired_devices);
        TextView scanDevices = findViewById(R.id.scan_devices);
        scanLayout = findViewById(R.id.scanned_devices_layout);
        scanDevices.setOnClickListener(v -> {
            Log.d("TAG", "Scan Clicked");
            scanLayout.setVisibility(View.VISIBLE);

            if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
            btAdapter.startDiscovery();

            scannedDeviceList.clear();
            registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        });

        setDeviceList();
        setDeviceAdapter(bondedDeviceList);
        pairedView.setAdapter(deviceAdapter);
        pairedView.setLayoutManager(new LinearLayoutManager(this));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void setDeviceAdapter(ArrayList<DeviceModel> arrayList) {
        deviceAdapter = new DeviceAdapter(this, arrayList, (item, position) -> {
            try {
                btAdapter.cancelDiscovery();
                if (item.getName().equals("No Devices Paired")) return;
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

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    private void setDeviceList() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                DeviceModel deviceModel = new DeviceModel(device.getName(), device.getAddress());
                bondedDeviceList.add(deviceModel);
            }
        }
        if (bondedDeviceList.isEmpty()) bondedDeviceList.add(new DeviceModel("No Devices Paired", ""));
    }
}
