import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowRight, Key, Check, User as UserIcon } from 'lucide-react';
import { useStore } from '../store';
import { GlassCard, GlassInput, PrimaryButton } from '../components/UI';
import { UserProfile } from '../types';

const Onboarding: React.FC = () => {
  const navigate = useNavigate();
  const setUser = useStore((state) => state.setUser);
  const [step, setStep] = useState(1);
  const [formData, setFormData] = useState<Partial<UserProfile>>({
    name: '',
    age: 25,
    height: 170,
    weight: 70,
    stepGoal: 10000,
    calorieGoal: 2000,
    apiKey: ''
  });

  const handleNext = () => {
    if (step === 1 && formData.name) setStep(2);
    else if (step === 2 && formData.apiKey) {
      // Clean the API Key
      const cleanedKey = formData.apiKey.trim();
      setUser({ ...formData, apiKey: cleanedKey, onboardingComplete: true });
      navigate('/dashboard');
    }
  };

  return (
    <div className="min-h-screen bg-[#0A0A0F] text-white p-6 pt-12 flex flex-col">
      {/* Progress Bar */}
      <div className="w-full h-1 bg-white/10 rounded-full mb-8">
        <motion.div 
          className="h-full bg-[#F6822B] rounded-full"
          initial={{ width: '0%' }}
          animate={{ width: step === 1 ? '50%' : '100%' }}
        />
      </div>

      <AnimatePresence mode="wait">
        {step === 1 ? (
          <motion.div 
            key="step1"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="flex-1 flex flex-col"
          >
            <h2 className="text-3xl font-bold mb-2">Tell us about you</h2>
            <p className="text-white/50 mb-8">We use this to personalize your AI coach.</p>

            <div className="space-y-4 flex-1">
              <div className="space-y-1">
                <label className="text-xs text-white/50 uppercase font-bold ml-1">Name</label>
                <GlassInput 
                  placeholder="Your name" 
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-xs text-white/50 uppercase font-bold ml-1">Height (cm)</label>
                  <GlassInput 
                    type="number"
                    value={formData.height}
                    onChange={(e) => setFormData({...formData, height: Number(e.target.value)})}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-xs text-white/50 uppercase font-bold ml-1">Weight (kg)</label>
                  <GlassInput 
                    type="number"
                    value={formData.weight}
                    onChange={(e) => setFormData({...formData, weight: Number(e.target.value)})}
                  />
                </div>
              </div>

               <div className="space-y-1">
                  <label className="text-xs text-white/50 uppercase font-bold ml-1">Daily Step Goal</label>
                  <GlassInput 
                    type="number"
                    value={formData.stepGoal}
                    onChange={(e) => setFormData({...formData, stepGoal: Number(e.target.value)})}
                  />
                </div>
            </div>

            <PrimaryButton onClick={handleNext} disabled={!formData.name} fullWidth>
              <span className="flex items-center justify-center gap-2">
                Continue <ArrowRight size={20} />
              </span>
            </PrimaryButton>
          </motion.div>
        ) : (
          <motion.div 
            key="step2"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="flex-1 flex flex-col"
          >
            <h2 className="text-3xl font-bold mb-2">Power up AI</h2>
            <p className="text-white/50 mb-8">Enter your Gemini API key to enable nutrition vision and smart coaching.</p>

            <GlassCard className="mb-6 border-[#F6822B]/30 bg-[#F6822B]/5">
              <div className="flex items-start gap-4">
                <div className="p-2 bg-[#F6822B]/20 rounded-lg text-[#F6822B]">
                  <Key size={24} />
                </div>
                <div>
                  <h3 className="font-bold text-[#F6822B]">Gemini API Required</h3>
                  <p className="text-sm text-white/60 mt-1">
                    Fitu runs entirely in your browser. Your key is stored securely on your device.
                  </p>
                </div>
              </div>
            </GlassCard>

            <div className="space-y-4 flex-1">
              <div className="space-y-1">
                <label className="text-xs text-white/50 uppercase font-bold ml-1">API Key</label>
                <GlassInput 
                  type="password"
                  placeholder="AIzaSy..." 
                  value={formData.apiKey}
                  onChange={(e) => setFormData({...formData, apiKey: e.target.value})}
                />
              </div>
              <a 
                href="https://aistudio.google.com/app/apikey" 
                target="_blank"
                rel="noreferrer"
                className="text-xs text-[#F6822B] underline text-center block"
              >
                Get a free API key here
              </a>
            </div>

            <PrimaryButton onClick={handleNext} disabled={!formData.apiKey} fullWidth>
              <span className="flex items-center justify-center gap-2">
                Start Journey <Check size={20} />
              </span>
            </PrimaryButton>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default Onboarding;