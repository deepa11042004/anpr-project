import { useState, useEffect, useRef } from 'react';
import { dashboardApi, gateApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { 
  Radio, 
  RefreshCw, 
  CheckCircle, 
  XCircle, 
  Hand,
  AlertCircle,
  Loader2,
  DoorOpen
} from 'lucide-react';
import { format } from 'date-fns';

const LiveFeed = () => {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [overrideModal, setOverrideModal] = useState({ open: false, entry: null });
  const [overrideReason, setOverrideReason] = useState('');
  const [overrideLoading, setOverrideLoading] = useState(false);
  const intervalRef = useRef(null);
  const { hasRole } = useAuth();

  const canOverride = hasRole(['SUPER_ADMIN', 'ADMIN', 'OPERATOR']);

  const fetchEntries = async () => {
    try {
      setError(null);
      const response = await dashboardApi.getLiveFeed();
      setEntries(response.data);
    } catch (err) {
      setError('Failed to load live feed');
      console.error('Live feed error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEntries();
    
    if (autoRefresh) {
      intervalRef.current = setInterval(fetchEntries, 5000); // Poll every 5 seconds
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [autoRefresh]);

  const handleOverride = async () => {
    if (!overrideReason.trim()) {
      return;
    }

    setOverrideLoading(true);
    try {
      await gateApi.override({
        vehicleNumber: overrideModal.entry?.vehicleNumber || 'MANUAL',
        reason: overrideReason,
        cameraIp: overrideModal.entry?.cameraIp
      });
      
      setOverrideModal({ open: false, entry: null });
      setOverrideReason('');
      fetchEntries(); // Refresh to show the override
    } catch (err) {
      console.error('Override error:', err);
    } finally {
      setOverrideLoading(false);
    }
  };

  const getStatusBadge = (outcome) => {
    switch (outcome) {
      case 'APPROVED':
        return (
          <span className="badge badge-success flex items-center gap-1">
            <CheckCircle size={12} />
            Approved
          </span>
        );
      case 'REJECTED':
        return (
          <span className="badge badge-danger flex items-center gap-1">
            <XCircle size={12} />
            Rejected
          </span>
        );
      case 'MANUAL_OVERRIDE':
        return (
          <span className="badge badge-warning flex items-center gap-1">
            <Hand size={12} />
            Override
          </span>
        );
      default:
        return (
          <span className="badge badge-info flex items-center gap-1">
            <AlertCircle size={12} />
            {outcome}
          </span>
        );
    }
  };

  const formatTime = (timestamp) => {
    try {
      return format(new Date(timestamp), 'HH:mm:ss');
    } catch {
      return '-';
    }
  };

  const formatDate = (timestamp) => {
    try {
      return format(new Date(timestamp), 'MMM dd, yyyy');
    } catch {
      return '-';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="animate-spin text-primary-600" size={48} />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className={`p-2 rounded-full ${autoRefresh ? 'bg-green-100' : 'bg-gray-100'}`}>
            <Radio className={autoRefresh ? 'text-green-600 animate-pulse' : 'text-gray-400'} size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Live Feed</h1>
            <p className="text-gray-500">
              {autoRefresh ? 'Auto-refreshing every 5 seconds' : 'Auto-refresh paused'}
            </p>
          </div>
        </div>
        
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              className="w-4 h-4 accent-primary-600"
            />
            <span className="text-sm">Auto-refresh</span>
          </label>
          
          <button 
            onClick={fetchEntries}
            className="btn btn-secondary flex items-center gap-2"
          >
            <RefreshCw size={18} />
            Refresh
          </button>

          {canOverride && (
            <button 
              onClick={() => setOverrideModal({ open: true, entry: null })}
              className="btn btn-primary flex items-center gap-2"
            >
              <DoorOpen size={18} />
              Manual Override
            </button>
          )}
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-700 rounded-lg">
          {error}
        </div>
      )}

      {/* Entries Table */}
      <div className="card overflow-hidden p-0">
        <div className="table-container">
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Time</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Vehicle</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Confidence</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Gate</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Camera</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {entries.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-6 py-12 text-center text-gray-500">
                    No entries yet. Waiting for vehicles...
                  </td>
                </tr>
              ) : (
                entries.map((entry) => (
                  <tr key={entry.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4">
                      <div>
                        <p className="font-medium">{formatTime(entry.timestamp)}</p>
                        <p className="text-xs text-gray-500">{formatDate(entry.timestamp)}</p>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div>
                        <p className="font-mono font-bold text-lg">{entry.vehicleNumber}</p>
                        {entry.normalizedPlate !== entry.vehicleNumber && (
                          <p className="text-xs text-gray-500">
                            Normalized: {entry.normalizedPlate}
                          </p>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      {entry.confidenceScore != null ? (
                        <div className="flex items-center gap-2">
                          <div className="w-16 h-2 bg-gray-200 rounded-full overflow-hidden">
                            <div 
                              className={`h-full rounded-full ${
                                entry.confidenceScore >= 85 
                                  ? 'bg-green-500' 
                                  : entry.confidenceScore >= 70 
                                  ? 'bg-yellow-500' 
                                  : 'bg-red-500'
                              }`}
                              style={{ width: `${entry.confidenceScore}%` }}
                            />
                          </div>
                          <span className="text-sm">{entry.confidenceScore}%</span>
                        </div>
                      ) : (
                        <span className="text-gray-400">-</span>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      {getStatusBadge(entry.authorizationOutcome)}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`text-sm ${
                        entry.gateAction === 'OPENED' 
                          ? 'text-green-600' 
                          : 'text-gray-500'
                      }`}>
                        {entry.gateAction?.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {entry.cameraIp || '-'}
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-xs text-gray-500 space-y-1">
                        {entry.companyName && <p>Company: {entry.companyName}</p>}
                        {entry.driverName && <p>Driver: {entry.driverName}</p>}
                        {entry.rejectionReason && (
                          <p className="text-red-500">Reason: {entry.rejectionReason}</p>
                        )}
                        {entry.overrideReason && (
                          <p className="text-yellow-600">Override: {entry.overrideReason}</p>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Override Modal */}
      {overrideModal.open && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6">
            <h3 className="text-lg font-bold mb-4">Manual Gate Override</h3>
            
            <div className="space-y-4">
              {overrideModal.entry && (
                <div className="p-3 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-500">Vehicle</p>
                  <p className="font-mono font-bold">{overrideModal.entry.vehicleNumber}</p>
                </div>
              )}
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Vehicle Number (if manual entry)
                </label>
                <input
                  type="text"
                  placeholder="Enter vehicle number"
                  className="input"
                  defaultValue={overrideModal.entry?.vehicleNumber || ''}
                  onChange={(e) => {
                    if (!overrideModal.entry) {
                      setOverrideModal(prev => ({
                        ...prev, 
                        entry: { vehicleNumber: e.target.value }
                      }));
                    }
                  }}
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Reason for Override <span className="text-red-500">*</span>
                </label>
                <textarea
                  value={overrideReason}
                  onChange={(e) => setOverrideReason(e.target.value)}
                  className="input min-h-[100px]"
                  placeholder="Enter the reason for manual override..."
                />
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <button 
                onClick={() => {
                  setOverrideModal({ open: false, entry: null });
                  setOverrideReason('');
                }}
                className="btn btn-secondary flex-1"
              >
                Cancel
              </button>
              <button 
                onClick={handleOverride}
                disabled={!overrideReason.trim() || overrideLoading}
                className="btn btn-primary flex-1 flex items-center justify-center gap-2"
              >
                {overrideLoading ? (
                  <>
                    <Loader2 className="animate-spin" size={18} />
                    Processing...
                  </>
                ) : (
                  <>
                    <DoorOpen size={18} />
                    Open Gate
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default LiveFeed;
