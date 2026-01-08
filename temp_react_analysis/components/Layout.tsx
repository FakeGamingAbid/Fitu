import React, { useState, useEffect } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { NavIcon, GlassCard } from './UI';
import { LayoutDashboard, Footprints, Dumbbell, Apple, User, WifiOff } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

export const Layout: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const path = location.pathname;
  
  const [isOffline, setIsOffline] = useState(!navigator.onLine);

  useEffect(() => {
    const handleOnline = () => setIsOffline(false);
    const handleOffline = () => setIsOffline(true);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Hide nav on onboarding and active workout session
  if (path === '/onboarding' || path.startsWith('/workout')) {
    return (
      <>
        <AnimatePresence>
          {isOffline && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="bg-red-500/90 backdrop-blur-md text-white text-xs font-bold text-center overflow-hidden z-50 fixed top-0 w-full"
            >
              <div className="py-2 flex items-center justify-center gap-2">
                <WifiOff size={12} />
                Offline Mode • AI Features Paused
              </div>
            </motion.div>
          )}
        </AnimatePresence>
        <Outlet />
      </>
    );
  }

  return (
    <div className="fixed inset-0 bg-[#0A0A0F] text-white flex justify-center overflow-hidden">
      <div className="w-full max-w-md h-full relative flex flex-col">
        {/* Offline Banner */}
        <AnimatePresence>
          {isOffline && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="bg-red-500/90 backdrop-blur-md text-white text-xs font-bold text-center overflow-hidden z-50"
            >
              <div className="py-2 flex items-center justify-center gap-2">
                <WifiOff size={12} />
                Offline Mode • AI Features Paused
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Scrollable Main Content */}
        <main className="flex-1 w-full overflow-y-auto overflow-x-hidden no-scrollbar pb-4">
          <Outlet />
        </main>
        
        {/* Floating Navigation */}
        <div className="absolute bottom-0 left-0 right-0 z-40 p-4 pointer-events-none">
          <div className="pointer-events-auto">
            <GlassCard className="!p-2 !rounded-3xl flex justify-between items-center shadow-2xl bg-[#121218]/90 backdrop-blur-xl border-white/5">
              <NavIcon 
                icon={LayoutDashboard} 
                label="Home" 
                active={path === '/dashboard'} 
                onClick={() => navigate('/dashboard')} 
              />
              <NavIcon 
                icon={Footprints} 
                label="Steps" 
                active={path === '/steps'} 
                onClick={() => navigate('/steps')} 
              />
              <div className="relative -top-6">
                 <button 
                   onClick={() => navigate('/coach')}
                   className={`w-14 h-14 rounded-full flex items-center justify-center shadow-[0_0_20px_rgba(246,130,43,0.5)] border-4 border-[#0A0A0F] transform transition-transform active:scale-95 ${path === '/coach' ? 'bg-white text-[#F6822B]' : 'bg-[#F6822B] text-white'}`}
                 >
                   <Dumbbell size={24} fill="currentColor" />
                 </button>
              </div>
              <NavIcon 
                icon={Apple} 
                label="Food" 
                active={path === '/nutrition'} 
                onClick={() => navigate('/nutrition')} 
              />
              <NavIcon 
                icon={User} 
                label="Profile" 
                active={path === '/profile'} 
                onClick={() => navigate('/profile')} 
              />
            </GlassCard>
          </div>
        </div>
      </div>
    </div>
  );
};