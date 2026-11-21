package com.arif.bluedot;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class DotService extends Service {

    private WindowManager windowManager;
    private DotView dotView;
    private Handler handler;
    private int colorIndex = 0;

    private final int[] colors = {
        Color.rgb(0, 170, 255),
        Color.rgb(150, 50, 255),
        Color.rgb(255, 40, 150),
        Color.rgb(255, 70, 50),
        Color.rgb(255, 170, 0),
        Color.rgb(0, 230, 120),
        Color.rgb(0, 240, 230)
    };

    private final Runnable colorTask = new Runnable() {
        @Override
        public void run() {
            if (dotView != null) {
                colorIndex = (colorIndex + 1) % colors.length;
                dotView.setDotColor(colors[colorIndex]);
                handler.postDelayed(this, 5000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        createNotification();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (dotView == null) {
            showDot();
        }

        handler.removeCallbacks(colorTask);
        handler.postDelayed(colorTask, 5000);

        return START_STICKY;
    }

    private void createNotification() {
        String channelId = "bluedot_channel";

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                channelId, "BlueDot Overlay",
                NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class)
                .createNotificationChannel(channel);
        }

        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
            this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification =
            new Notification.Builder(this, channelId)
                .setContentTitle("BlueDot is active")
                .setContentText("Floating dot is running")
                .setSmallIcon(android.R.drawable.presence_online)
                .setContentIntent(pending)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

    private void showDot() {
        dotView = new DotView(this);

        int size = (int) (30 * getResources().getDisplayMetrics().density);

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                size, size,
                Build.VERSION.SDK_INT >= 26
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.x = (int) (42 * getResources().getDisplayMetrics().density);
        params.y = (int) (10 * getResources().getDisplayMetrics().density);

        try {
            windowManager.addView(dotView, params);
            dotView.setDotColor(colors[colorIndex]);
        } catch (Exception e) {
            dotView = null;
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        if (handler != null) {
            handler.removeCallbacks(colorTask);
        }

        if (dotView != null) {
            try {
                windowManager.removeView(dotView);
            } catch (Exception ignored) {
            }
            dotView = null;
        }

        super.onDestroy();
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }

    private static class DotView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int dotColor = Color.BLUE;

        DotView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setDotColor(int color) {
            dotColor = color;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = 5f * getResources()
                .getDisplayMetrics().density;

            paint.setColor(dotColor);
            paint.setStyle(Paint.Style.FILL);
            paint.setShadowLayer(radius * 1.5f, 0, 0, dotColor);
            canvas.drawCircle(cx, cy, radius, paint);
        }
    }
}
