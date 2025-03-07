package com.lock.stockit.Helpers;

import android.content.Context;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.recyclerview.widget.RecyclerView;

import com.lock.stockit.Models.UserModel;

abstract public class UserBaseViewHolder extends RecyclerView.ViewHolder {


    /** Main */
    private final UserListeners userListeners;
    /** On Swipe */
    private final WindowManager windowManager;
    private final Float cardViewLeading;
    private final Float cardViewLeadEdge;
    private final Float cardViewTrailEdge;
    private final Float cardViewTrailing;
    private final int width;
    protected Float dXLead = (float) 0;
    protected Float dXTrail = (float) 0;
    public UserBaseViewHolder(View itemView, UserListeners userListeners) {
        super(itemView);
        this.userListeners = userListeners;
        windowManager = (WindowManager) itemView.getContext().getSystemService(Context.WINDOW_SERVICE);
        width = getWidth();
        cardViewLeading = (float) width * 0.10f; //leading
        cardViewLeadEdge = (float) width * 0.25f; //leading_rubber
        cardViewTrailEdge = (float) width * 0.75f; //trailing_rubber
        cardViewTrailing = (float) width * 0.90f; //trailing
    }

    public static void LogDebug(String message) {
        Log.d("TAG",message);
    }

    public static void LogError(String message) {
        Log.e("TAG",message);
    }

    protected UserListeners getListener() {
        return userListeners;
    }

    private int getWidth() {
        LogDebug("getWidth " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            LogDebug("getWidth >= R");
            WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();
            WindowInsets windowInsets = windowMetrics.getWindowInsets();
            Insets insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars() | WindowInsets.Type.displayCutout());
            int insetsWidth = insets.right + insets.left;
            //int insetsHeight = insets.top + insets.bottom;
            Rect bounds = windowMetrics.getBounds();
            //int height  = bounds.height() - insetsHeight;
            return bounds.width() - insetsWidth; //Int width = bounds.width() - insetsWidth;
        } else {
            LogDebug("getWidth < R");
            Point size = new Point();
            Display display = windowManager.getDefaultDisplay(); //activity.getWindowManager().getDefaultDisplay(); // deprecated in API 30
            display.getSize(size); // deprecated in API 30
            //int height = size.y;
            return size.x; //Int width = size.x;
        }
    }

    protected void setSwipe(View view, SwipeState swipeState) {
        onAnimate(view, onSwipeUp(swipeState), 0L);
    }

    protected void onAnimate(View view, Float dx, Long duration) {
        view.animate().x(dx).setDuration(duration).start();
    }

    protected Float onSwipeMove(Float currentLead, Float currentTrail, SwipeState swipeState) {
        LogDebug("onSwipeMove($currentLead, $currentTrail, $swipeState)");
        if (swipeState == SwipeState.LEFT || swipeState == SwipeState.RIGHT || swipeState == SwipeState.LEFT_RIGHT) {
            return currentLead;
        } else return cardViewLeading;
    }

    protected SwipeState getSwipeState(Float currentLead, Float currentTrail, SwipeState swipeState) {
        LogDebug("getSwipeState($currentLead, $currentTrail, $swipeState)");
        if (swipeState == SwipeState.LEFT && currentLead < cardViewLeading && currentTrail < cardViewTrailEdge) {
            LogDebug("SwipeState.LEFT");
            return SwipeState.LEFT;
        } else if (swipeState == SwipeState.RIGHT && currentLead > cardViewLeadEdge && currentTrail > cardViewTrailing) {
            LogDebug("SwipeState.RIGHT");
            return SwipeState.RIGHT;
        } else if (swipeState == SwipeState.LEFT_RIGHT && currentLead < cardViewLeading && currentTrail < cardViewTrailEdge) {
            LogDebug("SwipeState.LEFT");
            return SwipeState.LEFT;
        } else if (swipeState == SwipeState.LEFT_RIGHT && currentLead > cardViewLeadEdge && currentTrail > cardViewTrailing) {
            LogDebug("SwipeState.RIGHT");
            return SwipeState.RIGHT;
        } else {
            LogDebug("SwipeState.NONE");
            return SwipeState.NONE;
        }
    }

    protected Float onSwipeUp(SwipeState swipeState) {
        LogDebug("onSwipeUp($swipeState)");
        LogDebug("$cardViewLeading $cardViewLeadEdge $cardViewTrailEdge $cardViewTrailing - ${size.x.toFloat()}");
        if (swipeState == SwipeState.NONE) return cardViewLeading;
        else if (swipeState == SwipeState.LEFT) return ((float) width * -0.05f);
        else if (swipeState == SwipeState.RIGHT) return cardViewLeadEdge;
        else return cardViewLeading;
    }

    public abstract void bindDataToViewHolder(UserModel item, int position, SwipeState swipeState);
}