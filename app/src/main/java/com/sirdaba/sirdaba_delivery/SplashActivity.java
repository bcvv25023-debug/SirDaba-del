package com.sirdaba.sirdaba_delivery;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        ImageView tagline = findViewById(R.id.splash_tagline);

        // Scale + fade in animation for logo
        ScaleAnimation scale = new ScaleAnimation(
            0.6f, 1f, 0.6f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        scale.setDuration(700);
        scale.setInterpolator(this, android.R.interpolator.overshoot);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(700);

        AnimationSet logoAnim = new AnimationSet(false);
        logoAnim.addAnimation(scale);
        logoAnim.addAnimation(fadeIn);
        logo.startAnimation(logoAnim);

        // Tagline fades in after logo
        AlphaAnimation taglineAnim = new AlphaAnimation(0f, 1f);
        taglineAnim.setStartOffset(600);
        taglineAnim.setDuration(500);
        taglineAnim.setFillAfter(true);
        tagline.startAnimation(taglineAnim);

        // If launched from a notification tap, skip animation and go directly
        boolean fromNotification = getIntent().hasExtra("url")
            || getIntent().hasExtra("order_id");
        long delay = fromNotification ? 0 : 2200;

        // Navigate to MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            // Pass along any notification data
            if (getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras());
            }
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, delay);
    }
}
