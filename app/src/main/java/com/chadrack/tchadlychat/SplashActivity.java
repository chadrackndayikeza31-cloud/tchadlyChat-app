
package com.chadrack.tchadlychat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(5, 15, 55),
                        Color.rgb(10, 25, 95),
                        Color.rgb(20, 10, 65)
                }
        );

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(30), dp(24), dp(30));
        root.setBackground(background);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo_tchadlychat);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(dp(170), dp(170));
        logoParams.bottomMargin = dp(24);
        logo.setLayoutParams(logoParams);

        TextView title = new TextView(this);
        title.setText("TchadlyChat");
        title.setTextColor(Color.WHITE);
        title.setTextSize(36);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView slogan = new TextView(this);
        slogan.setText("Restez connecté, partout, toujours !");
        slogan.setTextColor(Color.rgb(220, 230, 255));
        slogan.setTextSize(16);
        slogan.setGravity(Gravity.CENTER);
        slogan.setPadding(0, dp(12), 0, dp(22));

        View divider = new View(this);
        GradientDrawable dividerBackground = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.rgb(0, 220, 255),
                        Color.rgb(70, 70, 255),
                        Color.rgb(190, 0, 255)
                }
        );
        dividerBackground.setCornerRadius(dp(5));
        divider.setBackground(dividerBackground);

        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(dp(150), dp(4));
        dividerParams.bottomMargin = dp(150);
        divider.setLayoutParams(dividerParams);

        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);

        TextView loading = new TextView(this);
        loading.setText("Chargement...");
        loading.setTextColor(Color.WHITE);
        loading.setTextSize(15);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(12), 0, 0);

        root.addView(logo);
        root.addView(title);
        root.addView(slogan);
        root.addView(divider);
        root.addView(progress);
        root.addView(loading);

        setContentView(root);

        handler.postDelayed(() -> {
            Intent intent = new Intent(
                    SplashActivity.this,
                    MainActivity.class
            );
            startActivity(intent);
            finish();
        }, 2500);
    }

    private int dp(int value) {
        return (int) (
                value * getResources().getDisplayMetrics().density
        );
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
