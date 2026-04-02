import { useState } from 'react';
import { connectivityApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Cable, Camera, DoorOpen, ShieldAlert, Wifi, Send } from 'lucide-react';

const Connectivity = () => {
  const { hasRole } = useAuth();
  const [cameraForm, setCameraForm] = useState({
    cameraHost: '',
    cameraPort: 80,
    useHttps: false,
    cameraUsername: '',
    cameraPassword: '',
    middlewareBaseUrl: window.location.origin,
  });
  const [boomGateForm, setBoomGateForm] = useState({ cameraIp: '' });
  const [busy, setBusy] = useState({ camera: false, gate: false });
  const [cameraResult, setCameraResult] = useState(null);
  const [gateResult, setGateResult] = useState(null);

  const canManage = hasRole(['SUPER_ADMIN', 'ADMIN']);

  const run = async (kind, call, setter) => {
    setBusy((prev) => ({ ...prev, [kind]: true }));
    try {
      const response = await call();
      setter(response.data);
    } catch (error) {
      setter({
        success: false,
        message: error.response?.data?.message || error.message || 'Unexpected error',
      });
    } finally {
      setBusy((prev) => ({ ...prev, [kind]: false }));
    }
  };

  if (!canManage) {
    return (
      <div className="card">
        <div className="flex items-center gap-3 text-red-600">
          <ShieldAlert size={22} />
          <h2 className="text-xl font-semibold">Access denied</h2>
        </div>
        <p className="mt-3 text-gray-600">Only ADMIN or SUPER_ADMIN can configure onsite connectivity.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Connectivity</h1>
        <p className="text-gray-500">Configure and validate ANPR camera and boom gate integration before onsite UAT.</p>
      </div>

      <div className="card space-y-4">
        <div className="flex items-center gap-2">
          <Camera size={20} className="text-primary-600" />
          <h2 className="text-lg font-semibold">ANPR Camera Connectivity</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">Camera Host/IP</label>
            <input className="input" value={cameraForm.cameraHost} onChange={(e) => setCameraForm((p) => ({ ...p, cameraHost: e.target.value }))} placeholder="192.168.1.25" />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Camera Port</label>
            <input className="input" type="number" value={cameraForm.cameraPort} onChange={(e) => setCameraForm((p) => ({ ...p, cameraPort: Number(e.target.value || 80) }))} />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Camera Username</label>
            <input className="input" value={cameraForm.cameraUsername} onChange={(e) => setCameraForm((p) => ({ ...p, cameraUsername: e.target.value }))} placeholder="admin" />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Camera Password</label>
            <input className="input" type="password" value={cameraForm.cameraPassword} onChange={(e) => setCameraForm((p) => ({ ...p, cameraPassword: e.target.value }))} placeholder="********" />
          </div>
          <div className="md:col-span-2">
            <label className="block text-sm font-medium mb-1">Middleware Base URL</label>
            <input className="input" value={cameraForm.middlewareBaseUrl} onChange={(e) => setCameraForm((p) => ({ ...p, middlewareBaseUrl: e.target.value }))} placeholder="http://10.0.0.15:8080" />
          </div>
          <div className="md:col-span-2">
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" className="w-4 h-4 accent-primary-600" checked={cameraForm.useHttps} onChange={(e) => setCameraForm((p) => ({ ...p, useHttps: e.target.checked }))} />
              Use HTTPS for camera status check
            </label>
          </div>
        </div>

        <button
          className="btn btn-primary flex items-center gap-2"
          onClick={() => run('camera', () => connectivityApi.testCamera(cameraForm), setCameraResult)}
          disabled={busy.camera}
        >
          <Wifi size={16} />
          {busy.camera ? 'Testing Camera...' : 'Test Camera Connectivity'}
        </button>

        {cameraResult && (
          <div className={`p-4 rounded border ${cameraResult.success ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'}`}>
            <p className={cameraResult.success ? 'text-green-700 font-medium' : 'text-red-700 font-medium'}>{cameraResult.message}</p>
            {cameraResult.testedEndpoint && <p className="text-sm mt-2 text-gray-700">Tested Endpoint: {cameraResult.testedEndpoint}</p>}
            {cameraResult.statusCode != null && <p className="text-sm text-gray-700">Status Code: {cameraResult.statusCode}</p>}
            {cameraResult.recommendedWebhookUrl && (
              <div className="mt-3 p-3 bg-white rounded border">
                <p className="text-sm font-semibold">Configure this webhook URL on camera:</p>
                <p className="font-mono text-sm break-all">{cameraResult.recommendedWebhookUrl}</p>
              </div>
            )}
            {cameraResult.cameraSetupChecklist && (
              <div className="mt-3 text-sm text-gray-800">
                <p className="font-semibold">Camera Setup Checklist</p>
                <p>{cameraResult.cameraSetupChecklist}</p>
              </div>
            )}
            {cameraResult.sampleAnprPayload && (
              <div className="mt-3 p-3 bg-white rounded border">
                <p className="text-sm font-semibold">Sample ANPR Payload</p>
                <pre className="text-xs whitespace-pre-wrap break-all">{cameraResult.sampleAnprPayload}</pre>
              </div>
            )}
          </div>
        )}
      </div>

      <div className="card space-y-4">
        <div className="flex items-center gap-2">
          <DoorOpen size={20} className="text-primary-600" />
          <h2 className="text-lg font-semibold">Boom Gate Trigger Test</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">Camera IP for Relay Trigger</label>
            <input className="input" value={boomGateForm.cameraIp} onChange={(e) => setBoomGateForm({ cameraIp: e.target.value })} placeholder="192.168.1.25" />
          </div>
        </div>

        <button
          className="btn btn-secondary flex items-center gap-2"
          onClick={() => run('gate', () => connectivityApi.testBoomGate(boomGateForm), setGateResult)}
          disabled={busy.gate}
        >
          <Send size={16} />
          {busy.gate ? 'Triggering...' : 'Send Gate Open Trigger'}
        </button>

        {gateResult && (
          <div className={`p-4 rounded border ${gateResult.success ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'}`}>
            <p className={gateResult.success ? 'text-green-700 font-medium' : 'text-red-700 font-medium'}>{gateResult.message}</p>
            {gateResult.relayEndpoint && <p className="text-sm mt-2 text-gray-700">Relay Endpoint: {gateResult.relayEndpoint}</p>}
          </div>
        )}
      </div>

      <div className="card bg-blue-50 border border-blue-200">
        <div className="flex items-center gap-2 mb-2">
          <Cable size={18} className="text-blue-700" />
          <h3 className="font-semibold text-blue-900">Onsite Integration Note</h3>
        </div>
        <p className="text-sm text-blue-900">
          You provide the webhook URL to the client camera team. They configure their ANPR camera to POST events to that URL.
          For boom gate, you need their camera relay endpoint/network access and valid credentials. Use this page to verify both flows before UAT.
        </p>
      </div>
    </div>
  );
};

export default Connectivity;
