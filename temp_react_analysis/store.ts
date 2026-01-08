import { create } from 'zustand';
import { persist, createJSONStorage, StateStorage } from 'zustand/middleware';
import { get, set, del } from 'idb-keyval';
import { UserProfile, DailyStats, Meal, WorkoutSession } from './types';
import { analyzeFoodImage } from './services/geminiService';

interface AppState {
  user: UserProfile | null;
  dailyStats: Record<string, DailyStats>;
  meals: Meal[];
  workouts: WorkoutSession[];
  lastInsightDate: string; // ISO Date string YYYY-MM-DD
  
  // Analysis State
  isAnalyzing: boolean;
  analysisError: string | null;

  // Internal state for async hydration
  _hasHydrated: boolean;
  setHasHydrated: (state: boolean) => void;

  // Actions
  setUser: (user: Partial<UserProfile>) => void;
  updateDailySteps: (date: string, steps: number) => void;
  addMeal: (meal: Meal) => void;
  addWorkout: (workout: WorkoutSession) => void;
  resetOnboarding: () => void;
  markInsightSeen: (date: string) => void;
  
  // Async Actions
  performFoodAnalysis: (imageBase64: string) => Promise<void>;
  clearAnalysisError: () => void;
}

const DEFAULT_STATS: DailyStats = {
  date: new Date().toISOString().split('T')[0],
  steps: 0,
  caloriesBurned: 0,
  activeMinutes: 0,
  distance: 0,
};

// Custom storage adapter for IndexedDB
const idbStorage: StateStorage = {
  getItem: async (name: string): Promise<string | null> => {
    return (await get(name)) || null;
  },
  setItem: async (name: string, value: string): Promise<void> => {
    await set(name, value);
  },
  removeItem: async (name: string): Promise<void> => {
    await del(name);
  },
};

export const useStore = create<AppState>()(
  persist(
    (set, get) => ({
      user: null,
      dailyStats: {},
      meals: [],
      workouts: [],
      lastInsightDate: '',
      
      isAnalyzing: false,
      analysisError: null,
      
      _hasHydrated: false,
      setHasHydrated: (state) => set({ _hasHydrated: state }),

      setUser: (userData) => set((state) => ({
        user: state.user ? { ...state.user, ...userData } : userData as UserProfile
      })),

      updateDailySteps: (date, steps) => set((state) => {
        const currentStats = state.dailyStats[date] || { ...DEFAULT_STATS, date };
        const distance = (steps * 0.762) / 1000; // Approx 0.762m per step
        const calories = steps * 0.04; // Approx 0.04 kcal per step
        
        return {
          dailyStats: {
            ...state.dailyStats,
            [date]: {
              ...currentStats,
              steps,
              distance,
              caloriesBurned: calories,
              activeMinutes: Math.floor(steps / 100) // Rough approx
            }
          }
        };
      }),

      addMeal: (meal) => set((state) => ({
        meals: [meal, ...state.meals]
      })),

      addWorkout: (workout) => set((state) => ({
        workouts: [workout, ...state.workouts]
      })),

      resetOnboarding: () => set({ user: null, meals: [], workouts: [], dailyStats: {}, lastInsightDate: '' }),

      markInsightSeen: (date) => set({ lastInsightDate: date }),

      performFoodAnalysis: async (imageBase64: string) => {
        const { user, addMeal } = get();
        set({ isAnalyzing: true, analysisError: null });

        try {
          if (!user?.apiKey) throw new Error("API Key missing");

          const analysis = await analyzeFoodImage(user.apiKey, imageBase64);
          
          // Determine meal type based on time of day
          const hour = new Date().getHours();
          let type: Meal['type'] = 'snack';
          if (hour >= 5 && hour < 11) type = 'breakfast';
          else if (hour >= 11 && hour < 16) type = 'lunch';
          else if (hour >= 16 && hour < 22) type = 'dinner';

          const newMeal: Meal = {
            id: Date.now().toString(),
            name: analysis.foodName,
            calories: analysis.calories,
            protein: analysis.macros.protein,
            carbs: analysis.macros.carbs,
            fats: analysis.macros.fats,
            timestamp: Date.now(),
            type,
            image: imageBase64
          };
          
          addMeal(newMeal);
        } catch (e: any) {
          console.error("Analysis failed:", e);
          set({ analysisError: e.message || "Failed to analyze food. Please try again." });
        } finally {
          set({ isAnalyzing: false });
        }
      },

      clearAnalysisError: () => set({ analysisError: null }),
    }),
    {
      name: 'fitu-storage',
      storage: createJSONStorage(() => idbStorage),
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated(true);
      },
      // Do not persist transient UI state like isAnalyzing
      partialize: (state) => ({
        user: state.user,
        dailyStats: state.dailyStats,
        meals: state.meals,
        workouts: state.workouts,
        lastInsightDate: state.lastInsightDate
      })
    }
  )
);