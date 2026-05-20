package com.sirdaba.sirdaba_delivery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.*;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SirDaba";
    private static final String DISTRIBUTOR_URL = "https://sirdaba.delivery/sirdaba-distributor/";

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private FusedLocationProviderClient locationClient;

    // File upload callback
    private ValueCallback<Uri[]> fileUploadCallback;
    private Uri cameraImageUri;

    // Permission launchers
    private final ActivityResultLauncher<String[]> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            // Re-check after permissions granted
        });

    private final ActivityResultLauncher<Intent> fileChooserLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (fileUploadCallback == null) return;
            Uri[] uris = null;
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null && data.getData() != null) {
                    // Gallery pick
                    uris = new Uri[]{data.getData()};
                } else if (cameraImageUri != null) {
                    // Camera capture (no data returned, URI was pre-set)
                    uris = new Uri[]{cameraImageUri};
                }
            }
            // Always deliver result (null = cancelled)
            fileUploadCallback.onReceiveValue(uris);
            fileUploadCallback = null;
            cameraImageUri = null;
        });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView      = findViewById(R.id.webview);
        progressBar  = findViewById(R.id.progress_bar);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        setupWebView();
        requestAppPermissions();
        fetchFcmToken();

        // Handle notification deep-link
        String notifUrl = getIntent().getStringExtra("url");
        if (notifUrl != null && !notifUrl.isEmpty()) {
            webView.loadUrl(notifUrl);
        } else {
            webView.loadUrl(DISTRIBUTOR_URL);
        }

        swipeRefresh.setOnRefreshListener(() -> webView.reload());
        swipeRefresh.setColorSchemeResources(R.color.orange_primary, R.color.green_primary);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // JavaScript & DOM storage
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // Cookies
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // Media & geolocation
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Zoom & viewport
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);

        // Cache
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // User agent — identify as SirDaba app
        String ua = settings.getUserAgentString();
        settings.setUserAgentString(ua + " SirDabaApp/1.0 Android");

        webView.setWebViewClient(new SirDabaWebViewClient());
        webView.setWebChromeClient(new SirDabaChromeClient());

        // Inject JS bridge
        webView.addJavascriptInterface(new SirDabaJSBridge(), "SirDabaAndroid");
    }

    // ──────────────────────────────────────────────────────────────
    // WebViewClient
    // ──────────────────────────────────────────────────────────────
    private class SirDabaWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            // Keep sirdaba.delivery inside the app
            if (url.contains("sirdaba.delivery")) {
                return false;
            }
            // Open external URLs in browser
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) {}
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
            swipeRefresh.setRefreshing(false);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            // Persist cookies
            CookieManager.getInstance().flush();
            // Inject FCM token into page
            injectFcmToken();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
            if (req.isForMainFrame()) {
                progressBar.setVisibility(View.GONE);
                view.loadUrl("file:///android_asset/offline.html");
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // WebChromeClient — GPS + File Upload + Camera
    // ──────────────────────────────────────────────────────────────
    private class SirDabaChromeClient extends WebChromeClient {

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                GeolocationPermissions.Callback callback) {
            // Auto-grant geolocation to our domain
            if (origin.contains("sirdaba.delivery")) {
                callback.invoke(origin, true, false);
            } else {
                callback.invoke(origin, false, false);
            }
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> fileCallback,
                FileChooserParams params) {
            if (fileUploadCallback != null) {
                fileUploadCallback.onReceiveValue(null);
            }
            fileUploadCallback = fileCallback;
            openFileChooser();
            return true;
        }

        @Override
        public void onProgressChanged(WebView view, int progress) {
            progressBar.setProgress(progress);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage cm) {
            Log.d(TAG + "/JS", cm.message());
            return true;
        }
    }

    // ──────────────────────────────────────────────────────────────
    // File chooser — Camera + Gallery
    // ──────────────────────────────────────────────────────────────
    private void openFileChooser() {
        // Camera intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraImageUri = null;
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            }
        }

        // Gallery intent
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);

        // Chooser
        Intent chooser = Intent.createChooser(galleryIntent, "اختر صورة");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
        fileChooserLauncher.launch(chooser);
    }

    private File createImageFile() {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile("SD_" + stamp + "_", ".jpg", dir);
        } catch (IOException e) {
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────────
    // JS Bridge — exposes Android features to the web page
    // ──────────────────────────────────────────────────────────────
    private class SirDabaJSBridge {

        @JavascriptInterface
        public String getFcmToken() {
            return getSharedPreferences("sirdaba", MODE_PRIVATE)
                .getString("fcm_token", "");
        }

        @JavascriptInterface
        public void onOrderReceived(String orderId, String city) {
            Log.d(TAG, "Order from JS bridge: " + orderId + " in " + city);
        }

        @JavascriptInterface
        public void requestGps() {
            getLastKnownLocation();
        }

        @JavascriptInterface
        public boolean isAndroidApp() {
            return true;
        }

        /** Called from JS after login: SirDabaAndroid.subscribeToCityTopic('agadir') */
        @JavascriptInterface
        public void subscribeToCityTopic(String cityCode) {
            FcmTopicHelper.subscribeToCity(cityCode);
        }

        /** Called from JS on logout: SirDabaAndroid.unsubscribeFromCity('agadir') */
        @JavascriptInterface
        public void unsubscribeFromCity(String cityCode) {
            FcmTopicHelper.unsubscribeFromCity(cityCode);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // FCM Token
    // ──────────────────────────────────────────────────────────────
    private void fetchFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            Log.d(TAG, "FCM Token: " + token);
            getSharedPreferences("sirdaba", MODE_PRIVATE)
                .edit().putString("fcm_token", token).apply();
        });
    }

    private void injectFcmToken() {
        String token = getSharedPreferences("sirdaba", MODE_PRIVATE)
            .getString("fcm_token", "");
        if (!token.isEmpty()) {
            dispatchFcmTokenToWebView(token);
        } else {
            // Token not cached yet — fetch fresh then inject
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(freshToken -> {
                if (freshToken != null && !freshToken.isEmpty()) {
                    getSharedPreferences("sirdaba", MODE_PRIVATE)
                        .edit().putString("fcm_token", freshToken).apply();
                    webView.post(() -> dispatchFcmTokenToWebView(freshToken));
                }
            });
        }
    }

    private void dispatchFcmTokenToWebView(String token) {
        // evaluateJavascript is more reliable than the deprecated loadUrl("javascript:...")
        String js = "(function(){"
            + "if(typeof window.onSirDabaFcmToken==='function'){"
            + "  window.onSirDabaFcmToken('" + token + "');"
            + "}"
            + "try{document.cookie='sd_fcm_token=" + token
            + ";path=/;domain=sirdaba.delivery';}catch(e){}"
            + "})();";
        webView.evaluateJavascript(js, null);
        Log.d(TAG, "FCM token injected into WebView");
    }

    // ──────────────────────────────────────────────────────────────
    // GPS
    // ──────────────────────────────────────────────────────────────
    private void getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    String js = "javascript:(function(){"
                        + "if(window.onSirDabaLocation){"
                        + "  window.onSirDabaLocation(" + location.getLatitude()
                        + "," + location.getLongitude() + ");"
                        + "}"
                        + "})()";
                    webView.post(() -> webView.loadUrl(js));
                }
            });
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Permissions
    // ──────────────────────────────────────────────────────────────
    private void requestAppPermissions() {
        String[] perms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            perms = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
        permissionLauncher.launch(perms);
    }

    // ──────────────────────────────────────────────────────────────
    // Notification tap when app is already open (singleTop)
    // ──────────────────────────────────────────────────────────────
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String url = intent.getStringExtra("url");
        if (url != null && !url.isEmpty()) {
            webView.loadUrl(url);
            Log.d(TAG, "onNewIntent: navigating to " + url);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Back button — navigate WebView history
    // ──────────────────────────────────────────────────────────────
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }
}
