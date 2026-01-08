import React from 'react';
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout';
import Onboarding from './pages/Onboarding';
import Dashboard from './pages/Dashboard';
import Steps from './pages/Steps';
import AICoach from './pages/AICoach';
import WorkoutSession from './pages/WorkoutSession';
import WorkoutGenerator from './pages/WorkoutGenerator';
import Nutrition from './pages/Nutrition';
import Profile from './pages/Profile';
import { useStore } from './store';

// Handles redirect logic after state hydration is complete
const RootRedirect: React.FC = () => {
  const user = useStore((state) => state.user);
  const hasHydrated = useStore((state) => state._hasHydrated);

  // Show a minimal loader while IndexedDB hydrates the store
  if (!hasHydrated) {
     return (
       <div className="min-h-screen bg-[#0A0A0F] flex items-center justify-center">
          <div className="w-10 h-10 border-4 border-[#F6822B] border-t-transparent rounded-full animate-spin"></div>
       </div>
     );
  }

  // Direct routing based on user status
  return user?.onboardingComplete ? <Navigate to="/dashboard" replace /> : <Navigate to="/onboarding" replace />;
};

const App: React.FC = () => {
  return (
    <HashRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<RootRedirect />} />
          <Route path="/onboarding" element={<Onboarding />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/steps" element={<Steps />} />
          <Route path="/coach" element={<AICoach />} />
          <Route path="/generator" element={<WorkoutGenerator />} />
          <Route path="/workout/:type" element={<WorkoutSession />} />
          <Route path="/nutrition" element={<Nutrition />} />
          <Route path="/profile" element={<Profile />} />
        </Route>
      </Routes>
    </HashRouter>
  );
};

export default App;
