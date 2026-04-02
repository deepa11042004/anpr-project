import { useEffect, useMemo, useRef, useState } from 'react';
import { systemLogsApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Activity, Pause, Play, RefreshCw, ShieldAlert } from 'lucide-react';

const levelClass = (level) => {
  switch (level) {
    case 'ERROR':
      return 'text-red-700 bg-red-50';
    case 'WARN':
      return 'text-yellow-800 bg-yellow-50';
    case 'DEBUG':
      return 'text-blue-700 bg-blue-50';
    case 'TRACE':
      return 'text-purple-700 bg-purple-50';
    default:
      return 'text-green-700 bg-green-50';
  }
};

const SystemLogs = () => {
  const { hasRole } = useAuth();
  const [entries, setEntries] = useState([]);
  const [cursor, setCursor] = useState(-1);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const logContainerRef = useRef(null);

  const canView = hasRole(['SUPER_ADMIN', 'ADMIN']);

  const fetchLogs = async (isInitial = false) => {
    try {
      setLoading(true);
      const response = await systemLogsApi.getLogs({
        cursor: isInitial ? -1 : cursor,
        maxLines: 300,
        tailLines: 300,
      });

      const data = response.data;
      if (!data.success) {
        setError(data.message || 'Failed to load logs');
        return;
      }

      setError('');
      setCursor(data.cursor ?? cursor);
      if (isInitial) {
        setEntries(data.entries || []);
      } else {
        setEntries((prev) => {
          const combined = [...prev, ...(data.entries || [])];
          return combined.length > 4000 ? combined.slice(combined.length - 4000) : combined;
        });
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load logs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!canView) {
      return;
    }
    fetchLogs(true);
  }, [canView]);

  useEffect(() => {
    if (!canView || !autoRefresh) {
      return;
    }
    const timer = setInterval(() => fetchLogs(false), 2000);
    return () => clearInterval(timer);
  }, [autoRefresh, cursor, canView]);

  useEffect(() => {
    if (logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
    }
  }, [entries]);

  const filteredEntries = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) {
      return entries;
    }
    return entries.filter((entry) => entry.line.toLowerCase().includes(q) || entry.level.toLowerCase().includes(q));
  }, [entries, query]);

  if (!canView) {
    return (
      <div className="card">
        <div className="flex items-center gap-3 text-red-600">
          <ShieldAlert size={22} />
          <h2 className="text-xl font-semibold">Access denied</h2>
        </div>
        <p className="mt-3 text-gray-600">Only ADMIN or SUPER_ADMIN can view system logs.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">System Logs</h1>
          <p className="text-gray-500">Live heartbeat of application events, errors, and processing flow.</p>
        </div>

        <div className="flex items-center gap-2">
          <button
            className="btn btn-secondary flex items-center gap-2"
            onClick={() => setAutoRefresh((prev) => !prev)}
          >
            {autoRefresh ? <Pause size={16} /> : <Play size={16} />}
            {autoRefresh ? 'Pause' : 'Resume'}
          </button>

          <button
            className="btn btn-secondary flex items-center gap-2"
            onClick={() => fetchLogs(false)}
            disabled={loading}
          >
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            Refresh
          </button>
        </div>
      </div>

      <div className="card">
        <div className="flex flex-col sm:flex-row sm:items-center gap-3 mb-4">
          <div className="flex items-center gap-2 text-gray-700">
            <Activity size={18} className={autoRefresh ? 'text-green-600' : 'text-gray-400'} />
            <span className="text-sm">{autoRefresh ? 'Live stream active' : 'Live stream paused'}</span>
          </div>

          <input
            className="input sm:ml-auto sm:max-w-sm"
            placeholder="Filter logs by text or level"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>

        {error && (
          <div className="mb-4 p-3 rounded border border-red-200 bg-red-50 text-red-700 text-sm">
            {error}
          </div>
        )}

        <div ref={logContainerRef} className="h-[70vh] overflow-auto rounded border bg-gray-950 p-2">
          <div className="space-y-1 font-mono text-xs">
            {filteredEntries.length === 0 ? (
              <div className="text-gray-400 p-2">No log lines to show.</div>
            ) : (
              filteredEntries.map((entry, index) => (
                <div key={`${entry.offset}-${index}`} className={`px-2 py-1 rounded ${levelClass(entry.level)}`}>
                  <span className="font-semibold mr-2">[{entry.level}]</span>
                  <span className="break-all">{entry.line}</span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SystemLogs;
