package com.lock.stockit.Adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lock.stockit.Helpers.ReceiptBaseViewHolder;
import com.lock.stockit.Helpers.ReceiptListeners;
import com.lock.stockit.Helpers.ReceiptViewHolder;
import com.lock.stockit.Helpers.SwipeState;
import com.lock.stockit.Models.ReceiptModel;
import com.lock.stockit.R;

import java.util.ArrayList;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptBaseViewHolder> {

    private final ReceiptListeners receiptListeners;
    private final SwipeState swipeState;
    private final ArrayList<ReceiptModel> receiptsList;

    public ReceiptAdapter(ReceiptListeners receiptListeners, SwipeState swipeState) {
        super();
        this.receiptListeners = receiptListeners;
        this.swipeState = swipeState;
        receiptsList = new ArrayList<>();
    }

    @NonNull @Override
    public ReceiptBaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.receipt_item, parent, false);
        return new ReceiptViewHolder(view, receiptListeners);
    }

    @Override
    public void onBindViewHolder(@NonNull ReceiptBaseViewHolder holder, int position) {
        holder.bindDataToViewHolder(receiptsList.get(position), position, swipeState);
    }

    @Override
    public int getItemCount() { return receiptsList.size(); }

    public void retainSwipe(ReceiptModel model, int position) {
        // Check if swipe is enabled in the current state
        final boolean isEnabled = swipeState == SwipeState.LEFT || swipeState == SwipeState.RIGHT || swipeState == SwipeState.LEFT_RIGHT;
        // If swipe is enabled, reset the swipe state for other cells
        if (!isEnabled) return;
        for (int index = 0; index < getItemCount(); index++) {
            final boolean isNotSwiped = receiptsList.get(index).getState() != SwipeState.NONE;
            if (index != position && isNotSwiped) {
                receiptsList.get(index).setState(SwipeState.NONE);
                notifyItemChanged(index);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setReceipts(ArrayList<ReceiptModel> receipts) {
        receiptsList.clear();
        receiptsList.addAll(receipts);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setFilteredReceipts(ArrayList<ReceiptModel> filteredReceipts) {
        receiptsList.clear();
        receiptsList.addAll(filteredReceipts);
        notifyDataSetChanged();
    }
}