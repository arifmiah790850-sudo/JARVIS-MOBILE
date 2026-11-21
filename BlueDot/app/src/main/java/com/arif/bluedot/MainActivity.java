package com.arif.bluedot;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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

        Intent service = new Intent(this, BlueDotService.class);
        service.setAction("START");
        startForegroundService(service);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 60, 35, 35);

        TextView title = new TextView(this);
        title.setText("BlueDot Always Overlay");
        title.setTextSize(25);
        layout.addView(title);

        TextView info = new TextView(this);
        info.setText("A glowing dot that changes color every 5 seconds.");
        info.setTextSize(16);
        layout.addView(info);

        Button start = new Button(this);
        start.setText("START BLUEDOT");
        start.setOnClickListener(v -> startDot());
        layout.addView(start);

        Button stop = new Button(this);
        stop.setText("STOP BLUEDOT");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, BlueDotService.class));
        });
        layout.addView(stop);

        setContentView(layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
