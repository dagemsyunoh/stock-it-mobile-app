package com.lock.stockit.Helpers;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Models.StockModel;
import com.lock.stockit.R;

public class StockViewHolder extends StockBaseViewHolder {


    private final TextView itemName, itemSize;
    private final NumberPicker itemQty;
    private final TextInputEditText itemPrice;
    private final AppCompatImageButton saveButton;
    private final ImageView leftImage;
    private final ImageView rightImage;
    private final CardView cardView;
    CollectionReference colRef = FirebaseFirestore.getInstance().collection("stocks");

    public StockViewHolder(View itemView, StockListeners customListeners) {
        super(itemView, customListeners);
        itemName = itemView.findViewById(R.id.item_text);
        itemSize = itemView.findViewById(R.id.size_text);
        itemQty = itemView.findViewById(R.id.qty_picker);
        itemPrice = itemView.findViewById(R.id.price);
        saveButton = itemView.findViewById(R.id.save_button);
        cardView = itemView.findViewById(R.id.card_view);
        leftImage = itemView.findViewById(R.id.button_left);
        rightImage = itemView.findViewById(R.id.button_right);
    }
    @Override
    public void bindDataToViewHolder(StockModel item, int position, SwipeState swipeState) {
        //region Input Data
        itemName.setText(item.getItemName());
        itemSize.setText(item.getItemSize());

        itemQty.setEnabled(false);
        itemQty.setMinValue(0);
        itemQty.setMaxValue(1000);
        itemQty.setWrapSelectorWheel(false);
        itemQty.setValue(item.getItemQuantity());

        itemPrice.setEnabled(false);
        itemPrice.setText(String.valueOf(item.getItemPrice()));

        //endregion
        //region Swipe
        setSwipe(cardView, item.getState());
        //endregion
        setSwipeEventListener(item, position, swipeState);
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setSwipeEventListener(final StockModel item, final int position, final SwipeState swipeState) {
        //region On Click
        if (swipeState != SwipeState.NONE) {
            leftImage.setOnClickListener(view -> {
                itemQty.setEnabled(true);
                itemPrice.setEnabled(true);
                saveButton.setVisibility(View.VISIBLE);
                getListener().onClickLeft(item, position);

            });
            rightImage.setOnClickListener(view -> getListener().onClickRight(item, position));
        }
        saveButton.setOnClickListener(view -> updateData(itemQty, itemPrice));
        
        cardView.setOnClickListener(view -> {}); // Do not remove, it is required for the swipe to work
        //endregion
        //region On Touch Swipe
        if (swipeState == SwipeState.NONE) return;
        cardView.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dXLead = view.getX() - event.getRawX();
                    dXTrail = view.getRight() - event.getRawX();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    getListener().onRetainSwipe(item, position);
                    onAnimate(view, onSwipeMove(event.getRawX() + dXLead, event.getRawX() + dXTrail,swipeState), 0L);
                    item.setState(getSwipeState(event.getRawX() + dXLead, event.getRawX() + dXTrail, swipeState));
                    return false;
                case MotionEvent.ACTION_UP:
                    onAnimate(view, onSwipeUp(item.getState()), 250L);
                    return false;
                default:
                    return true;
            }
        });
    }

    private void updateData(NumberPicker number, TextInputEditText input) {
        colRef.whereEqualTo("item name", itemName.getText().toString()).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            colRef.document(task.getResult().getDocuments().get(0).getId()).update("qty", number.getValue(), "price", Double.parseDouble(input.getText().toString()));
        });

        Toast.makeText(itemView.getContext(), "Updated", Toast.LENGTH_SHORT).show();
        saveButton.setVisibility(View.GONE);
    }
}
