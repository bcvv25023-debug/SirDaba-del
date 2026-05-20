package com.sirdaba.sirdaba_delivery;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import androidx.core.content.ContextCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

/**
 * SirDabaFirebaseService
 *
 * Receives FCM push notifications and shows them as local notifications.
 *
 * Expected FCM payload fields:
 *   title   — Notification title (e.g. "طلب توصيل جديد 🚀")
 *   body    — Notification body  (e.g. "طلب جديد في مدينة أكادير")
 *   city    — City code to filter (e.g. "agadir")
 *   order_id — Order identifier
 *   url     — Deep-link URL to open in WebView
 *   type    — "new_order" | "update" | "general"
 */
public class SirDabaFirebaseService extends FirebaseMessagingService {

    private static final String TAG = "SirDabaFCM";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        // Save token locally — MainActivity will inject it into the WebView
        getSharedPreferences("sirdaba", MODE_PRIVATE)
            .edit().putString("fcm_token", token).apply();

        // TODO: Send token to your backend
        // sendTokenToServer(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> data = message.getData();
        String title   = data.getOrDefault("title",    "SirDaba Delivery");
        String body    = data.getOrDefault("body",     "إشعار جديد");
        String type    = data.getOrDefault("type",     "general");
        String url     = data.getOrDefault("url",      "");
        String orderId = data.getOrDefault("order_id", "");
        String city    = data.getOrDefault("city",     "");

        // Also support notification payload (when app is foreground)
        if (message.getNotification() != null) {
            if (title.equals("SirDaba Delivery"))
                title = message.getNotification().getTitle();
            if (body.equals("إشعار جديد"))
                body = message.getNotification().getBody();
        }

        Log.d(TAG, "Message received | type=" + type + " city=" + city + " order=" + orderId);

        showNotification(title, body, url, type, orderId);
    }

    private void showNotification(String title, String body, String url,
                                   String type, String orderId) {

        String channelId = type.equals("new_order")
            ? SirDabaApp.CHANNEL_ORDERS
            : SirDabaApp.CHANNEL_GENERAL;

        // Build proper back stack: SplashActivity → MainActivity
        // This handles all states: foreground, background, and killed
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (!url.isEmpty())     mainIntent.putExtra("url", url);
        if (!orderId.isEmpty()) mainIntent.putExtra("order_id", orderId);

        int reqCode = new Random().nextInt(100000);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(mainIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
            reqCode,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(type.equals("new_order")
                ? NotificationCompat.PRIORITY_HIGH
                : NotificationCompat.PRIORITY_DEFAULT)
            .setColor(ContextCompat.getColor(this, R.color.orange_primary))
            .setContentIntent(pendingIntent);

        // Vibration for new orders
        if (type.equals("new_order")) {
            builder.setVibrate(new long[]{0, 400, 200, 400, 200, 400});
        }

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(reqCode, builder.build());
    }
}
