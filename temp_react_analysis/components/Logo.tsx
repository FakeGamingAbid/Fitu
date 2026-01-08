import React from 'react';

export const Logo: React.FC<{ size?: number; className?: string }> = ({ size = 64, className = '' }) => (
  <svg 
    xmlns="http://www.w3.org/2000/svg" 
    width={size} 
    height={size} 
    viewBox="0 0 256 256" 
    className={className}
    fill="none"
  >
    <rect width="256" height="256" rx="64" fill="url(#logo_gradient)" />
    <path 
      d="M80 190V70H170M80 130H150" 
      stroke="white" 
      strokeWidth="28" 
      strokeLinecap="round" 
      strokeLinejoin="round" 
    />
    <circle cx="186" cy="70" r="16" fill="white" />
    <defs>
      <linearGradient id="logo_gradient" x1="0" y1="0" x2="256" y2="256" gradientUnits="userSpaceOnUse">
        <stop stopColor="#F6822B"/>
        <stop offset="1" stopColor="#D94F00"/>
      </linearGradient>
    </defs>
  </svg>
);
