package com.sirdaba.sirdaba_delivery;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class SirDabaApp extends Application {

    public static final String CHANNEL_ORDERS   = "sirdaba_orders";
    public static final String CHANNEL_GENERAL  = "sirdaba_general";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            // Orders channel — HIGH importance for delivery alerts
            NotificationChannel orders = new NotificationChannel(
                CHANNEL_ORDERS,
                "طلبات التوصيل",
                NotificationManager.IMPORTANCE_HIGH
            );
            orders.setDescription("إشعارات طلبات التوصيل الجديدة في مدينتك");
            orders.enableVibration(true);
            orders.setShowBadge(true);
            nm.createNotificationChannel(orders);

            // General channel
            NotificationChannel general = new NotificationChannel(
                CHANNEL_GENERAL,
                "إشعارات عامة",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            general.setDescription("تحديثات وإشعارات عامة من SirDaba");
            nm.createNotificationChannel(general);
        }
    }
}
