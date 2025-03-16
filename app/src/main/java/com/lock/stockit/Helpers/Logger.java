package com.lock.stockit.Helpers;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Logger {
    public void setUserLog(String action, String target, String user) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dateTime = formatter.format(new Date());
        Map<String, Object> log = new HashMap<>();
        log.put("action", action);
        log.put("target", target);
        log.put("user", user);
        log.put("date-time", dateTime);
        FirebaseFirestore.getInstance().collection("user log").document().set(log);
    }
}
