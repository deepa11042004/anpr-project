import { useMemo } from 'react';
import { docsApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { BookOpen, Download, ShieldAlert, CheckCircle2 } from 'lucide-react';

const sections = [
  {
    title: 'Deployment on Linux',
    bullets: [
      'Install Java 17+ and deploy the single jar file.',
      'Run with: java -jar anpr-access-control-1.0.0.jar',
      'Use systemd for auto-restart in production.',
    ],
  },
  {
    title: 'Camera Webhook Setup',
    bullets: [
      'Configure ANPR camera to POST JSON to /api/anpr/event.',
      'Use Connectivity tab to validate camera reachability and checklist.',
      'Confirm intranet route from camera VLAN to middleware host.',
    ],
  },
  {
    title: 'Boom Gate Integration',
    bullets: [
      'Middleware triggers camera relay endpoint for gate open command.',
      'Use Connectivity tab to run relay trigger test safely onsite.',
      'Validate physical wiring and permissions with gate/camera team.',
    ],
  },
  {
    title: 'Database Switching and Migration',
    bullets: [
      'Use Settings tab to Test Connection and Migrate H2 data to MySQL.',
      'Apply Config writes runtime-db.properties and restart activates it.',
      'Reset to H2 Default removes override and restores packaged behavior.',
    ],
  },
  {
    title: 'Monitoring and Operations',
    bullets: [
      'Dashboard for live metrics and decision outcomes.',
      'System Logs tab for real-time heartbeat and errors.',
      'History Logs for audit trail and post-incident analysis.',
    ],
  },
];

const Documentation = () => {
  const { hasRole } = useAuth();
  const canView = hasRole(['SUPER_ADMIN', 'ADMIN', 'OPERATOR']);

  const downloadUrl = useMemo(() => docsApi.getDownloadUrl(), []);

  if (!canView) {
    return (
      <div className="card">
        <div className="flex items-center gap-3 text-red-600">
          <ShieldAlert size={22} />
          <h2 className="text-xl font-semibold">Access denied</h2>
        </div>
        <p className="mt-3 text-gray-600">You do not have permission to view product documentation.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Setup Documentation</h1>
          <p className="text-gray-500">Complete handover guide for client engineering teams.</p>
        </div>

        <a
          href={downloadUrl}
          className="btn btn-primary inline-flex items-center gap-2"
          target="_blank"
          rel="noreferrer"
        >
          <Download size={16} />
          Download Full Guide
        </a>
      </div>

      <div className="card border border-blue-200 bg-blue-50">
        <div className="flex items-center gap-2 text-blue-900 font-semibold">
          <BookOpen size={18} />
          Documentation Coverage
        </div>
        <p className="mt-2 text-sm text-blue-900">
          The downloadable guide includes architecture, Linux deployment, security model, webhook setup, boom gate relay,
          database migration/reset, connectivity commissioning, logs monitoring, UAT checklist, and troubleshooting.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {sections.map((section) => (
          <div key={section.title} className="card">
            <h2 className="font-semibold text-lg mb-3">{section.title}</h2>
            <div className="space-y-2">
              {section.bullets.map((bullet) => (
                <div key={bullet} className="flex items-start gap-2 text-sm text-gray-700">
                  <CheckCircle2 size={16} className="text-green-600 mt-0.5 shrink-0" />
                  <span>{bullet}</span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Documentation;
