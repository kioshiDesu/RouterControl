const BASE_URL = 'http://127.0.0.1:3000';

let apiToken = '';

/**
 * Get active session api token.
 */
export const getApiToken = () => apiToken;

/**
 * Get backend base URL.
 */
export const getBaseUrl = () => BASE_URL;

/**
 * Retrieve API token from embedded backend health route at startup.
 */
export const initApiToken = async () => {
  try {
    const res = await fetch(`${BASE_URL}/health`);
    const data = await res.json();
    if (data && data.token) {
      apiToken = data.token;
      return true;
    }
  } catch (error) {
    console.error('Failed to handshake with embedded Node.js backend', error);
  }
  return false;
};

/**
 * Perform authorized API call.
 */
export const apiCall = async (endpoint, options = {}) => {
  const url = `${BASE_URL}/api${endpoint}`;
  
  const headers = {
    'Content-Type': 'application/json',
    'x-app-token': apiToken,
    ...(options.headers || {})
  };

  const response = await fetch(url, {
    ...options,
    headers
  });

  const data = await response.json();
  if (!response.ok || data.error) {
    throw new Error(data.message || 'API request failed.');
  }

  return data;
};
