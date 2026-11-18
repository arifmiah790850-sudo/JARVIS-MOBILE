package com.jarvis.assistant;

import android.content.Context;
import android.service.voice.VoiceInteractionSession;
import android.view.View;
import android.widget.TextView;

public class AlizaVoiceInteractionSession
        extends VoiceInteractionSession {

    public AlizaVoiceInteractionSession(Context context) {
        super(context);
    }

    @Override
    public View onCreateContentView() {
        TextView view = new TextView(getContext());

        view.setText("ALIZA AI\n\nHello Boss! How can I help you?");
        view.setTextSize(22);
        view.setPadding(40, 60, 40, 60);

        return view;
    }
}
