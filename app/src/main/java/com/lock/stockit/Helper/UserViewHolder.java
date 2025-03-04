package com.lock.stockit.Helper;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

import com.lock.stockit.Model.UserModel;
import com.lock.stockit.R;

public class UserViewHolder extends BaseViewHolder{


    private final TextView email;
    private final SwitchCompat admin, activated;
    private final ImageView rightImage;
    private final CardView cardView;

    public UserViewHolder(View itemView, UserListeners customListeners) {
        super(itemView, customListeners);
        email = itemView.findViewById(R.id.email_text);
        admin = itemView.findViewById(R.id.admin_switch);
        activated = itemView.findViewById(R.id.activated_switch);
        cardView = itemView.findViewById(R.id.card_view);
        rightImage = itemView.findViewById(R.id.button_right);
    }
    @Override
    public void bindDataToViewHolder(UserModel item, int position, SwipeState swipeState) {
        //region Input Data
        email.setText(item.getEmail());
        admin.setChecked(item.isAdmin());
        activated.setChecked(item.isActivated());
        //endregion
        //region Swipe
        setSwipe(cardView, item.getState());
        //endregion
        setSwipeEventListener(item, position, swipeState);
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setSwipeEventListener(final UserModel item, final int position, final SwipeState swipeState) {
        //region On Click
        if (swipeState != SwipeState.NONE) {
            rightImage.setOnClickListener(view -> getListener().onClickRight(item, position));
        }
        cardView.setOnClickListener(view -> { //Do not remove this need this click listener to swipe with on touch listener
            LogDebug("on Click Card");
        });
        //endregion
        //region On Touch Swipe
        if (swipeState != SwipeState.NONE) {
            cardView.setOnTouchListener((view, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dXLead = view.getX() - event.getRawX();
                        dXTrail = view.getRight() - event.getRawX();
                        LogDebug("MotionEvent.ACTION_DOWN");
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        getListener().onRetainSwipe(item, position);
                        onAnimate(view, onSwipeMove(event.getRawX() + dXLead, event.getRawX() + dXTrail,swipeState), 0L);
                        item.setState(getSwipeState(event.getRawX() + dXLead, event.getRawX() + dXTrail, swipeState));
                        LogDebug("MotionEvent.ACTION_MOVE");
                        return false;
                    case MotionEvent.ACTION_UP:
                        onAnimate(view, onSwipeUp(item.getState()), 250L);
                        LogDebug("MotionEvent.ACTION_UP");
                        return false;
                    default:
                        return true;
                }
            });
        }
    }
}
