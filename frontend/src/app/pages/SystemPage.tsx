import { SystemMonitorPanel } from '../../modules/system/SystemMonitorPanel';
import { useAppContext } from '../state/AppContext';

export function SystemPage() {
  const {
    approvals,
    bookings,
    currentUser,
    dashboardOverview,
    myEvents,
    notifications,
    popularResources,
    publishedEvents,
    reloadWorkspace,
    resources,
    unreadCount
  } = useAppContext();

  return (
    <SystemMonitorPanel
      approvals={approvals}
      bookings={bookings}
      currentUser={currentUser}
      dashboardOverview={dashboardOverview}
      myEvents={myEvents}
      notifications={notifications}
      popularResources={popularResources}
      publishedEvents={publishedEvents}
      resources={resources}
      unreadCount={unreadCount}
      onReload={() => void reloadWorkspace()}
    />
  );
}
