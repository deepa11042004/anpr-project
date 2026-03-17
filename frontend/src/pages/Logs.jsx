import { useState, useEffect } from 'react';
import { logsApi } from '../api/client';
import { 
  Search, 
  Filter, 
  ChevronLeft, 
  ChevronRight,
  CheckCircle,
  XCircle,
  Hand,
  AlertCircle,
  Loader2,
  X,
  Download
} from 'lucide-react';
import { format } from 'date-fns';

const Logs = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [selectedLog, setSelectedLog] = useState(null);
  
  // Filters
  const [filters, setFilters] = useState({
    vehicleNumber: '',
    outcome: '',
    startDate: '',
    endDate: '',
    page: 0,
    size: 20
  });

  const fetchLogs = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const params = {
        ...filters,
        vehicleNumber: filters.vehicleNumber || undefined,
        outcome: filters.outcome || undefined,
        startDate: filters.startDate || undefined,
        endDate: filters.endDate || undefined,
      };

      const response = await logsApi.getLogs(params);
      setLogs(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
      setTotalElements(response.data.totalElements || 0);
    } catch (err) {
      setError('Failed to load logs');
      console.error('Logs error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [filters.page]);

  const handleSearch = () => {
    setFilters(prev => ({ ...prev, page: 0 }));
    fetchLogs();
  };

  const handleClearFilters = () => {
    setFilters({
      vehicleNumber: '',
      outcome: '',
      startDate: '',
      endDate: '',
      page: 0,
      size: 20
    });
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

  const formatDateTime = (timestamp) => {
    try {
      return format(new Date(timestamp), 'MMM dd, yyyy HH:mm:ss');
    } catch {
      return '-';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold">Historical Logs</h1>
        <p className="text-gray-500">
          {totalElements} total entries
        </p>
      </div>

      {/* Filters */}
      <div className="card">
        <div className="flex items-center gap-2 mb-4">
          <Filter size={20} className="text-gray-400" />
          <h3 className="font-semibold">Filters</h3>
        </div>
        
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          <div>
            <label className="block text-sm text-gray-500 mb-1">Vehicle Number</label>
            <input
              type="text"
              value={filters.vehicleNumber}
              onChange={(e) => setFilters(prev => ({ ...prev, vehicleNumber: e.target.value }))}
              className="input"
              placeholder="Search plate..."
            />
          </div>
          
          <div>
            <label className="block text-sm text-gray-500 mb-1">Status</label>
            <select
              value={filters.outcome}
              onChange={(e) => setFilters(prev => ({ ...prev, outcome: e.target.value }))}
              className="input"
            >
              <option value="">All</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
              <option value="MANUAL_OVERRIDE">Manual Override</option>
              <option value="ERROR">Error</option>
            </select>
          </div>
          
          <div>
            <label className="block text-sm text-gray-500 mb-1">Start Date</label>
            <input
              type="date"
              value={filters.startDate}
              onChange={(e) => setFilters(prev => ({ ...prev, startDate: e.target.value }))}
              className="input"
            />
          </div>
          
          <div>
            <label className="block text-sm text-gray-500 mb-1">End Date</label>
            <input
              type="date"
              value={filters.endDate}
              onChange={(e) => setFilters(prev => ({ ...prev, endDate: e.target.value }))}
              className="input"
            />
          </div>
          
          <div className="flex items-end gap-2">
            <button onClick={handleSearch} className="btn btn-primary flex items-center gap-2">
              <Search size={18} />
              Search
            </button>
            <button onClick={handleClearFilters} className="btn btn-secondary">
              Clear
            </button>
          </div>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-700 rounded-lg">
          {error}
        </div>
      )}

      {/* Logs Table */}
      <div className="card overflow-hidden p-0">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="animate-spin text-primary-600" size={48} />
          </div>
        ) : (
          <div className="table-container">
            <table className="w-full">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">ID</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Timestamp</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Vehicle</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Confidence</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Gate Action</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {logs.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="px-6 py-12 text-center text-gray-500">
                      No logs found matching your filters.
                    </td>
                  </tr>
                ) : (
                  logs.map((log) => (
                    <tr key={log.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-4 text-sm text-gray-500">#{log.id}</td>
                      <td className="px-6 py-4 text-sm">{formatDateTime(log.timestamp)}</td>
                      <td className="px-6 py-4">
                        <p className="font-mono font-bold">{log.vehicleNumber}</p>
                        {log.companyName && (
                          <p className="text-xs text-gray-500">{log.companyName}</p>
                        )}
                      </td>
                      <td className="px-6 py-4 text-sm">
                        {log.confidenceScore != null ? `${log.confidenceScore}%` : '-'}
                      </td>
                      <td className="px-6 py-4">{getStatusBadge(log.authorizationOutcome)}</td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {log.gateAction?.replace('_', ' ')}
                      </td>
                      <td className="px-6 py-4">
                        <button
                          onClick={() => setSelectedLog(log)}
                          className="text-primary-600 hover:text-primary-700 text-sm font-medium"
                        >
                          View Details
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-6 py-4 border-t">
            <p className="text-sm text-gray-500">
              Showing {filters.page * filters.size + 1} to{' '}
              {Math.min((filters.page + 1) * filters.size, totalElements)} of {totalElements}
            </p>
            
            <div className="flex items-center gap-2">
              <button
                onClick={() => setFilters(prev => ({ ...prev, page: prev.page - 1 }))}
                disabled={filters.page === 0}
                className="btn btn-secondary p-2 disabled:opacity-50"
              >
                <ChevronLeft size={18} />
              </button>
              
              <span className="text-sm">
                Page {filters.page + 1} of {totalPages}
              </span>
              
              <button
                onClick={() => setFilters(prev => ({ ...prev, page: prev.page + 1 }))}
                disabled={filters.page >= totalPages - 1}
                className="btn btn-secondary p-2 disabled:opacity-50"
              >
                <ChevronRight size={18} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Log Detail Modal */}
      {selectedLog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white border-b px-6 py-4 flex items-center justify-between">
              <h3 className="text-lg font-bold">Log Details #{selectedLog.id}</h3>
              <button
                onClick={() => setSelectedLog(null)}
                className="p-2 hover:bg-gray-100 rounded-lg"
              >
                <X size={20} />
              </button>
            </div>
            
            <div className="p-6 space-y-6">
              {/* Main Info */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-gray-500">Vehicle Number</p>
                  <p className="font-mono font-bold text-xl">{selectedLog.vehicleNumber}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Normalized Plate</p>
                  <p className="font-mono">{selectedLog.normalizedPlate}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Timestamp</p>
                  <p>{formatDateTime(selectedLog.timestamp)}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Status</p>
                  {getStatusBadge(selectedLog.authorizationOutcome)}
                </div>
              </div>

              {/* Technical Details */}
              <div>
                <h4 className="font-medium mb-3">Technical Details</h4>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <p className="text-gray-500">Confidence Score</p>
                    <p>{selectedLog.confidenceScore != null ? `${selectedLog.confidenceScore}%` : 'N/A'}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Gate Action</p>
                    <p>{selectedLog.gateAction?.replace('_', ' ')}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Camera IP</p>
                    <p>{selectedLog.cameraIp || 'N/A'}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Vehicle Type</p>
                    <p>{selectedLog.vehicleType || 'N/A'}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Direction</p>
                    <p>{selectedLog.direction || 'N/A'}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Country</p>
                    <p>{selectedLog.country || 'N/A'}</p>
                  </div>
                </div>
              </div>

              {/* Additional Info */}
              {(selectedLog.companyName || selectedLog.driverName) && (
                <div>
                  <h4 className="font-medium mb-3">Schedule Information</h4>
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    {selectedLog.companyName && (
                      <div>
                        <p className="text-gray-500">Company</p>
                        <p>{selectedLog.companyName}</p>
                      </div>
                    )}
                    {selectedLog.driverName && (
                      <div>
                        <p className="text-gray-500">Driver</p>
                        <p>{selectedLog.driverName}</p>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Rejection/Override Reason */}
              {selectedLog.rejectionReason && (
                <div className="p-4 bg-red-50 rounded-lg">
                  <p className="text-sm text-red-600 font-medium">Rejection Reason</p>
                  <p className="text-red-700">{selectedLog.rejectionReason}</p>
                </div>
              )}
              
              {selectedLog.overrideReason && (
                <div className="p-4 bg-yellow-50 rounded-lg">
                  <p className="text-sm text-yellow-600 font-medium">Override Reason</p>
                  <p className="text-yellow-700">{selectedLog.overrideReason}</p>
                  {selectedLog.userId && (
                    <p className="text-xs text-yellow-600 mt-1">By: {selectedLog.userId}</p>
                  )}
                </div>
              )}

              {/* Images */}
              {(selectedLog.plateImage || selectedLog.vehicleImage) && (
                <div>
                  <h4 className="font-medium mb-3">Captured Images</h4>
                  <div className="grid grid-cols-2 gap-4">
                    {selectedLog.plateImage && (
                      <div>
                        <p className="text-sm text-gray-500 mb-2">Plate Image</p>
                        <img 
                          src={`data:image/jpeg;base64,${selectedLog.plateImage}`} 
                          alt="License Plate"
                          className="rounded border max-h-32 object-contain"
                        />
                      </div>
                    )}
                    {selectedLog.vehicleImage && (
                      <div>
                        <p className="text-sm text-gray-500 mb-2">Vehicle Image</p>
                        <img 
                          src={`data:image/jpeg;base64,${selectedLog.vehicleImage}`} 
                          alt="Vehicle"
                          className="rounded border max-h-32 object-contain"
                        />
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Logs;
