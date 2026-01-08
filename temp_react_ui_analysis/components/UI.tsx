import React from 'react';
import { motion } from 'framer-motion';

export const GlassCard: React.FC<{ children: React.ReactNode; className?: string; onClick?: () => void }> = ({ children, className = '', onClick }) => (
  <motion.div
    whileTap={onClick ? { scale: 0.98 } : undefined}
    onClick={onClick}
    className={`bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-4 shadow-lg ${className}`}
  >
    {children}
  </motion.div>
);

export const PrimaryButton: React.FC<{ children: React.ReactNode; onClick?: () => void; disabled?: boolean; fullWidth?: boolean }> = ({ children, onClick, disabled, fullWidth }) => (
  <motion.button
    whileTap={{ scale: 0.95 }}
    onClick={onClick}
    disabled={disabled}
    className={`${fullWidth ? 'w-full' : ''} bg-[#F6822B] text-white font-bold py-4 px-6 rounded-2xl shadow-[0_4px_14px_0_rgba(246,130,43,0.39)] hover:shadow-[0_6px_20px_rgba(246,130,43,0.23)] disabled:opacity-50 disabled:cursor-not-allowed transition-all`}
  >
    {children}
  </motion.button>
);

export const GlassInput: React.FC<React.InputHTMLAttributes<HTMLInputElement>> = (props) => (
  <input
    {...props}
    className={`w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-white/30 focus:outline-none focus:border-[#F6822B] transition-colors ${props.className}`}
  />
);

export const NavIcon: React.FC<{ icon: any; label: string; active: boolean; onClick: () => void }> = ({ icon: Icon, label, active, onClick }) => (
  <button onClick={onClick} className="flex flex-col items-center justify-center space-y-1 w-16">
    <div className={`p-2 rounded-xl transition-all ${active ? 'bg-[#F6822B] text-white' : 'text-white/50 hover:bg-white/5'}`}>
      <Icon size={24} strokeWidth={active ? 2.5 : 2} />
    </div>
    <span className={`text-[10px] font-medium ${active ? 'text-white' : 'text-white/40'}`}>{label}</span>
  </button>
);

export const Toggle: React.FC<{ checked: boolean; onChange: (checked: boolean) => void; label?: string }> = ({ checked, onChange, label }) => (
  <div className="flex items-center justify-between cursor-pointer group" onClick={() => onChange(!checked)}>
    {label && <span className="text-sm font-medium text-white/70 group-hover:text-white transition-colors">{label}</span>}
    <div className={`w-12 h-7 rounded-full p-1 transition-colors duration-300 ${checked ? 'bg-[#F6822B]' : 'bg-white/10'}`}>
      <motion.div 
        className="w-5 h-5 bg-white rounded-full shadow-md"
        layout
        transition={{ type: "spring", stiffness: 700, damping: 30 }}
        style={{ x: checked ? 20 : 0 }}
      />
    </div>
  </div>
);