import { GoogleGenAI, Type } from "@google/genai";
import { NutritionAnalysis, GeneratedWorkoutPlan } from "../types";

export const analyzeFoodImage = async (apiKey: string, base64Image: string): Promise<NutritionAnalysis> => {
  if (!apiKey) throw new Error("API Key is missing");

  const ai = new GoogleGenAI({ apiKey });

  // Extract real mime type and data from base64 string
  const matches = base64Image.match(/^data:(image\/[a-zA-Z+]+);base64,(.+)$/);
  
  if (!matches || matches.length !== 3) {
    throw new Error("Invalid image format. Please upload a valid image.");
  }

  const mimeType = matches[1]; 
  const cleanBase64 = matches[2];

  try {
    const response = await ai.models.generateContent({
      model: 'gemini-3-flash-preview',
      contents: {
        parts: [
          {
            inlineData: {
              mimeType: mimeType,
              data: cleanBase64
            }
          },
          {
            text: "Analyze this food image. Identify the main dish, estimate calories, and provide macros (protein, carbs, fats) for the visible portion. Return ONLY JSON."
          }
        ]
      },
      config: {
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            foodName: { type: Type.STRING },
            calories: { type: Type.NUMBER },
            macros: {
              type: Type.OBJECT,
              properties: {
                protein: { type: Type.NUMBER },
                carbs: { type: Type.NUMBER },
                fats: { type: Type.NUMBER }
              },
              required: ["protein", "carbs", "fats"]
            }
          },
          required: ["foodName", "calories", "macros"]
        }
      }
    });

    const text = response.text;
    if (!text) throw new Error("No response from AI");
    
    return JSON.parse(text) as NutritionAnalysis;
  } catch (error: any) {
    console.error("Gemini Vision Error:", error);
    
    // Attempt to parse raw JSON error message if present (fixes the ugly alert)
    let errorMessage = error.message || "Failed to analyze image";
    
    // Check if the error message is actually a JSON string (like in the screenshot)
    if (typeof errorMessage === 'string' && errorMessage.trim().startsWith('{')) {
      try {
        const parsed = JSON.parse(errorMessage);
        if (parsed.error && parsed.error.message) {
          errorMessage = parsed.error.message;
        }
      } catch (e) {
        // failed to parse, keep original
      }
    }

    if (errorMessage.includes('429') || errorMessage.includes('quota')) {
      errorMessage = "Usage limit exceeded. Please try again later or check your API plan.";
    } else if (errorMessage.includes('403')) {
      errorMessage = "API Key not valid or has restricted access.";
    } else if (errorMessage.includes('limit: 0')) {
      errorMessage = "Model unavailable for this key. Please use a Paid API key or a different project.";
    }
    
    throw new Error(errorMessage);
  }
};

export const getFitnessAdvice = async (apiKey: string, context: string): Promise<string> => {
   const ai = new GoogleGenAI({ apiKey });
   try {
     const response = await ai.models.generateContent({
       model: 'gemini-3-flash-preview',
       contents: `You are a professional fitness coach. Give brief, encouraging advice based on this user stats: ${context}. Keep it under 30 words.`,
     });
     return response.text || "Keep moving!";
   } catch (e) {
     console.error(e);
     return "Stay consistent and keep moving!";
   }
};

export const generateDailyRecap = async (apiKey: string, userName: string, stats: { steps: number, calories: number, activeMinutes: number }): Promise<string> => {
  if (!apiKey) return "Great job today! Keep hitting those goals.";
  
  const ai = new GoogleGenAI({ apiKey });
  try {
    const response = await ai.models.generateContent({
      model: 'gemini-3-flash-preview',
      contents: `You are a friendly fitness companion. The user ${userName} has finished their day.
      Stats: ${stats.steps} steps, ${stats.calories} calories burned, ${stats.activeMinutes} active minutes.
      Write a warm, short (max 40 words) End-of-Day summary message celebrating their effort or encouraging them for tomorrow. Use emojis.`,
    });
    return response.text || "Another day stronger! Rest well and get ready to crush it tomorrow.";
  } catch (e) {
    console.error(e);
    return "Great effort today! Rest up for a strong tomorrow.";
  }
};

export const generateWorkoutPlan = async (
  apiKey: string, 
  preferences: { 
    focus: string, 
    level: string, 
    duration: string, 
    equipment: string 
  }
): Promise<GeneratedWorkoutPlan> => {
  if (!apiKey) throw new Error("API Key is missing");

  const ai = new GoogleGenAI({ apiKey });

  try {
    const response = await ai.models.generateContent({
      model: 'gemini-3-flash-preview',
      contents: `Create a specific workout routine.
      Focus: ${preferences.focus}
      Level: ${preferences.level}
      Duration: ${preferences.duration}
      Equipment: ${preferences.equipment}
      
      Return a JSON object with a title, difficulty, duration, and a list of 5-8 exercises.
      For each exercise include: name, sets, reps, rest (in seconds), and brief notes on form.`,
      config: {
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            title: { type: Type.STRING },
            difficulty: { type: Type.STRING },
            duration: { type: Type.STRING },
            exercises: {
              type: Type.ARRAY,
              items: {
                type: Type.OBJECT,
                properties: {
                  name: { type: Type.STRING },
                  sets: { type: Type.STRING },
                  reps: { type: Type.STRING },
                  rest: { type: Type.STRING },
                  notes: { type: Type.STRING }
                },
                required: ["name", "sets", "reps", "notes"]
              }
            }
          },
          required: ["title", "exercises", "difficulty"]
        }
      }
    });

    const text = response.text;
    if (!text) throw new Error("No response from AI");
    
    return JSON.parse(text) as GeneratedWorkoutPlan;
  } catch (error: any) {
    console.error("Workout Gen Error:", error);
    let msg = error.message || "Failed to generate workout.";
    if (msg.includes('429')) msg = "Usage limit exceeded. Please try again later.";
    throw new Error(msg);
  }
};
