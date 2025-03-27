package com.lock.stockit.Helpers;

import android.annotation.SuppressLint;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lock.stockit.Models.ReceiptModel;
import com.lock.stockit.Models.StockModel;
import com.lock.stockit.R;
import com.lock.stockit.ReceiptFragment;

import java.util.ArrayList;
import java.util.Locale;

public class ReceiptViewHolder extends ReceiptBaseViewHolder {


    private final TextView itemName, itemSize, itemUnitPrice, itemTotalPrice;
    private final TextInputEditText itemQty;
    private final FloatingActionButton plusOne, minusOne;
    private final ImageView rightImage;
    private final CardView cardView;

    public ReceiptViewHolder(View itemView, ReceiptListeners customListeners) {
        super(itemView, customListeners);
        itemName = itemView.findViewById(R.id.item_text);
        itemSize = itemView.findViewById(R.id.size_text);
        itemQty = itemView.findViewById(R.id.qty);
        itemUnitPrice = itemView.findViewById(R.id.unit_price_val_text);
        itemTotalPrice = itemView.findViewById(R.id.total_price_val_text);
        plusOne = itemView.findViewById(R.id.plus_one);
        minusOne = itemView.findViewById(R.id.minus_one);
        cardView = itemView.findViewById(R.id.card_view);
        rightImage = itemView.findViewById(R.id.button_right);
    }

    @Override
    public void bindDataToViewHolder(ReceiptModel item, int position, SwipeState swipeState) {
        //region Input Data
        String qtyText = String.valueOf(item.getItemQuantity());
        String unitPriceText = "₱" + String.format(Locale.getDefault(), "%.2f", item.getItemUnitPrice());
        String totalPriceText = "₱" + String.format(Locale.getDefault(), "%.2f", item.getItemTotalPrice());
        itemName.setText(item.getItemName());
        itemSize.setText(item.getItemSize());
        itemQty.setText(qtyText);
        itemUnitPrice.setText(unitPriceText);
        itemTotalPrice.setText(totalPriceText);
        itemQty.setFocusable(false);
        itemQty.setClickable(true);
        itemQty.setInputType(InputType.TYPE_NULL);
        //endregion
        //region Swipe
        setSwipe(cardView, item.getState());
        //endregion
        setSwipeEventListener(item, position, swipeState);
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setSwipeEventListener(final ReceiptModel item, final int position, final SwipeState swipeState) {
        //region On Click
        if (swipeState != SwipeState.NONE) {
            rightImage.setOnClickListener(view -> getListener().onClickRight(item, position));
        }

        plusOne.setOnClickListener(view -> checkMinMax(item, position, 1));

        minusOne.setOnClickListener(view -> checkMinMax(item, position, -1));

        cardView.setOnClickListener(view -> { }); // Do not remove, it is required for the swipe to work
        //endregion
        //region On Touch Swipe
        if (swipeState == SwipeState.NONE) return;
        cardView.setOnTouchListener((view, event) -> {
            view.performClick();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    dXLead = view.getX() - event.getRawX();
                    dXTrail = view.getRight() - event.getRawX();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    getListener().onRetainSwipe(item, position);
                    onAnimate(view, onSwipeMove(event.getRawX() + dXLead, event.getRawX() + dXTrail,swipeState), 250L);
                    item.setState(getSwipeState(event.getRawX() + dXLead, event.getRawX() + dXTrail, swipeState));
                    return false;
                case MotionEvent.ACTION_UP:
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    onAnimate(view, onSwipeUp(item.getState()), 250L);
                    return false;
                case MotionEvent.ACTION_CANCEL:
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    return false;
                default:
                    return true;
            }
        });
        //endregion
    }

    private void checkMinMax(ReceiptModel item, int position,int val) {
        if (itemQty.getText() == null || itemQty.getText().toString().isEmpty()) itemQty.setText(String.valueOf(0));
        int flag = 0;
        String iName = itemName.getText().toString();
        String iSize = itemSize.getText().toString();
        ArrayList<StockModel> stocks = ReceiptFragment.stockList;
        for (int i = 0; i < stocks.size(); i++)
            if (iName.equals(stocks.get(i).getItemName()) && iSize.equals(stocks.get(i).getItemSize())) flag = i;
        if (stocks.get(flag).getItemQuantity() == 0) {
            Toast.makeText(cardView.getContext(), "This item is out of stock.", Toast.LENGTH_SHORT).show();
            return;
        } if (Integer.parseInt(itemQty.getText().toString()) + val > stocks.get(flag).getItemQuantity()) {
            Toast.makeText(cardView.getContext(), "You've reached the maximum quantity.", Toast.LENGTH_SHORT).show();
            return;
        } if (Integer.parseInt(itemQty.getText().toString()) + val < 1) {
            Toast.makeText(cardView.getContext(), "You've reached the minimum quantity.", Toast.LENGTH_SHORT).show();
            return;
        }
        QtyEditor.qtyEditor(itemQty, val);
        changeValues(item, position);
    }

    private void changeValues (ReceiptModel item, int position) {
        item.setItemQuantity(Integer.parseInt(itemQty.getText().toString()));
        item.setItemTotalPrice(item.getItemQuantity() * item.getItemUnitPrice());
        itemTotalPrice.setText(String.valueOf(item.getItemTotalPrice()));
        getListener().changeQty(item, position);
    }
}
