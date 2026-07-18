import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { Plus, Trash, Edit, Wifi, Laptop, Key, HelpCircle } from 'lucide-react';

export default function Devices() {
  const { devices, addDevice, updateDevice, deleteDevice, connectDevice, activeDevice, connectionStatus, disconnect, pingStatuses, pingDevice, showToast } = useApp();
  
  const [showForm, setShowForm] = useState(false);
  const [editingDeviceId, setEditingDeviceId] = useState(null);
  const [name, setName] = useState('');
  const [host, setHost] = useState('');
  const [port, setPort] = useState('22');
  const [username, setUsername] = useState('');
  const [isDemo, setIsDemo] = useState(false);
  const [routerModel, setRouterModel] = useState('hAP Lite (v6)');
  const [connectionType, setConnectionType] = useState('SSH');
  const [l2tpSecret, setL2tpSecret] = useState('');

  // Password Entry Overlay modal
  const [passwordTarget, setPasswordTarget] = useState(null);
  const [password, setPassword] = useState('');

  const handleToggleForm = () => {
    if (showForm) {
      setShowForm(false);
      setEditingDeviceId(null);
      setName('');
      setHost('');
      setPort('22');
      setUsername('');
      setIsDemo(false);
      setRouterModel('hAP Lite (v6)');
      setConnectionType('SSH');
      setL2tpSecret('');
    } else {
      setShowForm(true);
    }
  };

  const handleEditClick = (device) => {
    setEditingDeviceId(device.id);
    setName(device.name);
    setHost(device.host);
    setPort(device.port.toString());
    setUsername(device.username);
    setIsDemo(device.isDemo);
    setRouterModel(device.routerModel || 'hAP Lite (v6)');
    setConnectionType(device.connectionType || 'SSH');
    setL2tpSecret(device.l2tpSecret || '');
    setShowForm(true);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name || !host || !username) return;

    const payload = {
      name,
      host,
      port: parseInt(port) || 22,
      username,
      isDemo,
      routerModel,
      connectionType,
      l2tpSecret
    };

    if (editingDeviceId) {
      updateDevice(editingDeviceId, payload);
    } else {
      addDevice(payload);
    }
    
    // Reset form
    setName('');
    setHost('');
    setPort('22');
    setUsername('');
    setIsDemo(false);
    setRouterModel('hAP Lite (v6)');
    setConnectionType('SSH');
    setL2tpSecret('');
    setEditingDeviceId(null);
    setShowForm(false);
  };

  const handleConnectClick = (device) => {
    if (device.isDemo) {
      connectDevice(device, '');
    } else {
      setPasswordTarget(device);
    }
  };

  const handlePasswordSubmit = (e) => {
    e.preventDefault();
    if (!passwordTarget) return;
    connectDevice(passwordTarget, password);
    setPasswordTarget(null);
    setPassword('');
  };

  return (
    <div className="space-y-6">
      {/* Header and Toggle Button */}
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-bold text-gray-800">Saved Profiles</h2>
        <button
          onClick={handleToggleForm}
          className={`flex items-center space-x-1 px-3 py-1.5 rounded-lg text-xs font-semibold shadow transition-colors text-white ${
            showForm ? 'bg-[#e74c3c]' : 'bg-[#1e3a5f]'
          }`}
        >
          <Plus size={14} />
          <span>{showForm ? 'Cancel' : 'Add Router'}</span>
        </button>
      </div>

      {/* Add / Edit Device Form */}
      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-xl p-5 shadow-sm border border-gray-100 space-y-4">
          <h3 className="text-sm font-bold text-[#1e3a5f]">
            {editingDeviceId ? 'Edit Router Profile' : 'Add New Router Profile'}
          </h3>
          <div className="space-y-3">
            <input
              type="text"
              placeholder="Profile Name (e.g. Home Router)"
              value={name}
              onChange={e => setName(e.target.value)}
              className="w-full text-sm px-3.5 py-2 border rounded-lg focus:outline-[#1e3a5f]"
              required
            />
            <input
              type="text"
              placeholder="Host IP / Address (e.g. 192.168.88.1)"
              value={host}
              onChange={e => setHost(e.target.value)}
              className="w-full text-sm px-3.5 py-2 border rounded-lg focus:outline-[#1e3a5f]"
              required
            />
            <div className="grid grid-cols-3 gap-3">
              <input
                type="number"
                placeholder="Port (22)"
                value={port}
                onChange={e => setPort(e.target.value)}
                className="col-span-1 w-full text-sm px-3.5 py-2 border rounded-lg focus:outline-[#1e3a5f]"
              />
              <input
                type="text"
                placeholder="Username"
                value={username}
                onChange={e => setUsername(e.target.value)}
                className="col-span-2 w-full text-sm px-3.5 py-2 border rounded-lg focus:outline-[#1e3a5f]"
                required
              />
            </div>

            {/* Router Hardware Model Selector */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-gray-700 block">Router Hardware Model</label>
              <div className="grid grid-cols-3 gap-2">
                {['hAP Lite (v6)', 'hEX (v7)', 'Generic'].map((model) => (
                  <button
                    key={model}
                    type="button"
                    onClick={() => setRouterModel(model)}
                    className={`text-xs font-semibold py-2 rounded-lg border transition-all ${
                      routerModel === model
                        ? 'bg-[#1e3a5f] text-white border-[#1e3a5f]'
                        : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    {model}
                  </button>
                ))}
              </div>
            </div>

            {/* Connection Type Selector */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-gray-700 block">Connection Type</label>
              <div className="grid grid-cols-2 gap-2">
                {['SSH', 'L2TP Tunnel'].map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setConnectionType(type)}
                    className={`text-xs font-semibold py-2 rounded-lg border transition-all ${
                      connectionType === type
                        ? 'bg-[#1e3a5f] text-white border-[#1e3a5f]'
                        : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    {type}
                  </button>
                ))}
              </div>
            </div>

            {/* L2TP Secret Field */}
            {connectionType === 'L2TP Tunnel' && (
              <input
                type="password"
                placeholder="L2TP IPsec Secret (Optional)"
                value={l2tpSecret}
                onChange={e => setL2tpSecret(e.target.value)}
                className="w-full text-sm px-3.5 py-2 border rounded-lg focus:outline-[#1e3a5f]"
              />
            )}
            
            <div className="flex justify-between items-center py-2">
              <div>
                <span className="text-xs font-bold block text-gray-700">Demo Router Simulation</span>
                <span className="text-[10px] text-gray-500 block">Runs realistic command responses offline</span>
              </div>
              <input
                type="checkbox"
                checked={isDemo}
                onChange={e => setIsDemo(e.target.checked)}
                className="w-5 h-5 accent-[#1e3a5f] rounded"
              />
            </div>

            <button
              type="submit"
              className="w-full text-sm font-semibold bg-[#1e3a5f] text-white py-2.5 rounded-lg shadow hover:opacity-90"
            >
              {editingDeviceId ? 'Update Profile' : 'Save Profile'}
            </button>
          </div>
        </form>
      )}

      {/* Profiles list */}
      <div className="space-y-3">
        {devices.length === 0 ? (
          <div className="bg-white rounded-xl py-12 px-6 border text-center text-gray-400">
            <Laptop size={44} className="mx-auto text-gray-300 mb-3" />
            <h3 className="font-bold text-gray-700">No Profiles Added</h3>
            <p className="text-xs mt-1">Configure your first MikroTik profile to manage resources.</p>
          </div>
        ) : (
          devices.map((device) => {
            const isCurrent = activeDevice && activeDevice.id === device.id;
            const isConnected = isCurrent && connectionStatus === 'connected';

            return (
              <div 
                key={device.id} 
                className={`bg-white rounded-xl p-4 border shadow-sm flex flex-col justify-between transition-all ${
                  isConnected ? 'border-green-500 bg-green-50/20' : 'border-gray-100'
                }`}
              >
                <div className="flex justify-between items-start">
                  <div>
                    <h3 className="font-bold text-[#1e3a5f] text-base">{device.name}</h3>
                    <p className="text-xs text-gray-500 mt-1">{device.username}@{device.host}:{device.port || 22}</p>
                    <p className="text-[11px] text-gray-400 mt-0.5 font-medium">
                      Model: {device.routerModel || 'hAP Lite (v6)'} &bull; Type: {device.connectionType || 'SSH'}
                    </p>
                  </div>
                  <div className="flex items-center space-x-1">
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 uppercase mr-1">
                      {device.isDemo ? 'Simulation' : 'SSH'}
                    </span>
                    <button
                      onClick={() => handleEditClick(device)}
                      className="p-1.5 rounded-lg text-[#1e3a5f] hover:bg-slate-50"
                      title="Edit Profile"
                    >
                      <Edit size={15} />
                    </button>
                    <button
                      onClick={() => deleteDevice(device.id)}
                      className="p-1.5 rounded-lg text-red-500 hover:bg-red-50"
                      title="Delete Profile"
                    >
                      <Trash size={15} />
                    </button>
                  </div>
                </div>

                {/* Ping Liveness Status Indicator */}
                <div className="mt-3 flex items-center justify-between bg-slate-50 rounded-lg px-3 py-1.5 border border-slate-100">
                  <div className="flex items-center space-x-2">
                    {(() => {
                      const info = pingStatuses[device.id];
                      if (!info) {
                        return (
                          <>
                            <span className="w-1.5 h-1.5 rounded-full bg-gray-300"></span>
                            <span className="text-[11px] text-gray-500">Status: Tap to check liveness</span>
                          </>
                        );
                      }
                      if (info.status === 'checking') {
                        return (
                          <>
                            <span className="w-1.5 h-1.5 rounded-full bg-orange-400 animate-pulse"></span>
                            <span className="text-[11px] text-orange-500 font-medium">Pinging...</span>
                          </>
                        );
                      }
                      if (info.status === 'online') {
                        return (
                          <>
                            <span className="w-1.5 h-1.5 rounded-full bg-green-500"></span>
                            <span className="text-[11px] text-green-600 font-semibold">Online ({info.latency}ms)</span>
                          </>
                        );
                      }
                      return (
                        <>
                          <span className="w-1.5 h-1.5 rounded-full bg-red-500"></span>
                          <span className="text-[11px] text-red-500 font-medium">Offline / Unreachable</span>
                        </>
                      );
                    })()}
                  </div>
                  <button
                    onClick={() => pingDevice(device)}
                    className="text-[10px] font-bold text-[#1e3a5f] bg-white hover:bg-slate-50 border border-slate-200 px-2 py-0.5 rounded shadow-xs transition-all"
                  >
                    Ping
                  </button>
                </div>

                {/* MikroTik Copyable L2TP Config Script */}
                {device.connectionType === 'L2TP Tunnel' && (
                  <div className="mt-3 bg-slate-900 rounded-lg p-3 border border-slate-800 text-slate-300">
                    <div className="flex justify-between items-center mb-1.5">
                      <span className="text-[9px] font-bold text-orange-400 uppercase tracking-wider">MikroTik RouterOS Script</span>
                      <button
                        onClick={() => {
                          const scriptText = `/interface l2tp-client add name="l2tp-RouterControl" connect-to="${device.host}" user="${device.username}" password="<PASSWORD_HERE>" ipsec-secret="${device.l2tpSecret || ''}" use-ipsec=yes disabled=no`;
                          navigator.clipboard.writeText(scriptText);
                          showToast('MikroTik L2TP configuration script copied!');
                        }}
                        className="text-[9px] font-bold text-white bg-blue-800 hover:bg-blue-700 px-2 py-0.5 rounded transition-all"
                      >
                        Copy Script
                      </button>
                    </div>
                    <code className="text-[9px] font-mono block break-all text-emerald-400 select-all leading-normal bg-slate-950 p-1.5 rounded">
                      {`/interface l2tp-client add name="l2tp-RouterControl" connect-to="${device.host}" user="${device.username}" password="<PASSWORD>" ipsec-secret="${device.l2tpSecret || 'SECRET'}" use-ipsec=yes disabled=no`}
                    </code>
                    <p className="text-[8px] text-slate-400 mt-1">Paste into your MikroTik Terminal console to configure this L2TP tunnel.</p>
                  </div>
                )}

                <div className="flex justify-between items-center mt-3 border-t pt-2.5 border-gray-50">
                  <span className="text-[10px] text-gray-400 font-mono">ID: {device.id}</span>
                  {isConnected ? (
                    <button
                      onClick={disconnect}
                      className="text-xs font-semibold bg-[#e74c3c] text-white px-3.5 py-1.5 rounded-lg shadow hover:opacity-90"
                    >
                      Disconnect
                    </button>
                  ) : (
                    <button
                      onClick={() => handleConnectClick(device)}
                      className="text-xs font-semibold bg-[#1e3a5f] text-white px-4 py-1.5 rounded-lg shadow hover:opacity-90"
                    >
                      Connect
                    </button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Password Modal Overlay */}
      {passwordTarget && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <form onSubmit={handlePasswordSubmit} className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl space-y-4">
            <div>
              <h3 className="font-bold text-gray-800 text-lg flex items-center"><Key size={18} className="mr-1.5 text-blue-600" /> Enter SSH Password</h3>
              <p className="text-xs text-gray-500 mt-1">
                Security: RouterControl stores this password temporarily in device memory during active sessions only.
              </p>
            </div>
            
            <input
              type="password"
              placeholder="RouterOS Password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              className="w-full text-sm px-3.5 py-2.5 border rounded-lg focus:outline-[#1e3a5f]"
              required
              autoFocus
            />

            <div className="flex justify-end space-x-2.5">
              <button
                type="button"
                onClick={() => setPasswordTarget(null)}
                className="text-xs font-bold text-gray-500 px-4 py-2"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="text-xs font-bold bg-[#1e3a5f] text-white px-4 py-2 rounded-lg"
              >
                Connect Device
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
