package com.jarvis.assistant;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.JSObject;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "AlizaVoice",
    permissions = {
        @Permission(
            alias = "microphone",
            strings = { Manifest.permission.RECORD_AUDIO }
        )
    }
)
public class AlizaVoicePlugin extends Plugin {

    private BroadcastReceiver voiceReceiver;

    @Override
    public void load() {
        super.load();

        voiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;

                String type = intent.getStringExtra("type");
                String value = intent.getStringExtra("value");
                boolean isFinal = intent.getBooleanExtra("final", false);

                if (type == null) return;

                JSObject data = new JSObject();
                data.put("value", value == null ? "" : value);
                data.put("final", isFinal);

                if ("result".equals(type)) {
                    notifyListeners("result", data);
                } else if ("state".equals(type)) {
                    data.put("status", value == null ? "" : value);
                    notifyListeners("state", data);
                } else if ("error".equals(type)) {
                    data.put("message", value == null ? "" : value);
                    notifyListeners("error", data);
                }
            }
        };

        ContextCompat.registerReceiver(
            getContext(),
            voiceReceiver,
            new IntentFilter(AlizaVoiceService.ACTION_EVENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    @PluginMethod
    public void checkPermission(PluginCall call) {
        boolean granted = ContextCompat.checkSelfPermission(
            getContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;

        JSObject result = new JSObject();
        result.put("granted", granted);
        call.resolve(result);
    }

    @PluginMethod
    public void requestMicrophonePermission(PluginCall call) {
        requestPermissionForAlias(
            "microphone",
            call,
            "microphonePermissionResult"
        );
    }

    @PermissionCallback
    private void microphonePermissionResult(PluginCall call) {
        boolean granted = getPermissionState("microphone")
            == com.getcapacitor.PermissionState.GRANTED;

        JSObject result = new JSObject();
        result.put("granted", granted);
        call.resolve(result);
    }

    @PluginMethod
    public void startListening(PluginCall call) {
        boolean granted = ContextCompat.checkSelfPermission(
            getContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;

        if (!granted) {
            call.reject("Microphone permission is required.");
            return;
        }

        String language = call.getString("language", "bn-IN");

        Intent intent = new Intent(getContext(), AlizaVoiceService.class);
        intent.setAction(AlizaVoiceService.ACTION_START);
        intent.putExtra("language", language);

        try {
            ContextCompat.startForegroundService(getContext(), intent);
            call.resolve();
        } catch (Exception error) {
            call.reject("Could not start ALIZA voice service.", error);
        }
    }

    @PluginMethod
    public void stopListening(PluginCall call) {
        Intent intent = new Intent(getContext(), AlizaVoiceService.class);
        intent.setAction(AlizaVoiceService.ACTION_STOP);

        try {
            getContext().startService(intent);
            call.resolve();
        } catch (Exception error) {
            call.reject("Could not stop ALIZA voice service.", error);
        }
    }

    @Override
    protected void handleOnDestroy() {
        if (voiceReceiver != null) {
            try {
                getContext().unregisterReceiver(voiceReceiver);
            } catch (Exception ignored) {
            }
            voiceReceiver = null;
        }

        super.handleOnDestroy();
    }
}
