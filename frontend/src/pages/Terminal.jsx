import React, { useState, useEffect, useRef } from 'react';
import { useApp } from '../context/AppContext';
import { getApiToken, getBaseUrl } from '../utils/api';
import { Terminal as TerminalIcon, Send, Copy, ShieldAlert, Wifi, WifiOff } from 'lucide-react';

export default function Terminal() {
  const { activeDevice, connectionStatus, terminalLines, setTerminalLines } = useApp();
  const [customCmd, setCustomCmd] = useState('');
  const [showReboot, setShowReboot] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);
  const wsRef = useRef(null);
  const terminalEndRef = useRef(null);

  useEffect(() => {
    terminalEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [terminalLines]);

  // Establish WebSocket connection when component mounts and a device is connected
  useEffect(() => {
    if (connectionStatus !== 'connected' || !activeDevice) return;

    const token = getApiToken();
    const baseUrl = getBaseUrl();
    const wsUrl = `${baseUrl.replace('http://', 'ws://')}/api/terminal-ws?token=${token}&deviceId=${activeDevice.id}`;
    
    console.log('Connecting to WebSocket terminal:', wsUrl);
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setWsConnected(true);
      setTerminalLines(prev => [
        ...prev,
        { text: 'Live terminal WebSocket connection established.', type: 'system' }
      ]);
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        if (msg.type === 'chunk') {
          setTerminalLines(prev => {
            const newLines = [...prev];
            if (newLines.length > 0 && newLines[newLines.length - 1].type === 'output') {
              const lastLine = newLines[newLines.length - 1];
              newLines[newLines.length - 1] = {
                ...lastLine,
                text: lastLine.text + msg.data
              };
            } else {
              newLines.push({ text: msg.data, type: 'output' });
            }
            return newLines;
          });
        } else if (msg.type === 'error') {
          setTerminalLines(prev => [...prev, { text: `Error: ${msg.data}`, type: 'error' }]);
        }
      } catch (err) {
        console.error('Error parsing WS message:', err);
      }
    };

    ws.onclose = () => {
      setWsConnected(false);
      setTerminalLines(prev => [
        ...prev,
        { text: 'WebSocket terminal connection closed.', type: 'system' }
      ]);
    };

    ws.onerror = (err) => {
      console.error('WebSocket error:', err);
      setTerminalLines(prev => [...prev, { text: 'WebSocket connection error.', type: 'error' }]);
    };

    return () => {
      ws.close();
      wsRef.current = null;
    };
  }, [activeDevice, connectionStatus, setTerminalLines]);

  const sendCommandViaWs = (commandString) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ command: commandString }));
    } else {
      setTerminalLines(prev => [
        ...prev,
        { text: 'Failed to execute command: WebSocket is offline.', type: 'error' }
      ]);
    }
  };

  const handleSend = (e) => {
    e.preventDefault();
    if (!customCmd.trim()) return;

    const commandString = customCmd.trim();
    setTerminalLines(prev => [
      ...prev,
      { text: `[admin@${activeDevice.name}] > ${commandString}`, type: 'prompt' },
      { text: '', type: 'output' } // Placeholder for streaming chunks
    ]);

    sendCommandViaWs(commandString);
    setCustomCmd('');
  };

  const handleQuickCommand = (cmd) => {
    setTerminalLines(prev => [
      ...prev,
      { text: `[admin@${activeDevice.name}] > ${cmd}`, type: 'prompt' },
      { text: '', type: 'output' } // Placeholder for streaming chunks
    ]);

    sendCommandViaWs(cmd);
  };

  const copyTerminal = () => {
    const fullText = terminalLines.map(l => l.text).join('\n');
    navigator.clipboard.writeText(fullText);
  };

  if (connectionStatus !== 'connected' || !activeDevice) {
    return (
      <div className="flex flex-col items-center justify-center text-center py-16 px-6">
        <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center text-[#1e3a5f] mb-4">
          <TerminalIcon size={32} />
        </div>
        <h2 className="text-xl font-bold text-gray-800">Terminal Locked</h2>
        <p className="text-gray-500 mt-2 max-w-sm">
          Please add and connect to a MikroTik router profile in the "Devices" tab to open the terminal CLI session.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-[calc(100vh-12rem)] max-h-screen space-y-4">
      {/* Quick Actions Bar */}
      <div>
        <span className="text-xs font-bold text-gray-500 block mb-2 uppercase tracking-wide">Quick RouterOS Diagnostics</span>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => handleQuickCommand('/ip address print')}
            className="text-[10px] font-mono bg-white border border-gray-200 text-gray-700 px-2.5 py-1.5 rounded-lg shadow-sm hover:bg-gray-50"
          >
            /ip address print
          </button>
          <button
            onClick={() => handleQuickCommand('/interface print')}
            className="text-[10px] font-mono bg-white border border-gray-200 text-gray-700 px-2.5 py-1.5 rounded-lg shadow-sm hover:bg-gray-50"
          >
            /interface print
          </button>
          <button
            onClick={() => handleQuickCommand('/system resource print')}
            className="text-[10px] font-mono bg-white border border-gray-200 text-gray-700 px-2.5 py-1.5 rounded-lg shadow-sm hover:bg-gray-50"
          >
            /system resource print
          </button>
          <button
            onClick={() => handleQuickCommand('/log print')}
            className="text-[10px] font-mono bg-white border border-gray-200 text-gray-700 px-2.5 py-1.5 rounded-lg shadow-sm hover:bg-gray-50"
          >
            /log print
          </button>
          <button
            onClick={() => setShowReboot(true)}
            className="text-[10px] font-mono bg-red-50 border border-red-100 text-[#e74c3c] px-2.5 py-1.5 rounded-lg shadow-sm font-semibold hover:bg-red-100/50"
          >
            /system reboot
          </button>
        </div>
      </div>

      {/* Terminal Display Viewport */}
      <div className="flex-1 bg-[#1e1e1e] rounded-xl p-4 flex flex-col overflow-hidden relative shadow-inner">
        <div className="flex justify-between items-center pb-2 border-b border-gray-800 mb-2">
          <div className="flex items-center space-x-2">
            <span className="text-xs font-semibold text-gray-400 font-mono">[admin@{activeDevice.name}]</span>
            <div className={`flex items-center space-x-1 text-[10px] font-semibold px-2 py-0.5 rounded-full ${
              wsConnected ? 'bg-emerald-500/10 text-emerald-400' : 'bg-rose-500/10 text-rose-400'
            }`}>
              {wsConnected ? <Wifi size={10} /> : <WifiOff size={10} />}
              <span>{wsConnected ? 'WebSocket Live' : 'WebSocket Offline'}</span>
            </div>
          </div>
          <button 
            onClick={copyTerminal}
            className="p-1 rounded text-gray-400 hover:text-white hover:bg-white/5 transition-colors"
          >
            <Copy size={14} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto font-mono text-xs space-y-2 pr-1.5">
          {terminalLines.map((line, idx) => {
            const colorClass = 
              line.type === 'error' ? 'text-red-400' :
              line.type === 'success' ? 'text-green-400' :
              line.type === 'system' ? 'text-blue-400' :
              line.type === 'prompt' ? 'text-emerald-400 font-semibold' : 'text-slate-200';
            
            return (
              <pre key={idx} className={`whitespace-pre-wrap leading-relaxed ${colorClass}`}>
                {line.text}
              </pre>
            );
          })}
          <div ref={terminalEndRef} />
        </div>
      </div>

      {/* Input row */}
      <form onSubmit={handleSend} className="flex space-x-2">
        <input
          type="text"
          placeholder="Run manual command..."
          value={customCmd}
          onChange={e => setCustomCmd(e.target.value)}
          className="flex-1 font-mono text-xs px-4 py-3 bg-white border border-gray-200 rounded-xl focus:outline-[#1e3a5f]"
          disabled={!wsConnected}
        />
        <button
          type="submit"
          className="bg-[#1e3a5f] text-white p-3 rounded-xl shadow hover:opacity-90 disabled:opacity-50"
          disabled={!wsConnected}
        >
          <Send size={16} />
        </button>
      </form>

      {/* Reboot Confirm Dialog */}
      {showReboot && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl space-y-4">
            <div>
              <h3 className="font-bold text-gray-800 text-lg flex items-center"><ShieldAlert size={20} className="mr-1.5 text-red-500" /> System Reboot</h3>
              <p className="text-xs text-gray-500 mt-2">
                Warning: Triggering /system reboot will terminate the SSH tunnel and restart the physical router board.
              </p>
            </div>

            <div className="flex justify-end space-x-2.5 pt-2">
              <button
                type="button"
                onClick={() => setShowReboot(false)}
                className="text-xs font-bold text-gray-500 px-4 py-2"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  handleQuickCommand('/system reboot');
                  setShowReboot(false);
                }}
                className="text-xs font-bold bg-[#e74c3c] text-white px-4 py-2 rounded-lg"
              >
                Reboot Router
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
