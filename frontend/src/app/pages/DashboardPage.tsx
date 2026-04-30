import { Notice } from '../../components/Notice';
import { DashboardPanel } from '../../modules/dashboard/DashboardPanel';
import { useAppContext } from '../state/AppContext';

export function DashboardPage() {
  const { dashboardOverview, error, message, popularResources, reloadWorkspace } = useAppContext();

  return (
    <>
      <Notice message={message} tone="success" />
      <Notice message={error} tone="error" />
      <DashboardPanel overview={dashboardOverview} popularResources={popularResources} onReload={() => void reloadWorkspace()} />
    </>
  );
}
