import { useState, useEffect } from 'react';
import { dashboardApi } from '../api/client';
import { 
  Car, 
  CheckCircle, 
  XCircle, 
  Hand,
  TrendingUp,
  Loader2,
  RefreshCw
} from 'lucide-react';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [period, setPeriod] = useState('today');

  const fetchStats = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await dashboardApi.getStats(period);
      setStats(response.data);
    } catch (err) {
      setError('Failed to load dashboard statistics');
      console.error('Dashboard error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
    // Auto-refresh every 30 seconds
    const interval = setInterval(fetchStats, 30000);
    return () => clearInterval(interval);
  }, [period]);

  const StatCard = ({ title, value, icon: Icon, iconColor, bgColor }) => (
    <div className="card">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-500 mb-1">{title}</p>
          <p className="text-3xl font-bold">{value ?? '-'}</p>
        </div>
        <div className={`p-3 rounded-full ${bgColor}`}>
          <Icon className={iconColor} size={24} />
        </div>
      </div>
    </div>
  );

  if (loading && !stats) {
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
        <div>
          <h1 className="text-2xl font-bold">Dashboard</h1>
          <p className="text-gray-500">{stats?.periodDescription || 'Overview'}</p>
        </div>
        
        <div className="flex items-center gap-3">
          <select
            value={period}
            onChange={(e) => setPeriod(e.target.value)}
            className="input py-2 px-3 w-auto"
          >
            <option value="today">Today</option>
            <option value="week">Last 7 Days</option>
            <option value="month">Last 30 Days</option>
            <option value="year">Last Year</option>
            <option value="all">All Time</option>
          </select>
          
          <button 
            onClick={fetchStats} 
            disabled={loading}
            className="btn btn-secondary flex items-center gap-2"
          >
            <RefreshCw className={loading ? 'animate-spin' : ''} size={18} />
            Refresh
          </button>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-700 rounded-lg">
          {error}
        </div>
      )}

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Total Entries"
          value={stats?.totalEntries}
          icon={Car}
          iconColor="text-primary-600"
          bgColor="bg-primary-100"
        />
        <StatCard
          title="Approved"
          value={stats?.approvedEntries}
          icon={CheckCircle}
          iconColor="text-green-600"
          bgColor="bg-green-100"
        />
        <StatCard
          title="Rejected"
          value={stats?.rejectedEntries}
          icon={XCircle}
          iconColor="text-red-600"
          bgColor="bg-red-100"
        />
        <StatCard
          title="Manual Overrides"
          value={stats?.manualOverrides}
          icon={Hand}
          iconColor="text-yellow-600"
          bgColor="bg-yellow-100"
        />
      </div>

      {/* Approval Rate Card */}
      <div className="card">
        <div className="flex items-center gap-4">
          <div className="p-4 bg-primary-100 rounded-full">
            <TrendingUp className="text-primary-600" size={32} />
          </div>
          <div>
            <p className="text-sm text-gray-500">Approval Rate</p>
            <p className="text-4xl font-bold text-primary-600">
              {stats?.approvalRate != null ? `${stats.approvalRate}%` : '-'}
            </p>
          </div>
        </div>
        
        {/* Progress Bar */}
        <div className="mt-6">
          <div className="h-4 bg-gray-100 rounded-full overflow-hidden">
            <div 
              className="h-full bg-gradient-to-r from-primary-500 to-primary-600 rounded-full transition-all duration-500"
              style={{ width: `${stats?.approvalRate || 0}%` }}
            />
          </div>
          <div className="flex justify-between mt-2 text-xs text-gray-500">
            <span>0%</span>
            <span>100%</span>
          </div>
        </div>
      </div>

      {/* Quick Info */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="card">
          <h3 className="font-semibold mb-4">System Status</h3>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-gray-600">ANPR Service</span>
              <span className="badge badge-success">Online</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Database</span>
              <span className="badge badge-success">Connected</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Authorization Source</span>
              <span className="badge badge-info">Configured</span>
            </div>
          </div>
        </div>
        
        <div className="card">
          <h3 className="font-semibold mb-4">Quick Actions</h3>
          <div className="space-y-3">
            <a href="/live-feed" className="block p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
              <p className="font-medium">View Live Feed</p>
              <p className="text-sm text-gray-500">Monitor incoming vehicles in real-time</p>
            </a>
            <a href="/logs" className="block p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
              <p className="font-medium">Browse Logs</p>
              <p className="text-sm text-gray-500">Search and filter historical entries</p>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
