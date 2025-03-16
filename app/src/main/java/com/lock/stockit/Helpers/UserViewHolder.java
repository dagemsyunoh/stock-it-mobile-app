package com.lock.stockit.Helpers;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lock.stockit.Models.UserModel;
import com.lock.stockit.R;

public class UserViewHolder extends UserBaseViewHolder {


    private final TextView email;
    private final SwitchCompat admin, activated;
    private final ImageView rightImage;
    private final CardView cardView;
    private final CollectionReference colRef = FirebaseFirestore.getInstance().collection("users");
    private final Logger logger = new Logger();

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
        if (swipeState != SwipeState.NONE)
            rightImage.setOnClickListener(view -> getListener().onClickRight(item, position));

        admin.setOnClickListener(view -> confirmChange("admin", admin));

        activated.setOnClickListener(view -> confirmChange("activated", activated));

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
                    onAnimate(view, onSwipeMove(event.getRawX() + dXLead, event.getRawX() + dXTrail,swipeState), 250L);
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

    private void updateData(String field, SwitchCompat switchClicked) {
        colRef.whereEqualTo("email", email.getText().toString()).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            colRef.document(task.getResult().getDocuments().get(0).getId()).update(field, switchClicked.isChecked());
        });
    }

    private void confirmChange(String field, SwitchCompat switchClicked) {
        StringBuilder message = new StringBuilder(email.getText().toString());
        if (switchClicked.isChecked()) message.append(" will be able to access certain data and functions. Are you sure you want to continue?");
        else message.append(" will be unable to access certain data and functions. Are you sure you want to continue?");
        Dialog changeDialog = new Dialog(cardView.getContext());
        changeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        changeDialog.setContentView(R.layout.change_status);
        changeDialog.setCancelable(false);
        changeDialog.create();
        changeDialog.show();
        TextView confirmText = changeDialog.findViewById(R.id.confirm_text);
        confirmText.setText(message.toString());
        Button buttonChange = changeDialog.findViewById(R.id.change_button);
        Button buttonCancel = changeDialog.findViewById(R.id.cancel_button);

        buttonCancel.setOnClickListener(v -> {
            switchClicked.setChecked(!switchClicked.isChecked());
            changeDialog.dismiss();
        });

        buttonChange.setOnClickListener(v -> {
            updateData(field, switchClicked);
            logger.setUserLog(field + " set to " + switchClicked.isChecked(),
                    email.getText().toString(),
                    FirebaseAuth.getInstance().getCurrentUser().getEmail());
            changeDialog.dismiss();
        });
    }
}
