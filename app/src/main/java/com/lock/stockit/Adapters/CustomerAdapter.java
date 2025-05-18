package com.lock.stockit.Adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lock.stockit.Helpers.CustomerBaseViewHolder;
import com.lock.stockit.Helpers.CustomerListeners;
import com.lock.stockit.Helpers.CustomerViewHolder;
import com.lock.stockit.Helpers.SwipeState;
import com.lock.stockit.Models.CustomerModel;
import com.lock.stockit.R;

import java.util.ArrayList;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerBaseViewHolder> {

    private final CustomerListeners customerListeners;
    private final SwipeState swipeState;
    private final ArrayList<CustomerModel> customersList;

    public CustomerAdapter(CustomerListeners customerListeners, SwipeState swipeState) {
        super();
        this.customerListeners = customerListeners;
        this.swipeState = swipeState;
        customersList = new ArrayList<>();
    }

    @NonNull
    @Override
    public CustomerBaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.customer_item, parent, false);
        return new CustomerViewHolder(view, customerListeners);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerBaseViewHolder holder, int position) {
        holder.bindDataToViewHolder(customersList.get(position), position, swipeState);
    }

    @Override
    public int getItemCount() {
        return customersList.size();
    }

    public void retainSwipe(CustomerModel model, int position) {
        // Check if swipe is enabled in the current state
        final boolean isEnabled = (swipeState == SwipeState.LEFT || swipeState == SwipeState.RIGHT || swipeState == SwipeState.LEFT_RIGHT);
        // If swipe is enabled, reset the swipe state for other cells
        if (!isEnabled) return;
        for (int index = 0; index < getItemCount(); index++) {
            final boolean isNotSwiped = customersList.get(index).getState() != SwipeState.NONE;
            if (index != position && isNotSwiped) {
                customersList.get(index).setState(SwipeState.NONE);
                notifyItemChanged(index);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setCustomers(ArrayList<CustomerModel> customers) {
        customersList.clear();
        customersList.addAll(customers);
        notifyDataSetChanged();
    }
}