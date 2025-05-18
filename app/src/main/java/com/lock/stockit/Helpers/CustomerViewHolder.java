package com.lock.stockit.Helpers;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Models.CustomerModel;
import com.lock.stockit.R;

public class CustomerViewHolder extends CustomerBaseViewHolder {

    private final CollectionReference customerRef = FirebaseFirestore.getInstance().collection("stores").document(new SecurePreferences(itemView.getContext(), "store-preferences", "store-key", true).getString("sid")).collection("customers");
    private final TextView name, transactions;
    private final ImageView rightImage;
    private final CardView cardView;
    private final Logger logger = new Logger();

    public CustomerViewHolder(View itemView, CustomerListeners customListeners) {
        super(itemView, customListeners);
        name = itemView.findViewById(R.id.name_text);
        transactions = itemView.findViewById(R.id.transactions_text);
        cardView = itemView.findViewById(R.id.card_view);
        rightImage = itemView.findViewById(R.id.button_right);
    }

    @Override
    public void bindDataToViewHolder(CustomerModel item, int position, SwipeState swipeState) {
        //region Input Data
        name.setText(item.getName());
        transactions.setText(String.valueOf(item.getTransactions()));
        //endregion
        //region Swipe
        setSwipe(cardView, item.getState());
        //endregion
        setSwipeEventListener(item, position, swipeState);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setSwipeEventListener(final CustomerModel item, final int position, final SwipeState swipeState) {
        //region On Click
        if (swipeState != SwipeState.NONE)
            rightImage.setOnClickListener(view -> getListener().onClickRight(item, position));

        cardView.setOnClickListener(view -> Log.d("TAG", "click")); // Do not remove, it is required for the swipe to work
        //endregion
        //region On Touch Swipe
        if (swipeState == SwipeState.NONE) return;

        cardView.setOnTouchListener((view, event) -> {
            view.performClick();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dXLead = view.getX() - event.getRawX();
                    dXTrail = view.getRight() - event.getRawX();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    getListener().onRetainSwipe(item, position);
                    onAnimate(view, onSwipeMove(event.getRawX() + dXLead, event.getRawX() + dXTrail, swipeState), 250L);
                    item.setState(getSwipeState(event.getRawX() + dXLead, event.getRawX() + dXTrail, swipeState));
                    return false;
                case MotionEvent.ACTION_UP:
                    onAnimate(view, onSwipeUp(item.getState()), 250L);
                    return false;
                case MotionEvent.ACTION_CANCEL:
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    return false;
                default:
                    return true;
            }
        });
    }
}
