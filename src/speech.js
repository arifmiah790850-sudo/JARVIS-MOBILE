import { SpeechRecognition } from "@capacitor-community/speech-recognition";
import { TextToSpeech } from "@capacitor-community/text-to-speech";
import { registerPlugin } from "@capacitor/core";

window.SpeechRecognitionPlugin = SpeechRecognition;
window.TextToSpeechPlugin = TextToSpeech;
window.PhoneControlPlugin = registerPlugin("PhoneControl");
