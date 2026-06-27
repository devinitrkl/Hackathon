const API_BASE = import.meta.env.VITE_API_URL || '';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error || `Request failed: ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export const api = {
  getAgents: () => request('/agents'),
  getOrders: (status) => request(status ? `/orders?status=${status}` : '/orders'),
  getSuggestions: (status) => request(status ? `/suggestions?status=${status}` : '/suggestions'),
  setAgentStatus: (id, status) =>
    request(`/agents/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  updateSuggestion: (id, status) =>
    request(`/suggestions/${id}`, { method: 'PATCH', body: JSON.stringify({ status }) }),
};
