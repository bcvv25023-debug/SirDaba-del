# ===========================================================
# SirDaba Delivery — ProGuard / R8 Rules
# ===========================================================

# Keep app source for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── WebView JavaScript Interface ──────────────────────────
# All @JavascriptInterface methods MUST be kept or JS calls will fail
-keepclassmembers class com.sirdaba.sirdaba_delivery.MainActivity$SirDabaJSBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ── Firebase ──────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── AndroidX / Support Library ────────────────────────────
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ── App classes ───────────────────────────────────────────
-keep class com.sirdaba.sirdaba_delivery.** { *; }

# ── General Android ───────────────────────────────────────
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ── Remove logging in release ─────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
