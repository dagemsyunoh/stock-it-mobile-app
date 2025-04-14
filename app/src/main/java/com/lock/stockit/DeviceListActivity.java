package com.lock.stockit;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lock.stockit.Adapters.DeviceAdapter;
import com.lock.stockit.Models.DeviceModel;

import java.util.ArrayList;
import java.util.Set;


public class DeviceListActivity extends AppCompatActivity {
    private final ArrayList<DeviceModel> deviceList = new ArrayList<>();
    private BluetoothAdapter btAdapter;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ActivityCompat.checkSelfPermission(DeviceListActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }
        if (btAdapter != null) btAdapter.cancelDiscovery();
    }

    @Override
    protected void onCreate(Bundle mSavedInstanceState) {
        super.onCreate(mSavedInstanceState);
        setContentView(R.layout.device_list);

        RecyclerView pairedView = findViewById(R.id.paired_devices);
        setDeviceList();
        DeviceAdapter deviceAdapter = new DeviceAdapter(this, deviceList, (item, position) -> {
            try {
                if (ActivityCompat.checkSelfPermission(DeviceListActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                    return;
                }
                btAdapter.cancelDiscovery();
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
        pairedView.setAdapter(deviceAdapter);
        pairedView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setDeviceList() {
        if (ActivityCompat.checkSelfPermission(DeviceListActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if (!pairedDevices.isEmpty()) for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress();
            deviceList.add(new DeviceModel(deviceName, deviceHardwareAddress));
        }
        else deviceList.add(new DeviceModel("No Devices Paired", ""));
    }
}
