package com.lock.stockit.Helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
    private boolean focus;

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
        if (item.getItemQtyType().equals("pcs")) itemQty.setInputType(InputType.TYPE_NULL);
        else itemQty.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        itemQty.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_NEXT ||
                    event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                checkMinMax(item, position, 0);
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
        itemQty.setOnFocusChangeListener((v, hasFocus) -> focus = hasFocus);

        KeyboardUtils.addKeyboardToggleListener((Activity) itemView.getContext(), isVisible -> {
            Log.d("HOLDER", "keyboard visible: " + isVisible + " position: " + position);
            if (!isVisible && focus) checkMinMax(item, position, 0);
        });
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
        }
        if (item.getItemQtyType().equals("pcs")) {
            if (Double.parseDouble(itemQty.getText().toString()) % 1 != 0) {
                Toast.makeText(cardView.getContext(), "This item cannot have decimal quantity", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Double.parseDouble(itemQty.getText().toString()) + val <= 0) {
                Toast.makeText(cardView.getContext(), "You've reached the minimum quantity.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (Double.parseDouble(itemQty.getText().toString()) + val <= 0) {
            Toast.makeText(cardView.getContext(), "You've reached the minimum quantity.", Toast.LENGTH_SHORT).show();
            double min = 0.01;
            itemQty.setText(String.valueOf(min));
            changeValues(item, position);
            return;
        }
        if (Double.parseDouble(itemQty.getText().toString()) + val > stocks.get(flag).getItemQuantity()) {
            Toast.makeText(cardView.getContext(), "Not enough stock. Automatically set to maximum.", Toast.LENGTH_SHORT).show();
            double iQty = stocks.get(flag).getItemQuantity();
            if (iQty % 1 == 0) itemQty.setText(String.valueOf((int) iQty));
            else itemQty.setText(String.valueOf(iQty));
            changeValues(item, position);
            return;
        }
        QtyEditor.qtyEditor(itemQty, val);
        changeValues(item, position);
    }

    private void changeValues (ReceiptModel item, int position) {
        item.setItemQuantity(Double.parseDouble(itemQty.getText().toString()));
        item.setItemTotalPrice(item.getItemQuantity() * item.getItemUnitPrice());
        itemTotalPrice.setText(String.valueOf(item.getItemTotalPrice()));
        getListener().changeQty(item, position);
    }
}
