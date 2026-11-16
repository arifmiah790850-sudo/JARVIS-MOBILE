require("dotenv").config();

const express = require("express");
const cors = require("cors");
const { GoogleGenAI } = require("@google/genai");

const app = express();

app.use(cors());
app.use(express.json());

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY
});

app.get("/health", (req, res) => {
  res.json({
    status: "online",
    assistant: "ALIZA",
    provider: "Gemini"
  });
});

app.post("/chat", async (req, res) => {
  console.log("[ALIZA] Chat request received:", new Date().toISOString());
  try {
    const { message, language } = req.body;

    if (!message || typeof message !== "string") {
      return res.status(400).json({
        error: "Message is required"
      });
    }

    const response = await ai.models.generateContent({
      model: "gemini-3.5-flash-lite",
      contents: message,
      config: {
        systemInstruction: `You are ALIZA, a helpful AI assistant. Reply in the language selected by the user: ${language || "English"}. If the user writes in another language, understand them and reply naturally in their language.`
      }
    });

    console.log("[ALIZA] Gemini response received");
    res.json({
      reply: response.text
    });

  } catch (error) {
    console.error("[ALIZA] Gemini Error:", error.message);

    res.status(500).json({
      error: "Gemini AI request failed"
    });
  }
});

app.listen(3000, "0.0.0.0", () => {
  console.log("ALIZA Gemini Server running on port 3000");
});
