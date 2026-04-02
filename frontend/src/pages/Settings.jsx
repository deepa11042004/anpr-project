import { useState } from 'react';
import { settingsApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Database, Plug, RefreshCw, RotateCcw, Save, ShieldAlert } from 'lucide-react';

const initialForm = {
  host: '',
  port: 3306,
  databaseName: '',
  username: '',
  password: '',
  useSsl: false,
};

const Settings = () => {
  const { hasRole } = useAuth();
  const [form, setForm] = useState(initialForm);
  const [busy, setBusy] = useState({ test: false, migrate: false, apply: false, restart: false });
  const [result, setResult] = useState(null);

  const canManage = hasRole(['SUPER_ADMIN', 'ADMIN']);
  const canRestart = hasRole(['SUPER_ADMIN']);

  const updateField = (name, value) => {
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const runAction = async (key, action) => {
    setBusy((prev) => ({ ...prev, [key]: true }));
    try {
      const response = await action();
      setResult({ type: response.data.success ? 'success' : 'error', message: response.data.message, data: response.data });
    } catch (error) {
      const message = error.response?.data?.message || error.message || 'Unexpected error';
      setResult({ type: 'error', message });
    } finally {
      setBusy((prev) => ({ ...prev, [key]: false }));
    }
  };

  if (!canManage) {
    return (
      <div className="card">
        <div className="flex items-center gap-3 text-red-600">
          <ShieldAlert size={22} />
          <h2 className="text-xl font-semibold">Access denied</h2>
        </div>
        <p className="mt-3 text-gray-600">Only ADMIN or SUPER_ADMIN can manage database settings.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Settings</h1>
        <p className="text-gray-500">Configure external MySQL, test connectivity, migrate H2 data, and restart with the new runtime config.</p>
      </div>

      <div className="card">
        <div className="flex items-center gap-2 mb-4">
          <Database size={20} className="text-primary-600" />
          <h2 className="text-lg font-semibold">External Database Configuration</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Host</label>
            <input className="input" value={form.host} onChange={(e) => updateField('host', e.target.value)} placeholder="192.168.1.10" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Port</label>
            <input className="input" type="number" value={form.port} onChange={(e) => updateField('port', Number(e.target.value || 3306))} />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Database Name</label>
            <input className="input" value={form.databaseName} onChange={(e) => updateField('databaseName', e.target.value)} placeholder="anpr_middleware" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
            <input className="input" value={form.username} onChange={(e) => updateField('username', e.target.value)} placeholder="db_user" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <input className="input" type="password" value={form.password} onChange={(e) => updateField('password', e.target.value)} placeholder="********" />
          </div>

          <div className="flex items-end">
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                className="w-4 h-4 accent-primary-600"
                checked={form.useSsl}
                onChange={(e) => updateField('useSsl', e.target.checked)}
              />
              Use SSL
            </label>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-3 mt-6">
          <button
            className="btn btn-secondary flex items-center justify-center gap-2"
            onClick={() => runAction('test', () => settingsApi.testConnection(form))}
            disabled={busy.test}
          >
            <Plug size={16} />
            {busy.test ? 'Testing...' : 'Test Connection'}
          </button>

          <button
            className="btn btn-primary flex items-center justify-center gap-2"
            onClick={() => runAction('migrate', () => settingsApi.migrateData(form))}
            disabled={busy.migrate}
          >
            <RefreshCw size={16} className={busy.migrate ? 'animate-spin' : ''} />
            {busy.migrate ? 'Migrating...' : 'Migrate H2 Data'}
          </button>

          <button
            className="btn btn-secondary flex items-center justify-center gap-2"
            onClick={() => runAction('apply', () => settingsApi.applyConfig(form))}
            disabled={busy.apply}
          >
            <Save size={16} />
            {busy.apply ? 'Saving...' : 'Apply Config'}
          </button>

          <button
            className="btn btn-danger flex items-center justify-center gap-2 disabled:opacity-40"
            onClick={() => runAction('restart', () => settingsApi.restartApp())}
            disabled={busy.restart || !canRestart}
            title={canRestart ? 'Restart application' : 'Only SUPER_ADMIN can restart'}
          >
            <RotateCcw size={16} className={busy.restart ? 'animate-spin' : ''} />
            {busy.restart ? 'Restarting...' : 'Restart App'}
          </button>
        </div>
      </div>

      {result && (
        <div className={`card border ${result.type === 'success' ? 'border-green-200 bg-green-50' : 'border-red-200 bg-red-50'}`}>
          <p className={`font-medium ${result.type === 'success' ? 'text-green-700' : 'text-red-700'}`}>{result.message}</p>
          {result.data?.usersMigrated != null && (
            <div className="mt-3 text-sm text-gray-700 grid grid-cols-1 sm:grid-cols-3 gap-2">
              <span>Users: {result.data.usersMigrated}</span>
              <span>Schedules: {result.data.scheduledVehiclesMigrated}</span>
              <span>Audit Logs: {result.data.auditLogsMigrated}</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Settings;
