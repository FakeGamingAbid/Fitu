import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Play, Activity, TrendingUp, History, Dumbbell, Sparkles } from 'lucide-react';
import { useStore } from '../store';
import { GlassCard } from '../components/UI';

const AICoach: React.FC = () => {
  const navigate = useNavigate();
  const workouts = useStore(state => state.workouts);
  
  // Group workouts by day
  const recentWorkouts = workouts.slice(0, 5);

  const exercises = [
    {
      id: 'squat',
      name: 'Squats',
      description: 'Lower body strength & glutes',
      color: 'from-orange-500 to-red-500',
      icon: Dumbbell,
      duration: '5-10 min'
    },
    {
      id: 'pushup',
      name: 'Pushups',
      description: 'Upper body & core stability',
      color: 'from-blue-500 to-purple-500',
      icon: Activity,
      duration: '5-15 min'
    },
    {
      id: 'jumping_jack',
      name: 'Jumping Jacks',
      description: 'Cardio & endurance',
      color: 'from-green-500 to-teal-500',
      icon: TrendingUp,
      duration: '10-20 min'
    }
  ];

  return (
    <div className="pb-24 pt-8 px-6 space-y-6">
      <header>
        <h1 className="text-3xl font-bold text-white">AI Coach</h1>
        <p className="text-white/50 text-sm">Real-time form correction</p>
      </header>

      {/* Generator Banner */}
      <div 
        onClick={() => navigate('/generator')}
        className="w-full bg-gradient-to-r from-purple-600 to-blue-600 rounded-3xl p-6 relative overflow-hidden shadow-lg shadow-purple-900/40 cursor-pointer group active:scale-98 transition-transform"
      >
        <div className="absolute top-0 right-0 w-32 h-32 bg-white/20 rounded-full blur-2xl -translate-y-1/2 translate-x-1/2" />
        <div className="relative z-10">
           <div className="flex items-center gap-2 mb-2 text-white">
              <Sparkles size={20} className="text-yellow-300" />
              <span className="font-bold text-xs uppercase tracking-wider bg-white/20 px-2 py-0.5 rounded-full">New</span>
           </div>
           <h3 className="text-xl font-bold text-white mb-1">Generate Custom Plan</h3>
           <p className="text-white/80 text-sm max-w-[80%]">Let AI build a routine based on your equipment and time.</p>
        </div>
      </div>

      {/* Featured Workout Selection */}
      <section className="space-y-4">
         <h3 className="text-white/50 uppercase text-xs font-bold tracking-wider ml-1">Start Workout</h3>
         <div className="grid gap-4">
            {exercises.map((ex) => (
              <div 
                key={ex.id}
                onClick={() => navigate(`/workout/${ex.id}`)}
                className="group relative overflow-hidden rounded-3xl h-32 cursor-pointer transition-all active:scale-98"
              >
                 <div className={`absolute inset-0 bg-gradient-to-r ${ex.color} opacity-20 group-hover:opacity-30 transition-opacity`} />
                 <div className="absolute inset-0 border border-white/10 rounded-3xl" />
                 
                 <div className="relative h-full p-6 flex items-center justify-between">
                    <div>
                       <div className="flex items-center gap-2 mb-1">
                          <ex.icon size={18} className="text-white" />
                          <h4 className="text-xl font-bold text-white">{ex.name}</h4>
                       </div>
                       <p className="text-white/60 text-sm">{ex.description}</p>
                       <p className="text-white/40 text-xs mt-2 flex items-center gap-1">
                          <History size={10} /> {ex.duration}
                       </p>
                    </div>
                    <div className="w-12 h-12 rounded-full bg-white/10 flex items-center justify-center backdrop-blur-md group-hover:bg-white/20 transition-colors">
                       <Play size={24} fill="white" className="ml-1" />
                    </div>
                 </div>
              </div>
            ))}
         </div>
      </section>

      {/* Recent Activity */}
      <section className="space-y-4">
        <h3 className="text-white/50 uppercase text-xs font-bold tracking-wider ml-1">Recent Sessions</h3>
        {recentWorkouts.length > 0 ? (
          <div className="space-y-3">
            {recentWorkouts.map((workout) => (
              <GlassCard key={workout.id} className="flex items-center justify-between !py-3">
                 <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center text-white/50">
                       {workout.type === 'squat' && <Dumbbell size={18}/>}
                       {workout.type === 'pushup' && <Activity size={18}/>}
                       {workout.type === 'jumping_jack' && <TrendingUp size={18}/>}
                    </div>
                    <div>
                       <p className="font-bold text-white capitalize">{workout.type.replace('_', ' ')}</p>
                       <p className="text-xs text-white/40">{new Date(workout.timestamp).toLocaleDateString()}</p>
                    </div>
                 </div>
                 <div className="text-right">
                    <p className="font-bold text-[#F6822B]">{workout.reps} <span className="text-xs text-white/40 font-normal">reps</span></p>
                    <p className="text-xs text-white/40">{Math.round(workout.calories)} kcal</p>
                 </div>
              </GlassCard>
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-white/30 bg-white/5 rounded-2xl border border-white/5">
             <p className="text-sm">No recent workouts</p>
          </div>
        )}
      </section>
    </div>
  );
};

export default AICoach;
