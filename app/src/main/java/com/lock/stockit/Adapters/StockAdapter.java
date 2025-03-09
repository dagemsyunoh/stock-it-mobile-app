package com.lock.stockit.Adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lock.stockit.Helpers.StockBaseViewHolder;
import com.lock.stockit.Helpers.StockListeners;
import com.lock.stockit.Helpers.StockViewHolder;
import com.lock.stockit.Helpers.SwipeState;
import com.lock.stockit.Models.StockModel;
import com.lock.stockit.R;

import java.util.ArrayList;

public class StockAdapter extends RecyclerView.Adapter<StockBaseViewHolder> {

    private final StockListeners stockListeners;
    private final SwipeState swipeState;
    private final ArrayList<StockModel> stocksList;

    public StockAdapter(StockListeners stockListeners, SwipeState swipeState) {
        super();
        this.stockListeners = stockListeners;
        this.swipeState = swipeState;
        stocksList = new ArrayList<>();
    }

    @NonNull @Override
    public StockBaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.stock_item, parent, false);
        return new StockViewHolder(view, stockListeners);
    }

    @Override
    public void onBindViewHolder(@NonNull StockBaseViewHolder holder, int position) {
        holder.bindDataToViewHolder(stocksList.get(position), position, swipeState);
    }

    @Override
    public int getItemCount() { return stocksList.size(); }

    public void retainSwipe(StockModel model, int position) {
        // Check if swipe is enabled in the current state
        final boolean isEnabled = swipeState == SwipeState.LEFT || swipeState == SwipeState.RIGHT || swipeState == SwipeState.LEFT_RIGHT;
        // If swipe is enabled, reset the swipe state for other cells
        if (!isEnabled) return;
        for (int index = 0; index < getItemCount(); index++) {
            final boolean isNotSwiped = stocksList.get(index).getState() != SwipeState.NONE;
            if (index != position && isNotSwiped) {
                stocksList.get(index).setState(SwipeState.NONE);
                notifyItemChanged(index);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setStocks(ArrayList<StockModel> stocks) {
        stocksList.clear();
        stocksList.addAll(stocks);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setFilteredStocks(ArrayList<StockModel> filteredStocks) {
        stocksList.clear();
        stocksList.addAll(filteredStocks);
        notifyDataSetChanged();
    }
}