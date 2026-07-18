import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { Scroll, Terminal, ChevronDown, ChevronUp } from 'lucide-react';

export default function Logs() {
  const { terminalLines, connectionStatus, activeDevice } = useApp();
  const [expandedIdx, setExpandedIdx] = useState(null);

  // Extract past commands and their returns
  const commandHistory = [];
  let currentCmd = null;

  terminalLines.forEach((line) => {
    if (line.type === 'prompt') {
      if (currentCmd) {
        commandHistory.push(currentCmd);
      }
      currentCmd = {
        command: line.text.replace(/\[.*\] > /, ''),
        output: '',
        timestamp: new Date().toLocaleTimeString()
      };
    } else if (line.type === 'output' && currentCmd) {
      currentCmd.output += line.text + '\n';
    }
  });

  if (currentCmd) {
    commandHistory.push(currentCmd);
  }

  const logsToDisplay = [...commandHistory].reverse().slice(0, 20);

  if (connectionStatus !== 'connected' || !activeDevice) {
    return (
      <div className="flex flex-col items-center justify-center text-center py-16 px-6">
        <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center text-[#1e3a5f] mb-4">
          <Scroll size={32} />
        </div>
        <h2 className="text-xl font-bold text-gray-800">Logs Locked</h2>
        <p className="text-gray-500 mt-2 max-w-sm">
          Please add and connect to a MikroTik router profile in the "Devices" tab to view command logs.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-xl font-bold text-gray-800">Command History</h2>
          <p className="text-xs text-gray-500">Tracked actions on {activeDevice.name}</p>
        </div>
      </div>

      <div className="space-y-3">
        {logsToDisplay.length === 0 ? (
          <div className="bg-white rounded-xl py-12 px-6 border text-center text-gray-400">
            <Scroll className="mx-auto text-gray-300 mb-3" size={44} />
            <h3 className="font-bold text-gray-700">No Commands Run</h3>
            <p className="text-xs mt-1">Logs will populate as soon as you execute standard terminal requests.</p>
          </div>
        ) : (
          logsToDisplay.map((log, idx) => {
            const isExpanded = expandedIdx === idx;
            return (
              <div 
                key={idx} 
                onClick={() => setExpandedIdx(isExpanded ? null : idx)}
                className="bg-white rounded-xl p-4 border border-gray-100 shadow-sm cursor-pointer hover:bg-gray-50/50 transition-colors"
              >
                <div className="flex justify-between items-center">
                  <div className="flex items-center space-x-2.5 min-w-0">
                    <Terminal size={16} className="text-[#1e3a5f] shrink-0" />
                    <span className="font-mono text-xs font-bold text-gray-800 truncate">{log.command}</span>
                  </div>
                  <div className="flex items-center space-x-2 shrink-0">
                    <span className="text-[10px] text-gray-400 font-mono">{log.timestamp}</span>
                    {isExpanded ? <ChevronUp size={16} className="text-gray-400" /> : <ChevronDown size={16} className="text-gray-400" />}
                  </div>
                </div>

                {isExpanded && (
                  <div className="mt-3 border-t pt-3 border-gray-50">
                    <pre className="bg-gray-900 text-slate-100 p-3 rounded-lg font-mono text-[10px] overflow-x-auto whitespace-pre-wrap max-h-48 leading-relaxed shadow-inner">
                      {log.output.trim() || 'No text output returned.'}
                    </pre>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
