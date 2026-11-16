package com.jarvis.assistant;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class AlizaVoiceService extends Service {

    public static final String ACTION_START =
        "com.jarvis.assistant.ALIZA_VOICE_START";
    public static final String ACTION_STOP =
        "com.jarvis.assistant.ALIZA_VOICE_STOP";
    public static final String ACTION_EVENT =
        "com.jarvis.assistant.ALIZA_VOICE_EVENT";

    private static final String CHANNEL_ID = "aliza_voice_session";
    private static final int NOTIFICATION_ID = 7101;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private SpeechRecognizer recognizer;
    private int recognizerGeneration = 0;
    private boolean active = false;
    private int consecutiveErrors = 0;
    private static final int MAX_CONSECUTIVE_ERRORS = 5;
    private String language = "bn-IN";

    private final Runnable restartTask = () -> {
        if (active) startRecognition();
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSession();
            return START_NOT_STICKY;
        }

        if (intent == null || !ACTION_START.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {
            sendEvent("error", "MICROPHONE_PERMISSION_DENIED", false);
            stopSelf();
            return START_NOT_STICKY;
        }

        language = intent.getStringExtra("language");
        if (language == null || language.trim().isEmpty()) {
            language = "bn-IN";
        }

        try {
            Notification notification = buildNotification();

            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                );
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }

            active = true;
            sendEvent("state", "starting", false);
            startRecognition();

        } catch (Exception error) {
            sendEvent("error", error.getMessage(), false);
            stopSession();
        }

        return START_NOT_STICKY;
    }

    private void startRecognition() {
        if (!active) return;

        handler.removeCallbacks(restartTask);
        destroyRecognizer();
        final int generation = recognizerGeneration;

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sendEvent("error", "SPEECH_RECOGNITION_UNAVAILABLE", false);
            stopSession();
            return;
        }

        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {

                private boolean current() {
                    return active && generation == recognizerGeneration;
                }

                @Override
                public void onReadyForSpeech(Bundle params) {
                    if (!current()) return;
                    consecutiveErrors = 0;
                    sendEvent("state", "listening", false);
                }

                @Override
                public void onBeginningOfSpeech() {
                    if (!current()) return;
                    sendEvent("state", "hearing_speech", false);
                }

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    if (!current()) return;
                    sendEvent("state", "processing", false);
                }

                @Override
                public void onError(int error) {
                    if (!current()) return;
                    sendEvent("error", "RECOGNIZER_ERROR_" + error, false);
                    if (++consecutiveErrors > MAX_CONSECUTIVE_ERRORS) {
                        sendEvent("error", "RECOGNIZER_RETRY_LIMIT_REACHED", false);
                        stopSession();
                    } else {
                        scheduleRestart();
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    if (!current()) return;

                    ArrayList<String> matches =
                        results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        );

                    if (matches != null && !matches.isEmpty()) {
                        sendEvent("result", matches.get(0), true);
                    } else {
                        // No usable transcript: continue listening.
                        scheduleRestart();
                    }
                    // JS restarts listening after command and TTS.
                }

                @Override
                public void onPartialResults(Bundle results) {
                    if (!current()) return;

                    ArrayList<String> matches =
                        results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        );

                    if (matches != null && !matches.isEmpty()) {
                        sendEvent("result", matches.get(0), false);
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });

            Intent recognitionIntent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            recognitionIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );
            recognitionIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                language
            );
            recognitionIntent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            );
            recognitionIntent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
            );
            recognitionIntent.putExtra(
                RecognizerIntent.EXTRA_CALLING_PACKAGE,
                getPackageName()
            );

            recognizer.startListening(recognitionIntent);

        } catch (Exception error) {
            sendEvent("error", error.getMessage(), false);
            if (++consecutiveErrors > MAX_CONSECUTIVE_ERRORS) {
                sendEvent("error", "RECOGNIZER_RETRY_LIMIT_REACHED", false);
                stopSession();
            } else {
                scheduleRestart();
            }
        }
    }

    private void scheduleRestart() {
        if (!active) return;
        handler.removeCallbacks(restartTask);
        handler.postDelayed(restartTask, 1200);
    }

    private void destroyRecognizer() {
        recognizerGeneration++;
        SpeechRecognizer oldRecognizer = recognizer;
        recognizer = null;

        if (oldRecognizer != null) {
            try {
                oldRecognizer.cancel();
                oldRecognizer.destroy();
            } catch (Exception ignored) {}

            recognizer = null;
        }
    }

    private void sendEvent(String type, String value, boolean finalResult) {
        Intent event = new Intent(ACTION_EVENT);
        event.setPackage(getPackageName());
        event.putExtra("type", type);
        event.putExtra("value", value == null ? "" : value);
        event.putExtra("final", finalResult);
        sendBroadcast(event);
    }

    private void stopSession() {
        active = false;
        handler.removeCallbacksAndMessages(null);
        destroyRecognizer();

        sendEvent("state", "stopped", false);

        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }

        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "ALIZA Voice Session",
                NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription(
                "Shows when ALIZA microphone listening is active."
            );

            NotificationManager manager =
                getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, AlizaVoiceService.class);
        stopIntent.setAction(ACTION_STOP);

        PendingIntent stopPendingIntent = PendingIntent.getService(
            this,
            7102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT |
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("ALIZA Voice Session")
            .setContentText("Microphone listening session is active.")
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "STOP",
                stopPendingIntent
            )
            .build();
    }

    @Override
    public void onDestroy() {
        active = false;
        handler.removeCallbacksAndMessages(null);
        destroyRecognizer();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
