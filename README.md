# SirDaba Delivery — تطبيق الموزعين (Android)

تطبيق WebView احترافي لموزعي SirDaba Delivery مع دعم كامل للإشعارات الفورية وGPS والكاميرا.

---

## 🏗️ هيكل المشروع

```
SirDabaDelivery/
├── app/
│   ├── build.gradle                    ← dependencies
│   ├── google-services.json            ← ⚠️ استبدل بملف Firebase الحقيقي
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── offline.html            ← صفحة بدون إنترنت
│       ├── java/com/sirdaba/delivery/
│       │   ├── SirDabaApp.java         ← Application class + Notification Channels
│       │   ├── SplashActivity.java     ← Splash screen مع أنيماشن
│       │   ├── MainActivity.java       ← WebView رئيسي + GPS + Camera + JS Bridge
│       │   ├── SirDabaFirebaseService.java ← FCM push notifications
│       │   └── FcmTopicHelper.java     ← City topic subscriptions
│       └── res/
│           ├── layout/
│           │   ├── activity_splash.xml
│           │   └── activity_main.xml
│           ├── drawable/
│           │   ├── splash_background.xml
│           │   ├── glow_circle.xml
│           │   ├── ic_notification.xml
│           │   └── ic_launcher_foreground.xml
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/
│               ├── file_provider_paths.xml
│               └── network_security_config.xml
```

---

## 🚀 خطوات الإعداد

### 1. Firebase (إلزامي للإشعارات)

1. اذهب إلى [console.firebase.google.com](https://console.firebase.google.com)
2. أنشئ مشروعاً جديداً باسم `sirdaba-delivery`
3. أضف تطبيق Android بـ package name: `com.sirdaba.delivery`
4. حمّل `google-services.json` واستبدل الملف الموجود في `app/`
5. فعّل **Cloud Messaging API v1** من إعدادات المشروع

### 2. شعار التطبيق (Logo)

- ضع صورة `logo_sirdaba.png` في `res/drawable/`  
  — الحجم المناسب: **480×200px** خلفية شفافة (PNG)
- ضع صورة `text_tagline.png` في `res/drawable/`  
  — نص "تطبيق الموزعين" بالعربية على خلفية شفافة

### 3. خط Cairo (اختياري لكن موصى به)

أضف ملف `cairo.ttf` في `res/font/cairo.ttf`، أو احذف مرجع `@font/cairo` من splash layout.

### 4. Build & Install

```bash
# في Android Studio: Build → Generate Signed APK
# أو عبر command line:
./gradlew assembleRelease
```

---

## 📲 تكامل الموقع مع التطبيق

### اكتشاف التطبيق من JavaScript
```javascript
if (window.SirDabaAndroid && SirDabaAndroid.isAndroidApp()) {
    // نحن داخل التطبيق
    const token = SirDabaAndroid.getFcmToken();
    // أرسل token إلى الخادم مع معرف الموزع
}
```

### الاشتراك في إشعارات المدينة (بعد تسجيل الدخول)
```javascript
// بعد تسجيل دخول الموزع ومعرفة مدينته:
if (window.SirDabaAndroid) {
    SirDabaAndroid.subscribeToCityTopic('agadir'); // أو 'casablanca' إلخ
}
```

### استقبال موقع GPS
```javascript
window.onSirDabaLocation = function(lat, lng) {
    console.log('موقع الموزع:', lat, lng);
    // أرسل للخادم لتحديث موقع الموزع
};
// طلب الموقع:
if (window.SirDabaAndroid) SirDabaAndroid.requestGps();
```

### استقبال FCM Token
```javascript
window.onSirDabaFcmToken = function(token) {
    // احفظ token في قاعدة البيانات مرتبطاً بالموزع
    fetch('/api/distributor/update-token', {
        method: 'POST',
        body: JSON.stringify({ fcm_token: token })
    });
};
```

---

## 🔔 إرسال إشعار لموزعي مدينة معينة (Backend)

```php
// PHP - إرسال لجميع موزعي مدينة أكادير
function sendOrderNotificationToCity($cityCode, $orderId, $orderDetails) {
    $serverKey = 'YOUR_FIREBASE_SERVER_KEY';
    $topic = 'city_' . strtolower($cityCode); // city_agadir

    $payload = [
        'message' => [
            'topic' => $topic,
            'data' => [
                'type'     => 'new_order',
                'order_id' => (string) $orderId,
                'city'     => $cityCode,
                'title'    => 'طلب توصيل جديد 🚀',
                'body'     => 'طلب جديد في ' . $orderDetails['city'] . ' — اضغط لقبوله',
                'url'      => 'https://sirdaba.delivery/sirdaba-distributor/?order=' . $orderId,
            ],
            'android' => [
                'priority' => 'high',
                'notification' => ['channel_id' => 'sirdaba_orders']
            ]
        ]
    ];

    // استخدم Google Auth Library للحصول على access token
    // أو استخدم Firebase Admin SDK
}
```

---

## ✅ الميزات المدمجة

| الميزة | الوصف |
|---|---|
| 🌐 WebView | يفتح `https://sirdaba.delivery/sirdaba-distributor/` |
| 🍪 Cookies | محفوظة بين الجلسات |
| 🔔 Push Notifications | FCM مع قنوات منفصلة للطلبات |
| 📍 GPS | يُحقن في الصفحة عبر JS bridge |
| 📷 Camera + Gallery | chooser كامل مع FileProvider |
| 🔄 SwipeRefresh | سحب للأسفل لتحديث الصفحة |
| 📵 Offline Page | صفحة HTML جميلة عند انقطاع الإنترنت |
| 🎨 Splash Screen | شاشة دخول مع أنيماشن 2.2 ثانية |
| 🏙️ City Topics | كل موزع يشترك في topic مدينته |
| 🔙 Back Navigation | زر الرجوع يتنقل في تاريخ WebView |
