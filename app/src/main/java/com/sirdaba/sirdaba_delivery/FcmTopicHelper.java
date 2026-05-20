package com.sirdaba.sirdaba_delivery;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * FcmTopicHelper
 *
 * Call subscribeToCity() after the distributor logs in successfully.
 * The WebView can call this via the JS bridge:
 *   SirDabaAndroid.subscribeToCityTopic('agadir')
 *
 * This ensures that when your backend sends a message to topic
 * "city_agadir", ALL distributors in Agadir receive it instantly.
 *
 * City codes should match what you use in your backend.
 * Examples: agadir, casablanca, marrakech, rabat, fes, tanger, etc.
 */
public class FcmTopicHelper {

    private static final String TAG = "SirDabaFCM";
    private static final String TOPIC_PREFIX = "city_";

    /**
     * Subscribe this device to receive new-order notifications for a city.
     * Safe to call multiple times — FCM deduplicates subscriptions.
     */
    public static void subscribeToCity(String cityCode) {
        if (cityCode == null || cityCode.isEmpty()) return;
        String topic = TOPIC_PREFIX + cityCode.toLowerCase().trim();
        FirebaseMessaging.getInstance()
            .subscribeToTopic(topic)
            .addOnSuccessListener(v ->
                Log.d(TAG, "✅ Subscribed to topic: " + topic))
            .addOnFailureListener(e ->
                Log.e(TAG, "❌ Failed to subscribe to " + topic + ": " + e.getMessage()));
    }

    /**
     * Unsubscribe when distributor logs out or changes city.
     */
    public static void unsubscribeFromCity(String cityCode) {
        if (cityCode == null || cityCode.isEmpty()) return;
        String topic = TOPIC_PREFIX + cityCode.toLowerCase().trim();
        FirebaseMessaging.getInstance()
            .unsubscribeFromTopic(topic)
            .addOnSuccessListener(v ->
                Log.d(TAG, "✅ Unsubscribed from topic: " + topic));
    }

    /**
     * Subscribe to all cities (admin/testing only).
     */
    public static void subscribeToAllCities() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_distributors");
    }
}
