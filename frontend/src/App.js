import React, { useState } from 'react';
import { AppProvider, useApp } from './context/AppContext';
import Dashboard from './pages/Dashboard';
import Devices from './pages/Devices';
import Terminal from './pages/Terminal';
import Logs from './pages/Logs';

// Inline Icons (Lucide fallback)
import { LayoutDashboard, Router, Terminal as TerminalIcon, Scroll, Radio, Power } from 'lucide-react';

const RouterControlApp = () => {
  const [activeTab, setActiveTab] = useState('dashboard');
  const { connectionStatus, activeDevice, toast, disconnect } = useApp();

  return (
    <div className="min-h-screen bg-[#f4f6f9] text-[#1a1a1a] flex flex-col font-sans">
      {/* Top Bar Navigation */}
      <header className="bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center sticky top-0 z-40 shadow-sm">
        <div className="flex items-center space-x-3">
          <span className="font-bold text-[#1e3a5f] text-lg tracking-tight">RouterControl</span>
          
          {/* Connection status LED badge */}
          <div className="flex items-center space-x-2 bg-gray-50 border border-gray-100 rounded-full px-3 py-1">
            <span className={`w-2.5 h-2.5 rounded-full ${
              connectionStatus === 'connecting' ? 'animate-pulse bg-[#ffb347]' :
              connectionStatus === 'connected' ? 'bg-[#2ecc71]' :
              connectionStatus === 'error' ? 'bg-[#e74c3c]' : 'bg-gray-400'
            }`} />
            <span className="text-xs font-semibold text-gray-600 uppercase tracking-wider">
              {connectionStatus === 'connected' ? activeDevice?.name : 
               connectionStatus === 'error' ? 'Connection Error' : connectionStatus}
            </span>
          </div>
        </div>

        {connectionStatus === 'connected' && (
          <button
            onClick={disconnect}
            className="flex items-center space-x-1.5 bg-[#e74c3c] hover:bg-red-600 text-white text-xs font-semibold px-3 py-1.5 rounded-lg shadow transition-colors"
          >
            <Power size={14} />
            <span>Exit Session</span>
          </button>
        )}
      </header>

      {/* Main View Container */}
      <main className="flex-1 max-w-lg w-full mx-auto p-4 pb-24">
        {activeTab === 'dashboard' && <Dashboard />}
        {activeTab === 'devices' && <Devices />}
        {activeTab === 'terminal' && <Terminal />}
        {activeTab === 'logs' && <Logs />}
      </main>

      {/* Persistent Bottom Tab Bar */}
      <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 flex justify-around py-3 z-50 shadow-[0_-2px_10px_rgba(0,0,0,0.03)]">
        <button
          onClick={() => setActiveTab('dashboard')}
          className={`flex flex-col items-center space-y-1 text-xs font-medium transition-colors ${
            activeTab === 'dashboard' ? 'text-[#1e3a5f]' : 'text-gray-400 hover:text-gray-600'
          }`}
        >
          <LayoutDashboard size={20} />
          <span>Dashboard</span>
        </button>

        <button
          onClick={() => setActiveTab('devices')}
          className={`flex flex-col items-center space-y-1 text-xs font-medium transition-colors ${
            activeTab === 'devices' ? 'text-[#1e3a5f]' : 'text-gray-400 hover:text-gray-600'
          }`}
        >
          <Router size={20} />
          <span>Devices</span>
        </button>

        <button
          onClick={() => setActiveTab('terminal')}
          className={`flex flex-col items-center space-y-1 text-xs font-medium transition-colors ${
            activeTab === 'terminal' ? 'text-[#1e3a5f]' : 'text-gray-400 hover:text-gray-600'
          }`}
        >
          <TerminalIcon size={20} />
          <span>Terminal</span>
        </button>

        <button
          onClick={() => setActiveTab('logs')}
          className={`flex flex-col items-center space-y-1 text-xs font-medium transition-colors ${
            activeTab === 'logs' ? 'text-[#1e3a5f]' : 'text-gray-400 hover:text-gray-600'
          }`}
        >
          <Scroll size={20} />
          <span>Logs</span>
        </button>
      </nav>

      {/* Floating Notification Toast */}
      {toast && (
        <div className={`fixed bottom-24 left-4 right-4 p-4 rounded-xl shadow-lg border text-white flex items-center space-x-3 transition-transform duration-300 transform translate-y-0 z-50 ${
          toast.isError ? 'bg-[#e74c3c] border-red-500' : 'bg-[#1e3a5f] border-blue-900'
        }`}>
          <div className="flex-1 text-sm font-semibold">{toast.message}</div>
        </div>
      )}
    </div>
  );
};

export default function App() {
  return (
    <AppProvider>
      <RouterControlApp />
    </AppProvider>
  );
}
