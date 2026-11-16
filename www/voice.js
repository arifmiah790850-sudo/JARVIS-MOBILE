let recognition = null;
let listeningEnabled = false;
let speaking = false;
let startInProgress = false;
let voiceResultListener = null;
let voiceStateListener = null;
let voiceErrorListener = null;
let restartTimer = null;
let lastHandledTranscript = "";
let lastVoiceError = "";

const wait = (ms) => new Promise(resolve => setTimeout(resolve, ms));

function voicePlugin() {
    return window.AlizaVoicePlugin;
}

async function requestNativeStop() {
    const plugin = voicePlugin();
    if (!plugin) return;

    try {
        await plugin.stopListening();
    } catch (error) {
        console.log("ALIZA native stop:", error);
    }
    recognition = null;
    circle.classList.remove("active");
    await wait(200);
}

async function handleFinalTranscript(text) {
    const finalText = (text || "").trim();
    if (!finalText || !listeningEnabled || speaking) return;
    if (finalText === lastHandledTranscript) return;

    lastHandledTranscript = finalText;
    speaking = true;
    clearTimeout(restartTimer);
    restartTimer = null;

    await requestNativeStop();

    addMessage("YOU", finalText);
    setStatus("PROCESSING VOICE: " + finalText);

    try {
        await handleVoiceCommand(finalText);
    } catch (error) {
        console.error("Voice command error:", error);
        setStatus("COMMAND ERROR: " + (error.message || error));
        speaking = false;
        resumeListeningAfterSpeech();
    }

    setTimeout(() => {
        lastHandledTranscript = "";
    }, 2500);
}

async function ensureVoiceListeners() {
    const plugin = voicePlugin();
    if (!plugin) throw new Error("ALIZA NATIVE VOICE PLUGIN NOT LOADED");

    if (!voiceResultListener) {
        voiceResultListener = await plugin.addListener("result", data => {
            if (!listeningEnabled || speaking) return;

            const text = (data.value || "").trim();
            if (!text) return;

            setStatus("HEARD: " + text);

            if (data.final) {
                handleFinalTranscript(text);
            }
        });
    }

    if (!voiceStateListener) {
        voiceStateListener = await plugin.addListener("state", data => {
            const status = data.status || "";

            if (status === "listening") {
                recognition = true;
                setStatus("🔴 LIVE LISTENING");
                circle.classList.add("active");
            } else if (status === "starting") {
                setStatus("STARTING MICROPHONE...");
            } else if (status === "hearing_speech") {
                setStatus("HEARING SPEECH...");
            } else if (status === "processing") {
                setStatus("PROCESSING SPEECH...");
            } else if (status === "stopped") {
                recognition = null;
                circle.classList.remove("active");

                if (listeningEnabled && !speaking) {
                    listeningEnabled = false;
                    setStatus(lastVoiceError ? "VOICE SERVICE STOPPED: " + lastVoiceError : "VOICE SERVICE STOPPED");
                }
            }
        });
    }

    if (!voiceErrorListener) {
        voiceErrorListener = await plugin.addListener("error", data => {
            const message = data.message || "UNKNOWN VOICE ERROR";
            lastVoiceError = message;
            setStatus("VOICE ERROR: " + message);
            console.error("ALIZA voice:", message);

            if (
                message.includes("RETRY_LIMIT_REACHED") ||
                message.includes("MICROPHONE_PERMISSION_DENIED")
            ) {
                listeningEnabled = false;
                recognition = null;
                circle.classList.remove("active");
            }
        });
    }
}

async function startNativeSession() {
    const plugin = voicePlugin();
    if (!plugin) throw new Error("ALIZA NATIVE VOICE PLUGIN NOT LOADED");
    if (!listeningEnabled || speaking || startInProgress) return;

    startInProgress = true;

    try {
        await ensureVoiceListeners();

        let permission = await plugin.checkPermission();

        if (!permission.granted) {
            permission = await plugin.requestMicrophonePermission();
        }

        if (!permission.granted) {
            listeningEnabled = false;
            setStatus("MICROPHONE PERMISSION DENIED");
            return;
        }

        const language = document.getElementById("language").value;
        await plugin.startListening({ language });
        setStatus("STARTING ALIZA VOICE SERVICE...");
    } finally {
        startInProgress = false;
    }
}

async function listen() {
    return startNativeSession();
}

async function pauseListeningForSpeech() {
    speaking = true;
    clearTimeout(restartTimer);
    restartTimer = null;
    await requestNativeStop();
}

function resumeListeningAfterSpeech() {
    speaking = false;

    if (!listeningEnabled) return;

    clearTimeout(restartTimer);
    setStatus("RESTARTING LISTENING...");

    restartTimer = setTimeout(async () => {
        restartTimer = null;
        if (listeningEnabled && !speaking) {
            try {
                await startNativeSession();
            } catch (error) {
                listeningEnabled = false;
                setStatus("VOICE ERROR: " + (error.message || error));
            }
        }
    }, 700);
}

async function startListening() {
    if (listeningEnabled) return;

    listeningEnabled = true;
    lastVoiceError = "";
    speaking = false;
    recognition = null;
    setStatus("REQUESTING MICROPHONE...");

    try {
        await startNativeSession();
    } catch (error) {
        listeningEnabled = false;
        setStatus("VOICE ERROR: " + (error.message || error));
    }
}

async function stopListening() {
    listeningEnabled = false;
    speaking = false;

    clearTimeout(restartTimer);
    restartTimer = null;

    await requestNativeStop();
    setStatus("LISTENING STOPPED");
}
