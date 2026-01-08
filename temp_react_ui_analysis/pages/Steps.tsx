import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useStore } from '../store';
import { GlassCard } from '../components/UI';
import { BarChart, Bar, ResponsiveContainer, XAxis, Tooltip, Cell } from 'recharts';
import { Play, Pause, Flame, Ruler, Zap, AlertTriangle, Smartphone, Activity, Calendar } from 'lucide-react';

const SneakerIcon = ({ className }: { className?: string }) => (
  <svg 
    xmlns="http://www.w3.org/2000/svg" 
    width="48" 
    height="48" 
    fill="currentColor" 
    viewBox="0 0 256 256"
    className={className}
  >
    <path d="M231.16,166.63l-28.63-14.31A47.74,47.74,0,0,1,176,109.39V80a8,8,0,0,0-8-8,48.05,48.05,0,0,1-48-48,8,8,0,0,0-12.83-6.37L30.13,76l-.2.16a16,16,0,0,0-1.24,23.75L142.4,213.66a8,8,0,0,0,5.66,2.34H224a16,16,0,0,0,16-16V180.94A15.92,15.92,0,0,0,231.16,166.63ZM224,200H151.37L40,88.63l12.87-9.76,38.79,38.79A8,8,0,0,0,103,106.34L65.74,69.11l40-30.31A64.15,64.15,0,0,0,160,87.5v21.89a63.65,63.65,0,0,0,35.38,57.24L224,180.94ZM70.8,184H32a8,8,0,0,1,0-16H70.8a8,8,0,1,1,0,16Zm40,24a8,8,0,0,1-8,8H48a8,8,0,0,1,0-16h54.8A8,8,0,0,1,110.8,208Z"></path>
  </svg>
);

const PermissionDeniedInstructions: React.FC = () => (
  <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-5 mb-6">
    <div className="flex items-center gap-3 mb-3 text-red-400 font-bold">
      <AlertTriangle size={20} />
      <h3>Sensors are Blocked</h3>
    </div>
    <ul className="space-y-3 text-sm text-red-100/70">
      <li className="flex gap-2">
        <span className="bg-red-500/20 text-red-400 px-1.5 py-0.5 rounded h-fit text-[10px] font-bold">iOS</span>
        <span>Go to <strong>Settings</strong> &gt; <strong>Safari</strong> &gt; <strong>Motion & Orientation</strong> &gt; <strong>ON</strong>.</span>
      </li>
      <li className="flex gap-2">
        <span className="bg-red-500/20 text-red-400 px-1.5 py-0.5 rounded h-fit text-[10px] font-bold">Android</span>
        <span>Tap <strong>Lock Icon</strong> in address bar &gt; <strong>Permissions</strong> &gt; <strong>Sensors</strong> &gt; <strong>Allow</strong>.</span>
      </li>
    </ul>
    <button 
      onClick={() => window.location.reload()}
      className="w-full mt-4 py-2 bg-red-500/20 border border-red-500/40 rounded-xl text-red-400 text-xs font-bold active:scale-95 transition-transform"
    >
      Refresh Page
    </button>
  </div>
);

const Steps: React.FC = () => {
  const dailyStats = useStore(state => state.dailyStats);
  const updateDailySteps = useStore(state => state.updateDailySteps);
  const user = useStore(state => state.user);
  const today = new Date().toISOString().split('T')[0];
  const currentStats = dailyStats[today] || { steps: 0, distance: 0, caloriesBurned: 0 };

  // UI State
  const [isTracking, setIsTracking] = useState(false);
  const [sessionSteps, setSessionSteps] = useState(0);
  const [liveIntensity, setLiveIntensity] = useState(0);
  const [permissionError, setPermissionError] = useState(false);
  const [isStepAnimating, setIsStepAnimating] = useState(false);

  // --- High Precision Algorithm Refs ---
  const stepsInSession = useRef(0);
  const lastStepTime = useRef(0);
  const gravity = useRef({ x: 0, y: 0, z: 0 });
  
  // Tuning Constants for Accuracy
  const ALPHA = 0.92;           
  const SMOOTH_FACTOR = 0.7;    
  const STEP_THRESHOLD = 2.4;   
  const RESET_THRESHOLD = 1.2;  
  const MIN_STEP_TIME = 420;    
  
  const isBelowReset = useRef(true);
  const smoothedMag = useRef(0);

  // --- Dynamic Weekly Data Calculation ---
  const weeklyActivityData = useMemo(() => {
    const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const result = [];
    const now = new Date();
    
    for (let i = 6; i >= 0; i--) {
      const d = new Date(now);
      d.setDate(now.getDate() - i);
      const iso = d.toISOString().split('T')[0];
      const dayName = days[d.getDay()];
      
      result.push({
        day: dayName,
        steps: dailyStats[iso]?.steps || 0,
        isToday: iso === today
      });
    }
    return result;
  }, [dailyStats, today]);

  const handleMotion = (event: DeviceMotionEvent) => {
    const acc = event.accelerationIncludingGravity;
    if (!acc || acc.x === null || acc.y === null || acc.z === null) return;

    gravity.current.x = ALPHA * gravity.current.x + (1 - ALPHA) * acc.x;
    gravity.current.y = ALPHA * gravity.current.y + (1 - ALPHA) * acc.y;
    gravity.current.z = ALPHA * gravity.current.z + (1 - ALPHA) * acc.z;

    const lx = acc.x - gravity.current.x;
    const ly = acc.y - gravity.current.y;
    const lz = acc.z - gravity.current.z;

    const rawMag = Math.sqrt(lx * lx + ly * ly + lz * lz);
    smoothedMag.current = (SMOOTH_FACTOR * smoothedMag.current) + ((1 - SMOOTH_FACTOR) * rawMag);
    
    if (Math.random() > 0.85) setLiveIntensity(smoothedMag.current);

    const now = Date.now();
    if (smoothedMag.current > STEP_THRESHOLD && isBelowReset.current) {
      if (now - lastStepTime.current > MIN_STEP_TIME) {
        stepsInSession.current += 1;
        lastStepTime.current = now;
        isBelowReset.current = false;
        
        setSessionSteps(stepsInSession.current);
        setIsStepAnimating(true);
        setTimeout(() => setIsStepAnimating(false), 250);

        if (typeof navigator.vibrate === 'function') navigator.vibrate(15);

        const state = useStore.getState();
        const currentTotal = state.dailyStats[today]?.steps || 0;
        state.updateDailySteps(today, currentTotal + 1);
      }
    } else if (smoothedMag.current < RESET_THRESHOLD) {
      isBelowReset.current = true;
    }
  };

  const startTracking = async () => {
    setPermissionError(false);
    if (typeof (DeviceMotionEvent as any).requestPermission === 'function') {
      try {
        const permission = await (DeviceMotionEvent as any).requestPermission();
        if (permission === 'granted') setIsTracking(true);
        else setPermissionError(true);
      } catch (e) {
        setIsTracking(true);
      }
    } else {
      setIsTracking(true);
    }
  };

  const stopTracking = () => {
    setIsTracking(false);
    setLiveIntensity(0);
  };

  useEffect(() => {
    if (isTracking) {
      window.addEventListener('devicemotion', handleMotion, true);
      return () => {
        window.removeEventListener('devicemotion', handleMotion, true);
      };
    }
  }, [isTracking]);

  return (
    <div className="pb-36 pt-8 px-6 space-y-6">
      <header className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-black text-white tracking-tight">Steps</h1>
          <p className="text-[#F6822B] text-[10px] font-black uppercase tracking-widest mt-1 flex items-center gap-1">
             <Activity size={12} /> Precision Engine V2
          </p>
        </div>
        {isTracking && (
          <div className="bg-[#F6822B]/10 border border-[#F6822B]/20 px-3 py-1.5 rounded-full flex items-center gap-2">
            <span className="w-2 h-2 bg-[#F6822B] rounded-full animate-pulse" />
            <span className="text-[10px] font-black text-[#F6822B] uppercase tracking-tighter">Active</span>
          </div>
        )}
      </header>

      {permissionError && <PermissionDeniedInstructions />}

      {/* Accuracy Monitor with NEW Phosphor Sneaker Icon */}
      <div className="relative flex justify-center py-4">
        <div className={`w-64 h-64 rounded-full border-[10px] border-white/5 flex flex-col items-center justify-center relative transition-all duration-300 shadow-[inset_0_0_40px_rgba(0,0,0,0.4)] ${isStepAnimating ? 'scale-105 border-[#F6822B]/30' : ''}`}>
           {/* Animated Step Ring */}
           {isStepAnimating && (
             <div className="absolute inset-0 rounded-full border-4 border-[#F6822B] animate-ping opacity-30" />
           )}
           
           {/* NEW ICON HERE */}
           <SneakerIcon className={`mb-2 transition-all duration-200 ${isStepAnimating ? 'text-[#F6822B] scale-110' : 'text-white/20'}`} />
           
           <p className="text-7xl font-black text-white tracking-tighter leading-none">{sessionSteps}</p>
           <p className="text-[10px] font-bold text-white/30 uppercase tracking-[0.2em] mt-2">Steps Taken</p>
        </div>
      </div>

      {/* Signal Cleanliness Visualizer */}
      {isTracking && (
        <div className="bg-white/5 p-5 rounded-3xl border border-white/10 overflow-hidden relative">
          <div className="flex justify-between items-center mb-4">
            <span className="text-[10px] font-black text-white/30 uppercase flex items-center gap-2">
              <Zap size={12} className="text-[#F6822B]" /> Motion Stability
            </span>
            <span className="text-[10px] font-mono text-[#F6822B]">{liveIntensity.toFixed(2)} m/s²</span>
          </div>
          <div className="h-2 w-full bg-white/5 rounded-full overflow-hidden">
             <div 
               className={`h-full transition-all duration-200 ease-out ${liveIntensity > STEP_THRESHOLD ? 'bg-red-500' : 'bg-[#F6822B]'}`}
               style={{ width: `${Math.min(100, (liveIntensity / 6) * 100)}%` }}
             />
          </div>
        </div>
      )}

      {/* Controls */}
      <div className="flex gap-4">
        <button
          onClick={isTracking ? stopTracking : startTracking}
          className={`flex-1 py-5 rounded-3xl font-black text-lg flex items-center justify-center gap-3 transition-all active:scale-95 shadow-2xl ${
            isTracking 
            ? 'bg-white/10 text-white border border-white/10' 
            : 'bg-[#F6822B] text-white shadow-orange-500/30'
          }`}
        >
          {isTracking ? <><Pause fill="currentColor" size={24}/> Pause session</> : <><Play fill="currentColor" size={24}/> Start Tracking</>}
        </button>
      </div>

      {/* Metrics Row - Removed Duration */}
      <div className="grid grid-cols-2 gap-3">
        <GlassCard className="flex flex-col items-center py-5">
          <Ruler size={20} className="text-purple-400 mb-2" />
          <span className="text-lg font-black text-white leading-none">{((sessionSteps * 0.762)/1000).toFixed(2)}</span>
          <span className="text-[8px] text-white/40 uppercase font-black mt-2">Km</span>
        </GlassCard>
        <GlassCard className="flex flex-col items-center py-5">
          <Flame size={20} className="text-red-500 mb-2" />
          <span className="text-lg font-black text-white leading-none">{Math.round(sessionSteps * 0.04)}</span>
          <span className="text-[8px] text-white/40 uppercase font-black mt-2">Kcal</span>
        </GlassCard>
      </div>

      {/* Today Overview */}
      <GlassCard className="relative overflow-hidden !p-6 border-[#F6822B]/20">
        <div className="flex justify-between items-center">
          <div>
            <h3 className="text-white/40 text-[10px] font-black uppercase tracking-widest mb-1 flex items-center gap-1">
              <Calendar size={10} /> Today's Goal
            </h3>
            <p className="text-3xl font-black text-white leading-tight">
              {currentStats.steps.toLocaleString()} 
              <span className="text-white/20 text-sm font-bold ml-2">/ {user?.stepGoal}</span>
            </p>
          </div>
          <div className="relative w-14 h-14 flex items-center justify-center">
             <svg className="absolute inset-0 w-full h-full -rotate-90">
                <circle cx="28" cy="28" r="24" fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="6" />
                <circle 
                  cx="28" cy="28" r="24" fill="none" stroke="#F6822B" strokeWidth="6" 
                  strokeDasharray={`${2 * Math.PI * 24}`}
                  strokeDashoffset={`${2 * Math.PI * 24 * (1 - Math.min(1, currentStats.steps / (user?.stepGoal || 10000)))}`}
                  strokeLinecap="round"
                />
             </svg>
             <span className="text-[10px] font-black text-white">
                {Math.min(100, Math.round((currentStats.steps / (user?.stepGoal || 10000)) * 100))}%
             </span>
          </div>
        </div>
      </GlassCard>

      {/* Weekly Activity Visual */}
      <section className="space-y-4 pt-4">
        <div className="flex justify-between items-end">
          <h3 className="text-white font-bold text-sm tracking-tight flex items-center gap-2">
            <Calendar size={16} className="text-[#F6822B]" /> Weekly Activity
          </h3>
          <span className="text-[10px] text-white/30 font-bold uppercase tracking-widest">Steps</span>
        </div>
        
        <div className="h-44 w-full bg-white/5 rounded-3xl p-5 border border-white/5">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={weeklyActivityData}>
              <XAxis 
                dataKey="day" 
                stroke="#ffffff20" 
                fontSize={10} 
                tickLine={false} 
                axisLine={false} 
                tick={{ fill: '#ffffff40', fontWeight: 700 }}
                dy={10}
              />
              <Tooltip 
                cursor={{ fill: 'rgba(255,255,255,0.05)', radius: 8 }}
                contentStyle={{ 
                  backgroundColor: '#1C1C24', 
                  border: '1px solid rgba(255,255,255,0.1)', 
                  borderRadius: '16px', 
                  fontSize: '12px',
                  fontWeight: 'bold',
                  boxShadow: '0 10px 30px rgba(0,0,0,0.5)'
                }}
                itemStyle={{ color: '#F6822B' }}
              />
              <Bar dataKey="steps" radius={[6, 6, 6, 6]} barSize={24}>
                {weeklyActivityData.map((entry, index) => (
                  <Cell 
                    key={`cell-${index}`} 
                    fill={entry.isToday ? '#F6822B' : '#F6822B40'} 
                    className="transition-all duration-500"
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>
      
      {!('ondevicemotion' in window) && (
        <div className="flex items-center gap-4 p-5 rounded-3xl bg-blue-500/10 border border-blue-500/20">
          <Smartphone className="text-blue-400 shrink-0" size={24} />
          <div>
             <p className="text-xs font-black text-blue-400 uppercase tracking-wider">Mobile Sensor Required</p>
             <p className="text-[10px] text-blue-200/50 mt-1 leading-tight">Step tracking requires hardware accelerometers. Please open Fitu on your phone!</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default Steps;