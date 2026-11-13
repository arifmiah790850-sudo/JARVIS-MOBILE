let recognition = null;
let listeningEnabled = false;
let restarting = false;
let voiceListener = null;
let stateListener = null;

async function listen() {
    const SR = window.SpeechRecognitionPlugin;

    if (!SR || !listeningEnabled || restarting) return;

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
                if (!listeningEnabled || !data.matches?.length) return;

                const text = data.matches[0].trim();
                if (!text) return;

                document.getElementById("message").value = text;
                addMessage("YOU", text);
                setStatus("VOICE RECEIVED");
                handleVoiceCommand(text);
            });
        }

        if (!stateListener) {
            stateListener = await SR.addListener("listeningState", (data) => {
                if (data.status === "started") {
                    setStatus("🔴 LIVE LISTENING");
                    circle.classList.add("active");
                } else {
                    circle.classList.remove("active");
                    if (listeningEnabled && !restarting) {
                        restarting = true;
                        setTimeout(() => {
                            restarting = false;
                            if (listeningEnabled) listen();
                        }, 1200);
                    }
                }
            });
        }

        if (!listeningEnabled) return;

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
        if (listeningEnabled) {
            listeningEnabled = false;
            circle.classList.remove("active");
        }
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

    try {
        if (recognition) {
            await window.SpeechRecognitionPlugin.stop();
        }
    } catch (error) {
        console.log("Stop:", error);
    }

    recognition = null;
    circle.classList.remove("active");
    setStatus("LISTENING STOPPED");
}
