import { useCallback, useEffect, useState } from 'react';
import { api } from './api';
import './App.css';

const POLL_MS = 5000;

function statusClass(status) {
  return `status status-${status.toLowerCase()}`;
}

function App() {
  const [agents, setAgents] = useState([]);
  const [orders, setOrders] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);

  const refresh = useCallback(async () => {
    try {
      setError(null);
      const [agentData, orderData, suggestionData] = await Promise.all([
        api.getAgents(),
        api.getOrders('REASSIGNMENT_PENDING'),
        api.getSuggestions('PENDING'),
      ]);
      setAgents(agentData);
      setOrders(orderData);
      setSuggestions(suggestionData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, POLL_MS);
    return () => clearInterval(interval);
  }, [refresh]);

  async function handleSuggestionUpdate(id, status) {
    setActionLoading(id + status);
    try {
      await api.updateSuggestion(id, status);
      await refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setActionLoading(null);
    }
  }

  async function handleAgentOffline(id) {
    setActionLoading('offline-' + id);
    try {
      await api.setAgentStatus(id, 'OFFLINE');
      await refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setActionLoading(null);
    }
  }

  const pendingOrders = orders.length > 0 ? orders : suggestions.map((s) => ({
    id: s.orderId,
    description: s.orderDescription,
    assignedAgentName: '—',
    pendingSuggestion: s,
  }));

  return (
    <div className="app">
      <header className="header">
        <div>
          <p className="kicker">ZipRun Ops</p>
          <h1>AI Reassignment Engine</h1>
        </div>
        <button type="button" className="btn secondary" onClick={refresh} disabled={loading}>
          Refresh
        </button>
      </header>

      {error && <div className="banner error">{error}</div>}
      {loading && <div className="banner info">Loading…</div>}

      <main className="grid">
        <section className="panel">
          <h2>Pending Reassignments</h2>
          {pendingOrders.length === 0 ? (
            <p className="empty">No orders awaiting reassignment. Set an agent offline to trigger the agentic loop.</p>
          ) : (
            <ul className="card-list">
              {pendingOrders.map((order) => {
                const suggestion = order.pendingSuggestion
                  || suggestions.find((s) => s.orderId === order.id);
                if (!suggestion) return null;
                return (
                  <li key={order.id} className="card">
                    <div className="card-head">
                      <div>
                        <strong>{order.id}</strong>
                        <p>{order.description}</p>
                        <p className="muted">Currently: {order.assignedAgentName || 'unknown'}</p>
                      </div>
                      {suggestion.triggerReason === 'AGENT_OFFLINE' && (
                        <span className="badge replan">Agentic Re-plan</span>
                      )}
                      {suggestion.triggerReason === 'INITIAL' && (
                        <span className="badge manual">Manual Suggestion</span>
                      )}
                    </div>
                    <div className="suggestion">
                      <p><strong>Recommended:</strong> {suggestion.recommendedAgentName} ({suggestion.recommendedAgentId})</p>
                      <p><strong>Confidence:</strong> {(suggestion.confidence * 100).toFixed(0)}%</p>
                      <blockquote>{suggestion.reasoning}</blockquote>
                    </div>
                    <div className="actions">
                      <button
                        type="button"
                        className="btn accept"
                        disabled={actionLoading === suggestion.id + 'ACCEPTED'}
                        onClick={() => handleSuggestionUpdate(suggestion.id, 'ACCEPTED')}
                      >
                        Accept
                      </button>
                      <button
                        type="button"
                        className="btn reject"
                        disabled={actionLoading === suggestion.id + 'REJECTED'}
                        onClick={() => handleSuggestionUpdate(suggestion.id, 'REJECTED')}
                      >
                        Reject
                      </button>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </section>

        <section className="panel">
          <h2>Agent Roster</h2>
          <ul className="agent-list">
            {agents.map((agent) => (
              <li key={agent.id} className="agent-row">
                <div>
                  <strong>{agent.name}</strong>
                  <span className="muted"> ({agent.id})</span>
                  <p className="muted">Load: {agent.activeOrderCount} orders</p>
                </div>
                <div className="agent-actions">
                  <span className={statusClass(agent.status)}>{agent.status}</span>
                  {agent.status !== 'OFFLINE' && (
                    <button
                      type="button"
                      className="btn danger small"
                      disabled={actionLoading === 'offline-' + agent.id}
                      onClick={() => handleAgentOffline(agent.id)}
                    >
                      Set Offline
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </section>
      </main>
    </div>
  );
}

export default App;
