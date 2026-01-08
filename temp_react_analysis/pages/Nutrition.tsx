import React, { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Camera, Plus, Loader2, Utensils, X, Check, AlertCircle } from 'lucide-react';
import { useStore } from '../store';
import { GlassCard } from '../components/UI';

const Nutrition: React.FC = () => {
  const user = useStore(state => state.user);
  const meals = useStore(state => state.meals);
  
  // Global Analysis State
  const isAnalyzing = useStore(state => state.isAnalyzing);
  const performAnalysis = useStore(state => state.performFoodAnalysis);
  const analysisError = useStore(state => state.analysisError);
  const clearError = useStore(state => state.clearAnalysisError);
  
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onloadend = () => {
      const base64 = reader.result as string;
      setPreview(base64);
    };
    reader.readAsDataURL(file);
    e.target.value = ''; // Reset input
  };

  const confirmAnalysis = () => {
    if (!preview) return;
    
    // Start background analysis
    performAnalysis(preview);
    
    // Close modal immediately so user can do other things
    setPreview(null);
  };

  const cancelPreview = () => {
    setPreview(null);
  };

  const totalCals = meals.reduce((acc, m) => acc + m.calories, 0);

  return (
    <div className="pb-24 pt-8 px-6 space-y-6 relative">
       <header>
          <h1 className="text-3xl font-bold text-white">Nutrition</h1>
          <p className="text-white/50 text-sm">AI Food Analysis</p>
      </header>
      
      {/* Error Alert */}
      <AnimatePresence>
        {analysisError && (
          <motion.div 
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="bg-red-500/10 border border-red-500/20 p-4 rounded-2xl flex items-start gap-3 relative"
          >
             <AlertCircle className="text-red-500 shrink-0" size={20} />
             <div className="flex-1">
                <p className="text-sm text-red-200 font-bold mb-1">Analysis Failed</p>
                <p className="text-xs text-red-200/70">{analysisError}</p>
             </div>
             <button onClick={clearError} className="p-1 hover:bg-white/10 rounded-full">
               <X size={16} className="text-white/50" />
             </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Confirmation Modal Overlay */}
      <AnimatePresence>
        {preview && (
          <div className="fixed inset-0 z-50 bg-black/95 backdrop-blur-xl flex flex-col items-center justify-center p-6">
             <motion.div 
               initial={{ opacity: 0, scale: 0.9 }}
               animate={{ opacity: 1, scale: 1 }}
               exit={{ opacity: 0, scale: 0.9 }}
               className="w-full max-w-sm flex flex-col items-center"
             >
                <h2 className="text-2xl font-bold text-white mb-6">Confirm Photo</h2>
                
                <div className="relative w-full aspect-square rounded-3xl overflow-hidden mb-8 border border-white/10 shadow-2xl bg-white/5">
                   <img src={preview} alt="Preview" className="w-full h-full object-cover" />
                </div>

                <div className="flex gap-4 w-full">
                   <button 
                     onClick={cancelPreview}
                     className="flex-1 py-4 rounded-2xl bg-white/10 text-white font-bold flex items-center justify-center gap-2 hover:bg-white/20 transition-colors"
                   >
                     <X size={20} /> Retake
                   </button>
                   <button 
                     onClick={confirmAnalysis}
                     className="flex-1 py-4 rounded-2xl bg-[#F6822B] text-white font-bold flex items-center justify-center gap-2 shadow-lg shadow-orange-500/20 hover:bg-[#ff9e57] transition-colors"
                   >
                     <Check size={20} /> Analyze
                   </button>
                </div>
                <p className="text-white/30 text-xs mt-6 text-center">
                  You can leave the app while AI analyzes this image.
                </p>
             </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Summary */}
      <GlassCard className="flex items-center justify-between">
         <div>
            <p className="text-white/50 text-xs uppercase font-bold">Today's Calories</p>
            <p className="text-3xl font-bold text-[#F6822B]">{Math.round(totalCals)} <span className="text-white text-sm font-normal">/ {user?.calorieGoal}</span></p>
         </div>
         <div 
           className="w-12 h-12 bg-[#F6822B] rounded-full flex items-center justify-center cursor-pointer shadow-lg shadow-orange-500/20 active:scale-95 transition-transform"
           onClick={() => fileInputRef.current?.click()}
         >
           <Plus size={24} className="text-white" />
         </div>
         <input 
           type="file" 
           ref={fileInputRef} 
           className="hidden" 
           accept="image/*"
           onChange={handleImageUpload}
         />
      </GlassCard>

      {/* Meals List */}
      <div className="space-y-4">
        <h3 className="text-lg font-bold text-white">Today's Meals</h3>
        
        {/* Background Analysis Loading State */}
        <AnimatePresence>
          {isAnalyzing && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
            >
              <GlassCard className="flex gap-4 items-center bg-[#F6822B]/5 border-[#F6822B]/20 relative overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent skew-x-12 animate-pulse" />
                <div className="w-16 h-16 rounded-xl bg-[#F6822B]/10 flex items-center justify-center shrink-0">
                  <Loader2 className="animate-spin text-[#F6822B]" size={24} />
                </div>
                <div className="flex-1">
                  <h4 className="font-bold text-[#F6822B] animate-pulse">Analyzing Food...</h4>
                  <p className="text-xs text-white/50">Identifying macros & calories</p>
                </div>
              </GlassCard>
            </motion.div>
          )}
        </AnimatePresence>

        {meals.length === 0 && !isAnalyzing ? (
          <div className="text-center py-10 text-white/30">
            <Camera size={48} className="mx-auto mb-2 opacity-50"/>
            <p>No meals tracked yet.</p>
            <p className="text-xs">Tap + to snap a photo</p>
          </div>
        ) : (
          meals.map(meal => (
            <GlassCard key={meal.id} className="flex gap-4 items-center">
              {meal.image ? (
                <img src={meal.image} alt={meal.name} className="w-16 h-16 rounded-xl object-cover bg-white/10" />
              ) : (
                <div className="w-16 h-16 rounded-xl bg-white/10 flex items-center justify-center">
                  <Utensils size={20} className="text-white/50" />
                </div>
              )}
              <div className="flex-1">
                <h4 className="font-bold text-white">{meal.name}</h4>
                <p className="text-xs text-white/50 capitalize">{meal.type} • {new Date(meal.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</p>
                <div className="flex gap-2 mt-1">
                   <span className="text-xs bg-orange-500/20 text-orange-400 px-1.5 rounded">P: {meal.protein}g</span>
                   <span className="text-xs bg-blue-500/20 text-blue-400 px-1.5 rounded">C: {meal.carbs}g</span>
                   <span className="text-xs bg-green-500/20 text-green-400 px-1.5 rounded">F: {meal.fats}g</span>
                </div>
              </div>
              <div className="text-right">
                <p className="font-bold text-[#F6822B]">{meal.calories}</p>
                <p className="text-[10px] text-white/40 uppercase">kcal</p>
              </div>
            </GlassCard>
          ))
        )}
      </div>
    </div>
  );
};

export default Nutrition;