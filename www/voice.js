let recognition = null;
let listeningEnabled = false;
let restarting = false;
let speaking = false;

let voiceListener = null;
let stateListener = null;

let partialTimer = null;
let restartTimer = null;
let pendingTranscript = "";
let lastHandledTranscript = "";

let startInProgress = false;

const wait = (ms) => new Promise(resolve => setTimeout(resolve, ms));

async function requestNativeStop() {
    const SR = window.SpeechRecognitionPlugin;

    if (!SR) return;

    try {
        // Plugin stop Promise resolve না করলেও আমরা await করছি না।
        SR.stop().catch(error => console.log("Recognition stop:", error));
    } catch (error) {
        console.log("Recognition stop:", error);
    }

    await wait(350);
}

async function listen() {
    const SR = window.SpeechRecognitionPlugin;

    if (!SR) {
        setStatus("ERROR: SPEECH PLUGIN NOT LOADED");
        return;
    }

    if (
        !listeningEnabled ||
        restarting ||
        speaking ||
        recognition ||
        startInProgress
    ) return;

    startInProgress = true;

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
                if (!listeningEnabled || speaking) return;

                if (!data.matches?.length) return;

                const text = data.matches[0].trim();
                if (!text) return;

                pendingTranscript = text;
                setStatus("HEARD: " + text);

                clearTimeout(partialTimer);
                clearTimeout(restartTimer);

                partialTimer = setTimeout(async () => {
                    partialTimer = null;

                    const finalText = pendingTranscript.trim();

                    if (
                        !finalText ||
                        finalText === lastHandledTranscript ||
                        !listeningEnabled
                    ) return;

                    lastHandledTranscript = finalText;
                    speaking = true;

                    clearTimeout(restartTimer);
                    setStatus("STOPPING MICROPHONE...");

                    await requestNativeStop();

                    recognition = null;
                    pendingTranscript = "";

                    addMessage("YOU", finalText);
                    setStatus("PROCESSING VOICE: " + finalText);

                    try {
                        await handleVoiceCommand(finalText);
                    } catch (error) {
                        console.error("Voice command error:", error);
                        setStatus("COMMAND ERROR: " + (error.message || error));
                        resumeListeningAfterSpeech();
                    }

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
                    return;
                }

                recognition = null;
                circle.classList.remove("active");

                // কথার ফলাফল আসার অপেক্ষায় থাকলে Restart নয়।
                if (
                    listeningEnabled &&
                    !speaking &&
                    !restarting &&
                    !partialTimer &&
                    !pendingTranscript
                ) {
                    restarting = true;

                    clearTimeout(restartTimer);

                    restartTimer = setTimeout(() => {
                        restarting = false;
                        restartTimer = null;

                        if (listeningEnabled && !speaking) {
                            listen();
                        }
                    }, 1500);
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

    } finally {
        startInProgress = false;
    }
}

async function pauseListeningForSpeech() {
    speaking = true;

    clearTimeout(partialTimer);
    clearTimeout(restartTimer);

    partialTimer = null;
    restartTimer = null;
    pendingTranscript = "";

    await requestNativeStop();

    recognition = null;
    circle.classList.remove("active");
}

function resumeListeningAfterSpeech() {
    speaking = false;

    if (listeningEnabled) {
        clearTimeout(restartTimer);
        setStatus("RESTARTING LISTENING...");

        restartTimer = setTimeout(() => {
            restartTimer = null;

            if (listeningEnabled && !speaking) {
                recognition = null;
                restarting = false;
                listen();
            }
        }, 700);
    }
}

async function startListening() {
    if (listeningEnabled) return;

    listeningEnabled = true;
    restarting = false;
    speaking = false;
    recognition = null;

    setStatus("STARTING MICROPHONE...");
    await listen();
}

async function stopListening() {
    listeningEnabled = false;
    restarting = false;

    clearTimeout(partialTimer);
    clearTimeout(restartTimer);

    partialTimer = null;
    restartTimer = null;
    pendingTranscript = "";

    await requestNativeStop();

    recognition = null;
    circle.classList.remove("active");
    setStatus("LISTENING STOPPED");
}
