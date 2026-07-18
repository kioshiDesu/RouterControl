import React, { createContext, useState, useEffect, useContext } from 'react';
import { apiCall, initApiToken } from '../utils/api';

const AppContext = createContext();

export const useApp = () => useContext(AppContext);

export const AppProvider = ({ children }) => {
  const [devices, setDevices] = useState(() => {
    const saved = localStorage.getItem('devices');
    return saved ? JSON.parse(saved) : [];
  });
  const [activeDevice, setActiveDevice] = useState(null);
  const [connectionStatus, setConnectionStatus] = useState('disconnected'); // disconnected | connecting | connected
  const [terminalLines, setTerminalLines] = useState([]);
  const [toast, setToast] = useState(null);
  const [pingStatuses, setPingStatuses] = useState({});

  useEffect(() => {
    localStorage.setItem('devices', JSON.stringify(devices));
  }, [devices]);

  useEffect(() => {
    // Perform startup handshake
    initApiToken().then(success => {
      if (success) {
        showToast('Successfully handshake with embedded manager server.');
      } else {
        showToast('Local backend server is offline or unreachable.', true);
      }
    });
  }, []);

  const showToast = (message, isError = false) => {
    setToast({ message, isError });
    setTimeout(() => setToast(null), 3000);
  };

  const pingDevice = async (device) => {
    setPingStatuses(prev => ({ ...prev, [device.id]: { status: 'checking' } }));
    try {
      const res = await apiCall('/connect/ping', {
        method: 'POST',
        body: JSON.stringify({
          host: device.host,
          port: device.port,
          isDemo: device.isDemo,
          connectionType: device.connectionType
        })
      });
      if (res.online) {
        setPingStatuses(prev => ({
          ...prev,
          [device.id]: { status: 'online', latency: res.latency }
        }));
      } else {
        setPingStatuses(prev => ({
          ...prev,
          [device.id]: { status: 'offline' }
        }));
      }
    } catch (error) {
      setPingStatuses(prev => ({
        ...prev,
        [device.id]: { status: 'offline' }
      }));
    }
  };

  const addDevice = (device) => {
    const newDevice = { ...device, id: Date.now().toString() };
    setDevices(prev => [...prev, newDevice]);
    showToast(`Profile '${device.name}' added successfully.`);
  };

  const updateDevice = (id, updatedFields) => {
    setDevices(prev => prev.map(d => d.id === id ? { ...d, ...updatedFields } : d));
    if (activeDevice && activeDevice.id === id) {
      setActiveDevice(prev => ({ ...prev, ...updatedFields }));
    }
    showToast(`Profile updated successfully.`);
  };

  const deleteDevice = (id) => {
    setDevices(prev => prev.filter(d => d.id !== id));
    if (activeDevice && activeDevice.id === id) {
      disconnect();
    }
    showToast('Device profile deleted.');
  };

  const connectDevice = async (device, password = '') => {
    setConnectionStatus('connecting');
    setTerminalLines([{ text: `Connecting to ${device.host}:${device.port || 22}...`, type: 'system' }]);
    
    try {
      await apiCall('/connect', {
        method: 'POST',
        body: JSON.stringify({ ...device, password })
      });
      setActiveDevice(device);
      setConnectionStatus('connected');
      setTerminalLines(prev => [
        ...prev,
        { text: 'Authentication successful!', type: 'success' },
        { text: 'Type standard RouterOS commands or click quick actions to retrieve statistics.', type: 'system' }
      ]);
      showToast(`Connected to ${device.name}!`);
    } catch (error) {
      setConnectionStatus('error');
      setTerminalLines(prev => [...prev, { text: `Connection Failed: ${error.message}`, type: 'error' }]);
      showToast(error.message, true);
    }
  };

  const disconnect = async () => {
    if (!activeDevice) return;
    try {
      await apiCall('/connect/disconnect', {
        method: 'POST',
        body: JSON.stringify({ id: activeDevice.id })
      });
    } catch (e) {
      console.error(e);
    }
    setActiveDevice(null);
    setConnectionStatus('disconnected');
    setTerminalLines(prev => [...prev, { text: 'Session closed.', type: 'system' }]);
    showToast('Session ended.');
  };

  const executeCommand = async (commandString) => {
    if (!activeDevice) return;
    
    setTerminalLines(prev => [...prev, { text: `[admin@${activeDevice.name}] > ${commandString}`, type: 'prompt' }]);

    try {
      const res = await apiCall('/command', {
        method: 'POST',
        body: JSON.stringify({ deviceId: activeDevice.id, command: commandString })
      });
      setTerminalLines(prev => [...prev, { text: res.output, type: 'output' }]);
      return res.output;
    } catch (error) {
      setTerminalLines(prev => [...prev, { text: error.message, type: 'error' }]);
      showToast(error.message, true);
      throw error;
    }
  };

  return (
    <AppContext.Provider value={{
      devices,
      activeDevice,
      connectionStatus,
      terminalLines,
      setTerminalLines,
      toast,
      addDevice,
      updateDevice,
      deleteDevice,
      connectDevice,
      disconnect,
      executeCommand,
      showToast,
      pingStatuses,
      pingDevice
    }}>
      {children}
    </AppContext.Provider>
  );
};
