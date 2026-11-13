package com.jarvis.assistant;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.provider.MediaStore;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "PhoneControl")
public class PhoneControlPlugin extends Plugin {

    @PluginMethod
    public void openTarget(PluginCall call) {
        String target = call.getString("target", "");
        Intent intent;

        switch (target) {
            case "settings":
                intent = new Intent(Settings.ACTION_SETTINGS);
                break;

            case "wifi":
                intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                break;

            case "bluetooth":
                intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                break;

            case "camera":
                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                break;

            case "youtube":
                intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com"));
                break;

            case "chrome":
                intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com"));
                intent.setPackage("com.android.chrome");
                break;

            default:
                call.reject("Unknown phone command");
                return;
        }

        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            call.resolve();
        } catch (Exception error) {
            call.reject("Could not open: " + target, error);
        }
    }
}
