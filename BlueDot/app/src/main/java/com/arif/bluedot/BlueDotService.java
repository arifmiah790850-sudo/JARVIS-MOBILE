package com.arif.bluedot;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class BlueDotService extends Service {

    private WindowManager windowManager;
    private View dot;
    private Handler handler;
    private int colorIndex = 0;
    private final int[] colors = {
        Color.rgb(0, 190, 255),
        Color.rgb(0, 255, 150),
        Color.rgb(180, 70, 255),
        Color.rgb(255, 70, 150),
        Color.rgb(255, 190, 30),
        Color.rgb(50, 110, 255)
    };

    private final Runnable colorTask = new Runnable() {
        @Override
        public void run() {
            colorIndex = (colorIndex + 1) % colors.length;
            if (dot != null) dot.invalidate();
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotification();
    }

    private void createNotification() {
        String channel = "bluedot_overlay";
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(
            channel, "BlueDot Overlay", NotificationManager.IMPORTANCE_LOW
        ));

        Notification notification = new Notification.Builder(this, channel)
            .setContentTitle("BlueDot is active")
            .setContentText("Floating dot overlay is running")
            .setSmallIcon(android.R.drawable.presence_online)
            .setOngoing(true)
            .build();

        startForeground(1, notification);
    }

    private void showDot() {
        if (dot != null) return;

        dot = new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float cx = getWidth() / 2f;
                float cy = getHeight() / 2f;
                float radius = 9f;
                int color = colors[colorIndex];

                paint.setShader(new RadialGradient(
                    cx, cy, 24f,
                    new int[]{color, color & 0x66FFFFFF, Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP
                ));
                canvas.drawCircle(cx, cy, 24f, paint);

                paint.setShader(null);
                paint.setColor(color);
                paint.setShadowLayer(9f, 0, 0, color);
                canvas.drawCircle(cx, cy, radius, paint);
                paint.clearShadowLayer();
            }
        };

        dot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            48, 48,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 28;
        params.y = 90;

        windowManager.addView(dot, params);
        handler.postDelayed(colorTask, 5000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        getSharedPreferences("settings", MODE_PRIVATE)
            .edit().putBoolean("enabled", true).apply();

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            startForeground(1, buildNotification());
        }

        showDot();
        return START_STICKY;
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, "bluedot_overlay")
            .setContentTitle("BlueDot is active")
            .setContentText("Floating dot overlay is running")
            .setSmallIcon(android.R.drawable.presence_online)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(colorTask);
        if (dot != null) {
            windowManager.removeView(dot);
            dot = null;
        }
        getSharedPreferences("settings", MODE_PRIVATE)
            .edit().putBoolean("enabled", false).apply();
        super.onDestroy();
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }
}
