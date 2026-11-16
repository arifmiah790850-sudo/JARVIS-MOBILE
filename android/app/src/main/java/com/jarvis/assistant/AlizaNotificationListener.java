package com.jarvis.assistant;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.content.Intent;

public class AlizaNotificationListener
        extends NotificationListenerService {

    public static final String ACTION_MESSAGE =
        "com.jarvis.assistant.ALIZA_MESSAGE_RECEIVED";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if (sbn == null) return;

        String pkg = sbn.getPackageName();

        if (pkg == null || pkg.equals(getPackageName())) return;

        String app = "";

        if (pkg.equals("com.whatsapp")) {
            app = "WhatsApp";
        } else if (pkg.equals("org.telegram.messenger")) {
            app = "Telegram";
        } else if (pkg.equals("com.facebook.orca")) {
            app = "Messenger";
        } else if (
            pkg.equals("com.google.android.apps.messaging") ||
            pkg.equals("com.android.mms") ||
            pkg.equals("com.samsung.android.messaging") ||
            pkg.equals("com.android.messaging")
        ) {
            app = "SMS";
        } else {
            return;
        }

        Notification notification = sbn.getNotification();

        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        CharSequence title =
            extras.getCharSequence(Notification.EXTRA_TITLE);

        CharSequence text =
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

        if (text == null) {
            text = extras.getCharSequence(Notification.EXTRA_TEXT);
        }

        if (title == null || text == null) return;

        String sender = title.toString();
        String message = text.toString();

        if (message.trim().isEmpty()) return;

        Intent event = new Intent(ACTION_MESSAGE);
        event.setPackage(getPackageName());

        event.putExtra("app", app);
        event.putExtra("sender", sender);
        event.putExtra("message", message);
        event.putExtra("package", pkg);
        event.putExtra("notification_id", sbn.getId());

        sendBroadcast(event);
    }
}
