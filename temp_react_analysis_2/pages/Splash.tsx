import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useStore } from '../store';

const Splash: React.FC = () => {
  const navigate = useNavigate();
  const user = useStore((state) => state.user);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (user?.onboardingComplete) {
        navigate('/dashboard');
      } else {
        navigate('/onboarding');
      }
    }, 2500);

    return () => clearTimeout(timer);
  }, [user, navigate]);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-[#0A0A0F] relative overflow-hidden">
      {/* Background Glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-64 h-64 bg-[#F6822B] rounded-full blur-[100px] opacity-20" />
      
      <motion.div
        initial={{ scale: 0.5, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.5, type: 'spring' }}
        className="relative z-10 flex flex-col items-center"
      >
        <div className="mb-6 shadow-2xl shadow-orange-500/20 rounded-[2rem]">
          <img src="/logo.svg" alt="Fitu" className="w-20 h-20" />
        </div>
        <h1 className="text-4xl font-extrabold tracking-tight text-white mb-2">Fitu</h1>
        <p className="text-white/50 font-medium tracking-widest text-sm uppercase">AI Fitness Coach</p>
      </motion.div>

      <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 1 }}
        className="absolute bottom-10 text-white/30 text-xs"
      >
        Powered by Gemini
      </motion.div>
    </div>
  );
};

export default Splash;