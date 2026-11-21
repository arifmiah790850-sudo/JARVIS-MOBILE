package com.arif.bluedot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private void startDot() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
            return;
        }

        getSharedPreferences("bluedot", MODE_PRIVATE)
            .edit().putBoolean("enabled", true).apply();

        Intent service = new Intent(this, DotService.class);

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }

    private void stopDot() {
        getSharedPreferences("bluedot", MODE_PRIVATE)
            .edit().putBoolean("enabled", false).apply();

        stopService(new Intent(this, DotService.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(30, 30, 30, 30);
        layout.setBackgroundColor(Color.rgb(15, 18, 30));

        TextView title = new TextView(this);
        title.setText("BlueDot");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView info = new TextView(this);
        info.setText("Floating neon dot\nColor changes every 5 seconds");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        layout.addView(info);

        Button start = new Button(this);
        start.setText("START BLUEDOT");
        start.setOnClickListener(v -> startDot());
        layout.addView(start);

        Button stop = new Button(this);
        stop.setText("STOP BLUEDOT");
        stop.setOnClickListener(v -> stopDot());
        layout.addView(stop);

        setContentView(layout);
    }
}
