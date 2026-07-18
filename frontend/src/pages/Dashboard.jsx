import React, { useEffect, useState } from 'react';
import { useApp } from '../context/AppContext';
import { ShieldCheck, RefreshCw, Cpu, Activity, Timer, Settings, HelpCircle, HardDrive, Power, User, Play, X, CheckCircle2 } from 'lucide-react';

export default function Dashboard() {
  const { activeDevice, connectionStatus, terminalLines, executeCommand, showToast } = useApp();
  const [metrics, setMetrics] = useState(null);

  // Quick Action Form States
  const [hotspotUser, setHotspotUser] = useState('');
  const [hotspotPass, setHotspotPass] = useState('');
  const [hotspotProfile, setHotspotProfile] = useState('default');
  const [espIp, setEspIp] = useState('10.0.0.254');
  
  const [actionLoading, setActionLoading] = useState(false);
  const [modalTitle, setModalTitle] = useState('');
  const [modalOutput, setModalOutput] = useState('');
  const [showOutputModal, setShowOutputModal] = useState(false);

  // Parse the latest `/system resource print` command run
  useEffect(() => {
    const resourceOutput = [...terminalLines]
      .reverse()
      .find(line => line.text.includes('uptime:') || line.text.includes('cpu-load:'));

    if (resourceOutput) {
      const parsed = parseStdout(resourceOutput.text);
      setMetrics(parsed);
    }
  }, [terminalLines]);

  const refreshMetrics = () => {
    if (connectionStatus === 'connected') {
      executeCommand('/system resource print');
    }
  };

  const parseStdout = (text) => {
    const lines = text.split('\n');
    const result = {
      uptime: 'N/A',
      version: 'N/A',
      freeMem: '0MB',
      totalMem: '256MB',
      cpuLoad: '0%',
      boardName: 'RouterBoard',
      freeHdd: 'N/A'
    };

    lines.forEach(line => {
      const parts = line.split(':');
      if (parts.length >= 2) {
        const key = parts[0].trim().toLowerCase();
        const val = parts.slice(1).join(':').trim();
        if (key.includes('uptime')) result.uptime = val;
        if (key.includes('version')) result.version = val;
        if (key.includes('free-memory')) result.freeMem = val;
        if (key.includes('total-memory')) result.totalMem = val;
        if (key.includes('cpu-load')) result.cpuLoad = val;
        if (key.includes('board-name')) result.boardName = val;
        if (key.includes('free-hdd-space')) result.freeHdd = val;
      }
    });

    return result;
  };

  const handleReboot = async () => {
    if (!window.confirm('Are you sure you want to reboot this MikroTik router? This will terminate the active session.')) {
      return;
    }
    setActionLoading(true);
    try {
      await executeCommand('/system reboot');
      showToast('Reboot command sent successfully!');
      setModalTitle('Reboot Command');
      setModalOutput('Rebooting system... Connection will close.\nRouter is restarting.');
      setShowOutputModal(true);
    } catch (e) {
      showToast('Reboot failed: ' + e.message, true);
    } finally {
      setActionLoading(false);
    }
  };

  const handleApplyHotspotUser = async (e) => {
    e.preventDefault();
    if (!hotspotUser || !hotspotPass) {
      showToast('Username and Password are required to configure Hotspot User.', true);
      return;
    }
    setActionLoading(true);
    const cmd = `:if ([:len [/ip hotspot user find name="${hotspotUser}"]] > 0) do={/ip hotspot user set [find name="${hotspotUser}"] password="${hotspotPass}" profile="${hotspotProfile}"} else={/ip hotspot user add name="${hotspotUser}" password="${hotspotPass}" profile="${hotspotProfile}"}`;
    try {
      const output = await executeCommand(cmd);
      showToast('Hotspot user configuration processed!');
      setModalTitle('Hotspot User Applied');
      setModalOutput(output || 'Command executed successfully.\nHotspot user was added or updated.');
      setShowOutputModal(true);
      // Reset form
      setHotspotUser('');
      setHotspotPass('');
    } catch (e) {
      showToast('Failed to apply Hotspot user: ' + e.message, true);
    } finally {
      setActionLoading(false);
    }
  };

  const handlePingEsp = async (e) => {
    e.preventDefault();
    if (!espIp) {
      showToast('Please specify an IP address.', true);
      return;
    }
    setActionLoading(true);
    const cmd = `/ping count=3 ${espIp}`;
    try {
      const output = await executeCommand(cmd);
      showToast(`Ping completed for ${espIp}!`);
      setModalTitle(`Ping Results: ${espIp}`);
      setModalOutput(output || 'No ping response text received, but ping finished.');
      setShowOutputModal(true);
    } catch (e) {
      showToast('Ping failed: ' + e.message, true);
    } finally {
      setActionLoading(false);
    }
  };

  if (connectionStatus !== 'connected' || !activeDevice) {
    return (
      <div className="flex flex-col items-center justify-center text-center py-16 px-6">
        <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center text-[#1e3a5f] mb-4">
          <Activity size={32} />
        </div>
        <h2 className="text-xl font-bold text-gray-800">No Connected Router</h2>
        <p className="text-gray-500 mt-2 max-w-sm">
          Please add and connect to a MikroTik router profile in the "Devices" tab to monitor dynamic metrics and execute quick commands.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="bg-gradient-to-br from-[#1e3a5f] to-[#2c5282] rounded-2xl p-5 text-white shadow-md">
        <div className="flex justify-between items-start">
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-blue-200">Session Status</span>
            <h1 className="text-2xl font-bold mt-1">{activeDevice.name}</h1>
            <p className="text-xs text-blue-100 mt-1">{activeDevice.username}@{activeDevice.host}</p>
          </div>
          <button 
            onClick={refreshMetrics}
            disabled={actionLoading}
            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 transition-all text-white disabled:opacity-50"
          >
            <RefreshCw size={18} className={actionLoading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {metrics ? (
        <div className="space-y-4">
          {/* Dual circular stats */}
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-white rounded-xl p-4 shadow-sm flex flex-col items-center justify-center border border-gray-100">
              <span className="text-xs font-bold text-gray-500 tracking-wider">CPU LOAD</span>
              <div className="relative w-24 h-24 flex items-center justify-center mt-3">
                <svg className="w-full h-full transform -rotate-90">
                  <circle cx="48" cy="48" r="38" className="stroke-gray-100" strokeWidth="8" fill="transparent" />
                  <circle 
                    cx="48" cy="48" r="38" 
                    className="stroke-[#1e3a5f]" 
                    strokeWidth="8" 
                    fill="transparent" 
                    strokeDasharray={2 * Math.PI * 38}
                    strokeDashoffset={(2 * Math.PI * 38) * (1 - (parseInt(metrics.cpuLoad) || 0) / 100)}
                    strokeLinecap="round"
                  />
                </svg>
                <span className="absolute text-lg font-bold text-gray-800">{metrics.cpuLoad}</span>
              </div>
            </div>

            <div className="bg-white rounded-xl p-4 shadow-sm flex flex-col items-center justify-center border border-gray-100">
              <span className="text-xs font-bold text-gray-500 tracking-wider">MEMORY USED</span>
              <div className="relative w-24 h-24 flex items-center justify-center mt-3">
                <svg className="w-full h-full transform -rotate-90">
                  <circle cx="48" cy="48" r="38" className="stroke-gray-100" strokeWidth="8" fill="transparent" />
                  <circle 
                    cx="48" cy="48" r="38" 
                    className="stroke-[#ffb347]" 
                    strokeWidth="8" 
                    fill="transparent" 
                    strokeDasharray={2 * Math.PI * 38}
                    strokeDashoffset={2 * Math.PI * 38 * 0.4}
                    strokeLinecap="round"
                  />
                </svg>
                <span className="absolute text-lg font-bold text-gray-800">40%</span>
              </div>
            </div>
          </div>

          {/* Specifications list */}
          <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100 space-y-4">
            <h3 className="text-sm font-bold text-[#1e3a5f] border-b pb-2">Device Info & Resources</h3>
            
            <div className="flex justify-between items-center text-sm">
              <span className="text-gray-500 flex items-center"><Timer size={16} className="mr-1.5" /> Uptime</span>
              <span className="font-bold text-gray-800">{metrics.uptime}</span>
            </div>

            <div className="flex justify-between items-center text-sm">
              <span className="text-gray-500 flex items-center"><ShieldCheck size={16} className="mr-1.5" /> RouterOS</span>
              <span className="font-bold text-gray-800">v{metrics.version}</span>
            </div>

            <div className="flex justify-between items-center text-sm">
              <span className="text-gray-500 flex items-center"><Cpu size={16} className="mr-1.5" /> Architecture</span>
              <span className="font-bold text-gray-800">{metrics.boardName}</span>
            </div>

            <div className="flex justify-between items-center text-sm">
              <span className="text-gray-500 flex items-center"><HardDrive size={16} className="mr-1.5" /> Free Storage</span>
              <span className="font-bold text-gray-800">{metrics.freeHdd}</span>
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-white rounded-2xl p-8 border border-dashed flex flex-col items-center justify-center text-center">
          <RefreshCw className="text-blue-500 animate-spin mb-3" size={24} />
          <h3 className="font-bold text-gray-700">Querying System Status...</h3>
          <p className="text-xs text-gray-500 mt-1 max-w-xs">
            Handshaking RouterOS session to obtain hardware specs and resources. This will take a second.
          </p>
        </div>
      )}

      {/* QUICK COMMANDS & ACTIONS CARD (NEW) */}
      <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100 space-y-5">
        <div className="flex justify-between items-center border-b pb-2">
          <h3 className="text-sm font-bold text-[#1e3a5f] flex items-center">
            <Settings size={16} className="mr-1.5 text-blue-600" /> Quick Commands
          </h3>
          <button
            onClick={handleReboot}
            disabled={actionLoading}
            className="flex items-center space-x-1 text-xs font-bold text-red-600 hover:text-red-700 bg-red-50 hover:bg-red-100 px-3 py-1.5 rounded-lg transition-all"
          >
            <Power size={13} />
            <span>Reboot Router</span>
          </button>
        </div>

        {/* Hotspot User Manager */}
        <div className="space-y-3">
          <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider flex items-center">
            <User size={14} className="mr-1.5 text-slate-500" /> Hotspot User Manager
          </h4>
          <form onSubmit={handleApplyHotspotUser} className="grid grid-cols-1 md:grid-cols-3 gap-2">
            <input
              type="text"
              placeholder="Username"
              value={hotspotUser}
              onChange={e => setHotspotUser(e.target.value)}
              className="text-xs px-3 py-2 border rounded-lg focus:outline-[#1e3a5f] w-full bg-slate-50 border-slate-200"
            />
            <input
              type="text"
              placeholder="Password"
              value={hotspotPass}
              onChange={e => setHotspotPass(e.target.value)}
              className="text-xs px-3 py-2 border rounded-lg focus:outline-[#1e3a5f] w-full bg-slate-50 border-slate-200"
            />
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="Profile"
                value={hotspotProfile}
                onChange={e => setHotspotProfile(e.target.value)}
                className="text-xs px-3 py-2 border rounded-lg focus:outline-[#1e3a5f] flex-1 bg-slate-50 border-slate-200"
              />
              <button
                type="submit"
                disabled={actionLoading}
                className="text-xs font-bold text-white bg-[#1e3a5f] hover:bg-[#2c5282] px-3 py-2 rounded-lg transition-all flex items-center justify-center shrink-0 min-w-[70px]"
              >
                Apply
              </button>
            </div>
          </form>
          <p className="text-[10px] text-gray-400">If the username exists, it will be updated; otherwise, a new user will be created on MikroTik.</p>
        </div>

        {/* ESP8266 Custom Ping */}
        <div className="space-y-3 border-t pt-4">
          <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider flex items-center">
            <Play size={14} className="mr-1.5 text-slate-500" /> Custom Device Ping (ESP8266)
          </h4>
          <form onSubmit={handlePingEsp} className="flex gap-2">
            <input
              type="text"
              placeholder="ESP8266 IP (e.g. 10.0.0.254)"
              value={espIp}
              onChange={e => setEspIp(e.target.value)}
              className="text-xs px-3 py-2 border rounded-lg focus:outline-[#1e3a5f] flex-1 bg-slate-50 border-slate-200"
            />
            <button
              type="submit"
              disabled={actionLoading}
              className="text-xs font-bold text-white bg-green-600 hover:bg-green-700 px-4 py-2 rounded-lg transition-all flex items-center justify-center shrink-0"
            >
              Ping ESP
            </button>
          </form>
          <p className="text-[10px] text-gray-400">Triggers a 3-packet ICMP ping from the MikroTik router interface to verify local ESP8266 device availability.</p>
        </div>
      </div>

      {/* Admin Tip */}
      <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 flex items-start space-x-3 text-sm text-[#1e3a5f]">
        <HelpCircle size={20} className="shrink-0 mt-0.5" />
        <div>
          <span className="font-bold block">Advisory Guidance</span>
          <span className="text-blue-900 mt-1 block text-xs">
            Quick Actions will execute CLI scripts automatically. Check the "Terminal" or "Logs" tab to view history.
          </span>
        </div>
      </div>

      {/* COMMAND OUTPUT FEEDBACK MODAL */}
      {showOutputModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl border border-slate-100 flex flex-col max-h-[80vh]">
            <div className="flex justify-between items-center px-5 py-4 border-b">
              <h3 className="font-bold text-[#1e3a5f] text-base flex items-center">
                <CheckCircle2 size={18} className="mr-2 text-green-500" /> {modalTitle}
              </h3>
              <button 
                onClick={() => setShowOutputModal(false)}
                className="p-1.5 rounded-lg hover:bg-slate-100 text-gray-400 hover:text-gray-600"
              >
                <X size={18} />
              </button>
            </div>
            <div className="p-5 overflow-y-auto flex-1 bg-slate-900 font-mono text-xs text-green-400 p-4 rounded-b-lg select-all">
              <pre className="whitespace-pre-wrap">{modalOutput}</pre>
            </div>
            <div className="px-5 py-3 border-t bg-slate-50 flex justify-end">
              <button
                onClick={() => setShowOutputModal(false)}
                className="text-xs font-bold text-gray-600 bg-white hover:bg-gray-100 border px-4 py-2 rounded-lg transition-all"
              >
                Close Output
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
