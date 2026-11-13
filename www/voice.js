let recognition = null;
let listeningEnabled = false;
let restarting = false;
let speaking = false;
let voiceListener = null;
let stateListener = null;
let partialTimer = null;
let pendingTranscript = "";
let lastHandledTranscript = "";

async function listen() {
    const SR = window.SpeechRecognitionPlugin;

    if (!SR || !listeningEnabled || restarting || speaking || recognition) return;

    try {
        const available = await SR.available();

        if (!available.available) {
            setStatus("SPEECH RECOGNITION UNAVAILABLE");
            listeningEnabled = false;
            return;
        }

        const permission = await SR.requestPermissions();

        if (permission.speechRecognition !== "granted") {
            setStatus("MICROPHONE PERMISSION DENIED");
            listeningEnabled = false;
            return;
        }

        if (!voiceListener) {
            voiceListener = await SR.addListener("partialResults", (data) => {
                if (!listeningEnabled || speaking || !data.matches?.length) return;

                const text = data.matches[0].trim();
                if (!text) return;

                pendingTranscript = text;

                clearTimeout(partialTimer);

                partialTimer = setTimeout(async () => {
                    const finalText = pendingTranscript.trim();

                    if (!finalText || finalText === lastHandledTranscript) return;

                    lastHandledTranscript = finalText;
                    speaking = true;

                    try {
                        await SR.stop();
                    } catch (error) {
                        console.log("Recognition stop:", error);
                    }

                    recognition = null;

                    addMessage("YOU", finalText);
                    setStatus("VOICE RECEIVED");
                    handleVoiceCommand(finalText);

                    setTimeout(() => {
                        lastHandledTranscript = "";
                    }, 2500);

                }, 1800);
            });
        }

        if (!stateListener) {
            stateListener = await SR.addListener("listeningState", (data) => {
                if (data.status === "started") {
                    setStatus("🔴 LIVE LISTENING");
                    circle.classList.add("active");
                } else {
                    recognition = null;
                    circle.classList.remove("active");

                    if (listeningEnabled && !restarting && !speaking) {
                        restarting = true;

                        setTimeout(() => {
                            restarting = false;
                            if (listeningEnabled && !speaking) listen();
                        }, 1200);
                    }
                }
            });
        }

        if (!listeningEnabled || speaking) return;

        recognition = true;

        await SR.start({
            language: document.getElementById("language").value,
            maxResults: 1,
            partialResults: true,
            popup: false
        });

    } catch (error) {
        recognition = null;
        setStatus("VOICE ERROR: " + (error.message || error));

        if (listeningEnabled && !speaking) {
            listeningEnabled = false;
            circle.classList.remove("active");
        }
    }
}

async function pauseListeningForSpeech() {
    speaking = true;
    clearTimeout(partialTimer);
    pendingTranscript = "";

    if (recognition && window.SpeechRecognitionPlugin) {
        try {
            await window.SpeechRecognitionPlugin.stop();
        } catch (error) {
            console.log("Pause:", error);
        }
    }

    recognition = null;
    circle.classList.remove("active");
}

function resumeListeningAfterSpeech() {
    speaking = false;

    if (listeningEnabled) {
        setStatus("RESTARTING LISTENING...");

        setTimeout(() => {
            if (listeningEnabled && !speaking) listen();
        }, 700);
    }
}

async function startListening() {
    if (listeningEnabled) return;

    listeningEnabled = true;
    restarting = false;
    recognition = null;

    setStatus("STARTING MICROPHONE...");
    await listen();
}

async function stopListening() {
    listeningEnabled = false;
    restarting = false;
    clearTimeout(partialTimer);

    try {
        if (recognition && window.SpeechRecognitionPlugin) {
            await window.SpeechRecognitionPlugin.stop();
        }
    } catch (error) {
        console.log("Stop:", error);
    }

    recognition = null;
    circle.classList.remove("active");
    setStatus("LISTENING STOPPED");
}
