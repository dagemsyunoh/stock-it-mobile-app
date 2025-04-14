package com.lock.stockit.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lock.stockit.Helpers.DeviceListeners;
import com.lock.stockit.Models.DeviceModel;
import com.lock.stockit.R;

import java.util.ArrayList;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {

    private final Context context;
    private final ArrayList<DeviceModel> deviceList;
    private final DeviceListeners deviceListeners;


    public DeviceAdapter(Context context, ArrayList<DeviceModel> deviceList, DeviceListeners listener) {
        this.context = context;
        this.deviceList = deviceList;
        this.deviceListeners = listener;
    }

    @NonNull
    @Override
    public DeviceAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.device_box, parent, false);
        return new DeviceAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceAdapter.MyViewHolder holder, int position) {
        holder.textName.setText(deviceList.get(position).getName());
        holder.textAddress.setText(deviceList.get(position).getAddress());

        holder.itemView.setOnClickListener(v -> deviceListeners.selectDevice(deviceList.get(position), position));
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textAddress;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.device_name);
            textAddress = itemView.findViewById(R.id.device_address);
        }
    }
}
