
export interface UserProfile {
  name: string;
  age: number;
  height: number; // cm
  weight: number; // kg
  stepGoal: number;
  calorieGoal: number;
  onboardingComplete: boolean;
  apiKey: string;
  avatar?: string;
}

export interface DailyStats {
  date: string;
  steps: number;
  caloriesBurned: number;
  activeMinutes: number;
  distance: number; // km
}

export interface Meal {
  id: string;
  name: string;
  calories: number;
  protein: number;
  carbs: number;
  fats: number;
  timestamp: number;
  image?: string;
  type: 'breakfast' | 'lunch' | 'dinner' | 'snack';
}

export interface WorkoutSession {
  id: string;
  type: 'squat' | 'pushup' | 'jumping_jack';
  reps: number;
  duration: number; // seconds
  calories: number;
  timestamp: number;
  formScore: number; // 0-100
}

export type ExerciseType = 'squat' | 'pushup' | 'jumping_jack';

export interface NutritionAnalysis {
  foodName: string;
  calories: number;
  macros: {
    protein: number;
    carbs: number;
    fats: number;
  };
}

export interface GeneratedExercise {
  name: string;
  sets: string;
  reps: string;
  rest: string;
  notes: string;
}

export interface GeneratedWorkoutPlan {
  title: string;
  difficulty: string;
  duration: string;
  exercises: GeneratedExercise[];
}
