package com.lock.stockit.Helpers;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Models.ReceiptModel;
import com.lock.stockit.R;

import java.util.Locale;

public class ReceiptViewHolder extends ReceiptBaseViewHolder {


    private final TextView itemName, itemSize, itemUnitPrice, itemTotalPrice;
    private final TextInputEditText itemQty;
    private final FloatingActionButton plusOne, minusOne;
    private final ImageView rightImage;
    private final CardView cardView;
    private final CollectionReference colRef = FirebaseFirestore.getInstance().collection("receipts");

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

        plusOne.setOnClickListener(view -> {
            QtyMover.onPlusOne(itemQty);
            itemTotalPrice.setText(String.valueOf(Double.parseDouble(String.valueOf(itemQty.getText())) * item.getItemUnitPrice()));
        });

        minusOne.setOnClickListener(view -> {
            QtyMover.onMinusOne(itemQty);
            if (Integer.parseInt(itemQty.getText().toString()) == 1) {
                Toast.makeText(cardView.getContext(), "Quantity cannot be less than 1. Please delete the item instead.", Toast.LENGTH_SHORT).show();
                return;
            }
            itemTotalPrice.setText(String.valueOf(Double.parseDouble(String.valueOf(itemQty.getText())) * item.getItemUnitPrice()));
        });

        cardView.setOnClickListener(view -> { }); // Do not remove, it is required for the swipe to work
        //endregion
        //region On Touch Swipe
        if (swipeState == SwipeState.NONE) return;
        cardView.setOnTouchListener((view, event) -> {
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
}
