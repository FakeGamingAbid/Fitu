import React, { useState, useEffect } from 'react';
import { useStore } from '../store';
import { GlassCard } from '../components/UI';
import { Download, Trash2, Smartphone } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const Profile: React.FC = () => {
  const user = useStore(state => state.user);
  const reset = useStore(state => state.resetOnboarding);
  const navigate = useNavigate();

  const [installPrompt, setInstallPrompt] = useState<any>(null);

  useEffect(() => {
    const handler = (e: any) => {
      e.preventDefault();
      setInstallPrompt(e);
    };
    window.addEventListener('beforeinstallprompt', handler);
    return () => window.removeEventListener('beforeinstallprompt', handler);
  }, []);

  const handleInstall = async () => {
    if (!installPrompt) return;
    installPrompt.prompt();
    const { outcome } = await installPrompt.userChoice;
    if (outcome === 'accepted') {
      setInstallPrompt(null);
    }
  };

  const handleLogout = () => {
    if (window.confirm("Are you sure you want to reset? This will delete all your tracking data from this device.")) {
      reset();
      navigate('/');
    }
  };

  const handleExport = () => {
    const state = useStore.getState();
    const data = {
      user: state.user,
      dailyStats: state.dailyStats,
      meals: state.meals,
      workouts: state.workouts,
      lastInsightDate: state.lastInsightDate,
      exportDate: new Date().toISOString()
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `fitu-data-${new Date().toISOString().split('T')[0]}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  if (!user) return null;

  // Calculate BMI
  const heightInMeters = user.height / 100;
  const bmiValue = user.weight / (heightInMeters * heightInMeters);
  const bmi = bmiValue.toFixed(1);
  
  let bmiLabel = 'Normal';
  let bmiColor = 'text-green-500';
  
  if (bmiValue < 18.5) {
    bmiLabel = 'Underweight';
    bmiColor = 'text-blue-400';
  } else if (bmiValue >= 25 && bmiValue < 30) {
    bmiLabel = 'Overweight';
    bmiColor = 'text-orange-400';
  } else if (bmiValue >= 30) {
    bmiLabel = 'Obese';
    bmiColor = 'text-red-500';
  }

  return (
    <div className="pb-24 pt-8 px-6 space-y-6">
      <header>
          <h1 className="text-3xl font-bold text-white">Profile</h1>
      </header>

      <div className="flex items-center gap-4 mb-8">
        <div className="w-20 h-20 rounded-full bg-gradient-to-br from-[#F6822B] to-[#D94F00] flex items-center justify-center text-3xl font-bold text-white shadow-lg">
          {user.name.charAt(0)}
        </div>
        <div>
          <h2 className="text-xl font-bold text-white">{user.name}</h2>
          <p className="text-white/50 text-sm">Pro Member</p>
        </div>
      </div>

      <section className="space-y-4">
        <h3 className="text-white/50 uppercase text-xs font-bold tracking-wider ml-1">My Goals</h3>
        <GlassCard>
           <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-xs text-white/40 block mb-1">Daily Steps</label>
                <div className="text-xl font-bold">{user.stepGoal.toLocaleString()}</div>
              </div>
              <div>
                <label className="text-xs text-white/40 block mb-1">Daily Calories</label>
                <div className="text-xl font-bold">{user.calorieGoal.toLocaleString()}</div>
              </div>
           </div>
        </GlassCard>
      </section>

      <section className="space-y-4">
        <h3 className="text-white/50 uppercase text-xs font-bold tracking-wider ml-1">Body Stats</h3>
        <GlassCard className="space-y-4">
           <div className="flex justify-between items-center py-2 border-b border-white/5">
             <span className="text-white/80">BMI</span>
             <div className="text-right flex items-center gap-2">
                <span className={`text-lg font-bold ${bmiColor}`}>{bmi}</span>
                <span className={`text-[10px] px-2 py-1 rounded-full bg-white/5 ${bmiColor} uppercase font-bold tracking-wide`}>{bmiLabel}</span>
             </div>
           </div>
           <div className="flex justify-between items-center py-2 border-b border-white/5">
             <span className="text-white/80">Height</span>
             <span className="text-white/80 text-sm">{user.height} cm</span>
           </div>
           <div className="flex justify-between items-center py-2 border-b border-white/5">
             <span className="text-white/80">Weight</span>
             <span className="text-white/80 text-sm">{user.weight} kg</span>
           </div>
           <div className="flex justify-between items-center py-2">
             <span className="text-white/80">API Key</span>
             <span className="text-white/40 text-sm truncate max-w-[100px]">••••••••</span>
           </div>
        </GlassCard>
      </section>

      <section className="space-y-4">
        <h3 className="text-white/50 uppercase text-xs font-bold tracking-wider ml-1">App Settings</h3>
        <GlassCard className="space-y-1">
           {installPrompt && (
             <button 
               onClick={handleInstall}
               className="w-full flex items-center justify-between py-4 border-b border-white/5 group"
             >
                <div className="flex items-center gap-3 text-[#F6822B]">
                  <div className="p-2 rounded-lg bg-[#F6822B]/10 group-hover:bg-[#F6822B]/20 transition-colors">
                     <Smartphone size={20} />
                  </div>
                  <div className="text-left">
                    <p className="font-bold text-sm">Install App</p>
                    <p className="text-xs text-white/40">Add to home screen</p>
                  </div>
                </div>
             </button>
           )}

           <button 
             onClick={handleExport}
             className="w-full flex items-center justify-between py-4 border-b border-white/5 group"
           >
              <div className="flex items-center gap-3 text-white">
                <div className="p-2 rounded-lg bg-blue-500/10 text-blue-400 group-hover:bg-blue-500/20 transition-colors">
                   <Download size={20} />
                </div>
                <div className="text-left">
                  <p className="font-bold text-sm">Export Data</p>
                  <p className="text-xs text-white/40">Download JSON backup</p>
                </div>
              </div>
           </button>
           
           <button 
             onClick={handleLogout}
             className="w-full flex items-center justify-between py-4 group"
           >
              <div className="flex items-center gap-3 text-red-500">
                <div className="p-2 rounded-lg bg-red-500/10 group-hover:bg-red-500/20 transition-colors">
                   <Trash2 size={20} />
                </div>
                <div className="text-left">
                  <p className="font-bold text-sm">Reset App</p>
                  <p className="text-xs text-white/40 group-hover:text-red-400/60">Delete all local data</p>
                </div>
              </div>
           </button>
        </GlassCard>
      </section>
      
      <p className="text-center text-xs text-white/30 pt-4">Version 2.0.0 • Fitu PWA</p>
    </div>
  );
};

export default Profile;