import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Footprints, Flame, Utensils, Moon, X, Sparkles, Scale, Loader2 } from 'lucide-react';
import { useStore } from '../store';
import { GlassCard } from '../components/UI';
import { PieChart, Pie, Cell, ResponsiveContainer } from 'recharts';
import { useNavigate } from 'react-router-dom';
import { generateDailyRecap } from '../services/geminiService';

const Dashboard: React.FC = () => {
  const user = useStore(state => state.user);
  const dailyStats = useStore(state => state.dailyStats);
  const meals = useStore(state => state.meals);
  const workouts = useStore(state => state.workouts);
  const lastInsightDate = useStore(state => state.lastInsightDate);
  const markInsightSeen = useStore(state => state.markInsightSeen);
  const isAnalyzingNutrition = useStore(state => state.isAnalyzing);
  
  const navigate = useNavigate();
  const [showRecap, setShowRecap] = useState(false);
  const [recapMessage, setRecapMessage] = useState("");
  const [loadingRecap, setLoadingRecap] = useState(false);

  const todayIso = new Date().toISOString().split('T')[0];
  const todayLocal = new Date().toDateString();

  const stats = dailyStats[todayIso] || { steps: 0, caloriesBurned: 0, activeMinutes: 0 };
  
  // Calculate Totals
  const todaysMeals = meals.filter(m => new Date(m.timestamp).toDateString() === todayLocal);
  const caloriesIn = todaysMeals.reduce((acc, m) => acc + m.calories, 0);

  const todaysWorkouts = workouts.filter(w => new Date(w.timestamp).toDateString() === todayLocal);
  const workoutCalories = todaysWorkouts.reduce((acc, w) => acc + w.calories, 0);
  
  const totalBurned = stats.caloriesBurned + workoutCalories;
  const netCalories = caloriesIn - totalBurned;
  
  const stepProgress = Math.min(100, (stats.steps / (user?.stepGoal || 10000)) * 100);
  const caloriesBurnedProgress = Math.min(100, (totalBurned / (user?.calorieGoal || 2000)) * 100);

  // Check for End of Day Recap (After 8 PM)
  useEffect(() => {
    const checkTime = async () => {
      const now = new Date();
      const currentHour = now.getHours(); // 0-23
      
      // Trigger if it's after 8 PM (20:00) AND we haven't shown it today
      if (currentHour >= 20 && lastInsightDate !== todayIso && user?.apiKey) {
        setShowRecap(true);
        setLoadingRecap(true);
        
        try {
          const msg = await generateDailyRecap(user.apiKey, user.name, {
             steps: stats.steps,
             calories: Math.floor(totalBurned),
             activeMinutes: stats.activeMinutes
          });
          setRecapMessage(msg);
        } catch (err) {
          setRecapMessage("Great work today! Rest up for tomorrow.");
        } finally {
          setLoadingRecap(false);
        }
      }
    };
    
    checkTime();
  }, [lastInsightDate, todayIso, user, stats, totalBurned]);

  const handleCloseRecap = () => {
    setShowRecap(false);
    markInsightSeen(todayIso);
  };

  const StatRing = ({ percent, color, children }: { percent: number; color: string; children?: React.ReactNode }) => {
    const data = [{ value: percent }, { value: 100 - percent }];
    return (
      <div className="relative w-24 h-24 flex items-center justify-center">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              innerRadius={36}
              outerRadius={42}
              startAngle={90}
              endAngle={-270}
              dataKey="value"
              stroke="none"
            >
              <Cell fill={color} />
              <Cell fill="#ffffff10" />
            </Pie>
          </PieChart>
        </ResponsiveContainer>
        <div className="absolute inset-0 flex items-center justify-center text-white">
          {children}
        </div>
      </div>
    );
  };

  return (
    <div className="pb-24 pt-8 px-6 space-y-6 relative">
      <header className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-white">Hello, {user?.name?.split(' ')[0]}</h1>
          <p className="text-white/50 text-sm">{new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}</p>
        </div>
        <div 
            onClick={() => navigate('/profile')}
            className="w-10 h-10 rounded-full bg-gradient-to-br from-[#F6822B] to-[#D94F00] flex items-center justify-center text-sm font-bold cursor-pointer"
        >
          {user?.name?.charAt(0)}
        </div>
      </header>

      {/* Main Steps Card */}
      <GlassCard className="relative overflow-hidden" onClick={() => navigate('/steps')}>
        <div className="flex justify-between items-center">
          <div>
            <h3 className="text-white/70 font-medium mb-1 flex items-center gap-2"><Footprints size={16}/> Steps</h3>
            <p className="text-4xl font-bold font-sans">{stats.steps.toLocaleString()}</p>
            <p className="text-xs text-white/40 mt-1">Goal: {user?.stepGoal?.toLocaleString()}</p>
          </div>
          <StatRing percent={stepProgress} color="#F6822B">
             <span className="text-xs font-bold">{Math.round(stepProgress)}%</span>
          </StatRing>
        </div>
      </GlassCard>

      <GlassCard>
          <h3 className="text-white/70 font-medium mb-2 flex items-center gap-2 text-sm"><Flame size={14} className="text-red-500"/> Burned</h3>
          <p className="text-2xl font-bold">{Math.round(totalBurned)}</p>
          <div className="w-full bg-white/10 h-1 mt-3 rounded-full overflow-hidden">
              <div className="h-full bg-red-500 rounded-full" style={{ width: `${caloriesBurnedProgress}%` }}></div>
          </div>
      </GlassCard>

      <GlassCard onClick={() => navigate('/nutrition')}>
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-bold flex items-center gap-2">
              <Utensils size={18} className="text-green-500"/> Nutrition
            </h3>
            {isAnalyzingNutrition ? (
               <div className="flex items-center gap-2 bg-[#F6822B]/20 text-[#F6822B] px-2 py-1 rounded-full text-xs font-bold animate-pulse">
                  <Loader2 size={12} className="animate-spin" /> Analyzing
               </div>
            ) : (
               <span className="text-xs text-white/50 bg-white/10 px-2 py-1 rounded-full">Track</span>
            )}
          </div>
          
          {caloriesIn > 0 ? (
             <div className="flex items-center justify-between">
                <div>
                   <p className="text-xs text-white/50 mb-1">Eaten</p>
                   <p className="text-2xl font-bold text-green-500">{Math.round(caloriesIn)}</p>
                </div>
                <div className="h-8 w-[1px] bg-white/10"></div>
                <div>
                   <p className="text-xs text-white/50 mb-1">Net</p>
                   <p className={`text-2xl font-bold ${netCalories > 0 ? 'text-white' : 'text-[#F6822B]'}`}>
                     {netCalories > 0 ? '+' : ''}{Math.round(netCalories)}
                   </p>
                </div>
             </div>
          ) : (
            <p className="text-sm text-white/60">
               {isAnalyzingNutrition ? "AI is reviewing your food..." : "Track your meals with AI vision."}
            </p>
          )}
      </GlassCard>

      <GlassCard className="bg-gradient-to-br from-[#F6822B]/20 to-transparent border-[#F6822B]/30" onClick={() => navigate('/coach')}>
         <div className="flex flex-col items-center py-4 text-center">
            <h3 className="text-xl font-bold text-white mb-2">AI Workout Coach</h3>
            <p className="text-sm text-white/70 mb-4">Real-time form correction & rep counting</p>
            <button className="bg-[#F6822B] text-white px-6 py-2 rounded-full font-bold text-sm shadow-lg">Start Workout</button>
         </div>
      </GlassCard>

      {/* End of Day Recap Modal */}
      <AnimatePresence>
        {showRecap && (
          <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
             <motion.div 
               initial={{ opacity: 0 }}
               animate={{ opacity: 1 }}
               exit={{ opacity: 0 }}
               className="absolute inset-0 bg-black/80 backdrop-blur-sm"
               onClick={handleCloseRecap}
             />
             <motion.div
               initial={{ scale: 0.9, opacity: 0, y: 20 }}
               animate={{ scale: 1, opacity: 1, y: 0 }}
               exit={{ scale: 0.9, opacity: 0, y: 20 }}
               className="relative bg-[#1C1C24] border border-white/10 w-full max-w-sm rounded-3xl p-6 shadow-2xl overflow-hidden"
             >
                {/* Decorative BG */}
                <div className="absolute top-0 right-0 w-32 h-32 bg-blue-500/20 rounded-full blur-[50px] pointer-events-none" />
                <div className="absolute bottom-0 left-0 w-32 h-32 bg-[#F6822B]/20 rounded-full blur-[50px] pointer-events-none" />

                <div className="relative z-10 text-center">
                   <div className="w-12 h-12 rounded-full bg-blue-500/20 flex items-center justify-center mx-auto mb-4 text-blue-400">
                      <Moon size={24} />
                   </div>
                   <h2 className="text-2xl font-bold text-white mb-1">Daily Recap</h2>
                   <p className="text-white/40 text-sm mb-6">{new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}</p>

                   {loadingRecap ? (
                      <div className="py-8 flex flex-col items-center gap-3">
                         <Sparkles className="animate-spin text-[#F6822B]" size={24} />
                         <p className="text-sm text-white/50">Consulting AI Coach...</p>
                      </div>
                   ) : (
                      <div className="bg-white/5 rounded-2xl p-4 mb-6 text-sm text-white/90 leading-relaxed italic">
                        "{recapMessage}"
                      </div>
                   )}

                   <div className="grid grid-cols-3 gap-2 mb-6">
                      <div className="bg-black/20 p-2 rounded-xl">
                         <div className="text-xs text-white/40 mb-1">Steps</div>
                         <div className="font-bold text-[#F6822B]">{stats.steps}</div>
                      </div>
                      <div className="bg-black/20 p-2 rounded-xl">
                         <div className="text-xs text-white/40 mb-1">Kcal</div>
                         <div className="font-bold text-red-500">{Math.round(totalBurned)}</div>
                      </div>
                      <div className="bg-black/20 p-2 rounded-xl">
                         <div className="text-xs text-white/40 mb-1">Active</div>
                         <div className="font-bold text-blue-500">{stats.activeMinutes}m</div>
                      </div>
                   </div>

                   <button 
                     onClick={handleCloseRecap}
                     className="w-full py-3 bg-white text-black font-bold rounded-xl active:scale-95 transition-transform"
                   >
                     Good Night
                   </button>
                </div>

                <button 
                   onClick={handleCloseRecap}
                   className="absolute top-4 right-4 p-2 bg-white/5 rounded-full text-white/50 hover:text-white"
                >
                   <X size={20} />
                </button>
             </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default Dashboard;